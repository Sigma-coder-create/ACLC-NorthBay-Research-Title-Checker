package com.aclc.web.controller;

import com.aclc.web.model.ResearchTitle;
import com.aclc.web.model.Section;
import com.aclc.web.repository.ResearchTitleRepository;
import com.aclc.web.repository.SectionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api")
public class ResearchController {

    private final ResearchTitleRepository researchRepo;
    private final SectionRepository sectionRepo;

    public ResearchController(ResearchTitleRepository researchRepo, SectionRepository sectionRepo) {
        this.researchRepo = researchRepo;
        this.sectionRepo = sectionRepo;
    }

    @GetMapping("/sections")
    public List<Section> getSections() {
        return sectionRepo.findAll();
    }
    @GetMapping("/dashboard") // Or whatever your dashboard URL is
    public String showDashboard(HttpSession session, Model model) {
        
        // 1. Handle the Session-based Visitor Count
        Integer visitorCount = (Integer) session.getAttribute("visitorCount");
        if (visitorCount == null) {
            visitorCount = 1;
        } else {
            visitorCount++;
        }
        session.setAttribute("visitorCount", visitorCount);
        
        // 2. Pass the counts to your HTML
        model.addAttribute("visitorCount", visitorCount);
        
        // 3. Fetch your actual database counts for the other cards
        // Assuming you have a researchTitleRepository injected:
        long totalInputted = researchRepo.count(); 
        // Example: researchTitleRepository.countByStatus("Accepted");
        long totalAccepted = 5; // Placeholder until you add your repository logic
        
        model.addAttribute("totalInputted", totalInputted);
        model.addAttribute("totalAccepted", totalAccepted);

        return "Main"; // This matches your Main.html
    }
    @GetMapping("/research-titles")
    public Page<ResearchTitle> getResearchTitles(
            @RequestParam(required = false) Integer sectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "researchTitle") String sort,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sortObj = direction.equalsIgnoreCase("asc") ?
                Sort.by(sort).ascending() : Sort.by(sort).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sortObj);

        if (sectionId != null) {
            return researchRepo.findBySectionId(sectionId, pageRequest);
        } else {
            return researchRepo.findAll(pageRequest);
        }
    }
}