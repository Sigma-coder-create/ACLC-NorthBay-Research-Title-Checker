package com.aclc.web.controller;

import com.aclc.web.model.ResearchTitle;
import com.aclc.web.model.Section;
import com.aclc.web.repository.ResearchTitleRepository;
import com.aclc.web.repository.SectionRepository;
import com.aclc.web.service.SimilarityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.List;

@Controller
public class MainController {

    private final ResearchTitleRepository researchTitleRepo;
    private final SectionRepository sectionRepo;
    private final SimilarityService similarityService;

    public MainController(ResearchTitleRepository researchTitleRepo,
                          SectionRepository sectionRepo,
                          SimilarityService similarityService) {
        this.researchTitleRepo = researchTitleRepo;
        this.sectionRepo = sectionRepo;
        this.similarityService = similarityService;
    }

    // --- Dashboard & Main View ---
    @GetMapping("/main")
    public String dashboard(@RequestParam(required = false) Integer sectionId,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(defaultValue = "researchTitle") String sort,
                            @RequestParam(defaultValue = "asc") String direction,
                            @RequestParam(defaultValue = "dashboard") String view,
                            @RequestParam(required = false) String submitted,
                            @RequestParam(required = false) String warning,
                            @RequestParam(defaultValue = "0") int hardBindPage,
                            @RequestParam(defaultValue = "10") int hardBindSize,
                            Model model) {

        Sort sortObj = direction.equalsIgnoreCase("desc") ?
                Sort.by(sort).descending() : Sort.by(sort).ascending();
        PageRequest pageable = PageRequest.of(page, size, sortObj);

        Page<ResearchTitle> researchTitles;
        if (sectionId != null) {
            researchTitles = researchTitleRepo.findBySectionIdAndRecordStateAndStatusNot(
                    sectionId, "ACTIVE", "Hard Bind", pageable);
        } else {
            researchTitles = researchTitleRepo.findByRecordStateAndStatusNot(
                    "ACTIVE", "Hard Bind", pageable);
        }

        long totalAllActive = researchTitleRepo.countByRecordState("ACTIVE");
        long totalInputtedExcludingHardBind =
                researchTitleRepo.countByRecordStateAndStatusNot("ACTIVE", "Hard Bind");
        long totalAccepted =
                researchTitleRepo.countByRecordStateAndStatus("ACTIVE", "Approved");

        Page<ResearchTitle> hardBindPageObj = researchTitleRepo
                .findByStatusAndRecordState("Hard Bind", "ACTIVE",
                        PageRequest.of(hardBindPage, hardBindSize, Sort.by("lastUpdated").descending()));

        model.addAttribute("hardBindTitles", hardBindPageObj);
        model.addAttribute("researchTitles", researchTitles);
        model.addAttribute("sections", sectionRepo.findAll());
        model.addAttribute("currentSectionId", sectionId);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentDirection", direction);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("currentView", view);
        model.addAttribute("totalInputtedExcludingHardBind", totalInputtedExcludingHardBind);
        model.addAttribute("totalAllActive", totalAllActive);
        model.addAttribute("totalAccepted", totalAccepted);

        if ("true".equals(submitted)) {
            model.addAttribute("successMessage", "Research title submitted successfully!");
        }
        if (warning != null && !warning.isEmpty()) {
            model.addAttribute("warningMessage", warning);
        }

        return "Main";
    }

    // --- Research Title Submission ---
    @PostMapping("/submit-research")
    public String submitResearch(@RequestParam String researchTitle,
                                @RequestParam String schoolYear,
                                @RequestParam String strand,
                                @RequestParam(required = false) Integer sectionId,
                                @RequestParam(required = false, defaultValue = "no") String software,
                                @RequestParam(required = false, defaultValue = "no") String webpage,
                                @RequestParam(required = false, defaultValue = "no") String researchPaper) {
        String trimmedTitle = researchTitle.trim();
        if (trimmedTitle.isEmpty()) {
            return "redirect:/main?view=forms&warning=" + encode("Title cannot be empty.");
        }

        // Mandatory duplicate detection
        java.util.List<SimilarityService.DuplicateInfo> duplicates = similarityService.findDuplicates(trimmedTitle);
        if (!duplicates.isEmpty()) {
            SimilarityService.DuplicateInfo top = duplicates.get(0);
            int percent = (int)(top.similarity * 100);
            String shortTitle = top.title.length() > 80 ? top.title.substring(0, 77) + "..." : top.title;
            String warnMsg = String.format(
                "Submission blocked: this title is %d%% similar to \"%s\" (SY %s). Please modify your title.",
                percent, shortTitle, top.schoolYear
            );
            return "redirect:/main?view=forms&warning=" + encode(warnMsg);
        }

        // Original saving logic
        ResearchTitle rt = new ResearchTitle();
        rt.setResearchTitle(trimmedTitle);
        rt.setSchoolYear(schoolYear);
        rt.setStrand(strand);
        rt.setSoftware(software);
        rt.setWebpage(webpage);
        rt.setResearchPaper(researchPaper);
        rt.setRecordState("ACTIVE");
        rt.setLastUpdated(java.time.LocalDateTime.now());
        rt.setStatus("Approved");
        rt.setApprovedBy("");
        rt.setApplied("no");

        if (sectionId != null) {
            Section section = sectionRepo.findById(sectionId).orElse(null);
            rt.setSection(section);
        }

        researchTitleRepo.save(rt);
        return "redirect:/main?view=forms&submitted=true";
    }

    
    // --- Home Page Redirect (now goes directly to main) ---
    @GetMapping("/")
    public String home() {
        return "redirect:/main";
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8.toString());
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }
    @GetMapping("/check-similarity")
    @ResponseBody
    public Map<String, Object> checkSimilarity(@RequestParam String title) {
        List<SimilarityService.DuplicateInfo> duplicates = similarityService.findDuplicates(title.trim());
        if (duplicates.isEmpty()) {
            return Map.of("blocked", false);
        }
        SimilarityService.DuplicateInfo top = duplicates.get(0);
        int percent = (int)(top.similarity * 100);
        String shortTitle = top.title.length() > 80 ? top.title.substring(0, 77) + "..." : top.title;
        return Map.of(
            "blocked", true,
            "percent", percent,
            "similarTitle", shortTitle,
            "schoolYear", top.schoolYear
        );
    }
    @GetMapping("/api/titles")
    @ResponseBody
    public List<Map<String, String>> getAllTitles() {
        return researchTitleRepo.findAll().stream()
                .map(rt -> Map.of(
                    "title", rt.getResearchTitle(),
                    "status", rt.getStatus(),
                    "id", rt.getId().toString()))
                .collect(Collectors.toList());
    }
}