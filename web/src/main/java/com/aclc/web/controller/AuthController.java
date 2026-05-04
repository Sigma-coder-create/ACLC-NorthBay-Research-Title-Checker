package com.aclc.web.controller;

import com.aclc.web.model.ResearchTitle;
import com.aclc.web.model.Section;
import com.aclc.web.repository.ResearchTitleRepository;
import com.aclc.web.repository.SectionRepository;
import com.aclc.web.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;

@Controller
public class AuthController {

    private final UserService userService;
    private final ResearchTitleRepository researchTitleRepo;
    private final SectionRepository sectionRepo;

    public AuthController(UserService userService,
                          ResearchTitleRepository researchTitleRepo,
                          SectionRepository sectionRepo) {
        this.userService = userService;
        this.researchTitleRepo = researchTitleRepo;
        this.sectionRepo = sectionRepo;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage() { return "Login"; }

    @GetMapping("/register")
    public String registerPage() { return "Register"; }

    @GetMapping("/forgot")
    public String forgotPage() { return "Forgot"; }

    @GetMapping("/main")
    public String dashboard(@RequestParam(required = false) Integer sectionId,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(defaultValue = "researchTitle") String sort,
                            @RequestParam(defaultValue = "asc") String direction,
                            @RequestParam(required = false, defaultValue = "dashboard") String view,
                            @RequestParam(required = false) String submitted,
                            @RequestParam(required = false) String warning,
                            HttpSession session,
                            Model model) {

        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        Sort sortObj = direction.equalsIgnoreCase("desc") ?
                Sort.by(sort).descending() : Sort.by(sort).ascending();
        PageRequest pageable = PageRequest.of(page, size, sortObj);

        Page<ResearchTitle> researchTitles;
        if (sectionId != null) {
            researchTitles = researchTitleRepo.findBySectionIdAndRecordState(sectionId, "ACTIVE", pageable);
        } else {
            researchTitles = researchTitleRepo.findByRecordState("ACTIVE", pageable);
        }

        long totalInputted = researchTitleRepo.countByRecordState("ACTIVE");
        long totalAccepted = researchTitleRepo.countByRecordStateAndStatus("ACTIVE", "Approved");

        // =========================
        // ✅ IMPROVED GLOBAL VISITOR COUNTER (NO REFRESH SPAM)
        if (session.getAttribute("counted") == null) {

            Integer visitors = (Integer) session.getServletContext().getAttribute("visitorCount");
            if (visitors == null) visitors = 0;

            visitors++;
            session.getServletContext().setAttribute("visitorCount", visitors);

            session.setAttribute("counted", true);
        }

        Integer visitors = (Integer) session.getServletContext().getAttribute("visitorCount");
        if (visitors == null) visitors = 0;

        // =========================

        model.addAttribute("researchTitles", researchTitles);
        model.addAttribute("sections", sectionRepo.findAll());
        model.addAttribute("currentSectionId", sectionId);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentDirection", direction);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("currentView", view);
        model.addAttribute("visitorCount", visitors);
        model.addAttribute("totalInputted", totalInputted);
        model.addAttribute("totalAccepted", totalAccepted);

        if ("true".equals(submitted)) {
            model.addAttribute("successMessage", "Research title submitted successfully!");
        }
        if (warning != null && !warning.isEmpty()) {
            model.addAttribute("warningMessage", warning);
        }

        return "Main";
    }

    @PostMapping("/submit-research")
    public String submitResearch(@RequestParam String researchTitle,
                                @RequestParam String schoolYear,
                                @RequestParam String strand,
                                @RequestParam(required = false) Integer sectionId,
                                @RequestParam(required = false, defaultValue = "no") String software,
                                @RequestParam(required = false, defaultValue = "no") String webpage,
                                HttpSession session) {

        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        ResearchTitle rt = new ResearchTitle();
        rt.setResearchTitle(researchTitle);
        rt.setSchoolYear(schoolYear);
        rt.setStrand(strand);
        rt.setSoftware(software);
        rt.setWebpage(webpage);
        rt.setRecordState("ACTIVE");
        rt.setLastUpdated(java.time.LocalDateTime.now());
        rt.setStatus("Pending");
        rt.setApprovedBy("");
        rt.setApplied("no");

        if (sectionId != null) {
            Section section = sectionRepo.findById(sectionId).orElse(null);
            rt.setSection(section);
        }

        researchTitleRepo.save(rt);

        return "redirect:/main?view=forms&submitted=true";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {

        if (userService.login(username, password) != null) {
            session.setAttribute("user", username);
            return "redirect:/main";
        }

        model.addAttribute("error", "Invalid credentials");
        return "Login";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String password,
                           Model model) {

        if (password.length() < 6) {
            model.addAttribute("error", "Password too short");
            return "Register";
        }

        if (!userService.register(username, email, password)) {
            model.addAttribute("error", "Username taken or email invalid");
            return "Register";
        }

        return "redirect:/login";
    }

    @GetMapping("/test")
    public String test(Model model) {
        Page<ResearchTitle> titles = researchTitleRepo.findAll(PageRequest.of(0, 100));
        model.addAttribute("researchTitles", titles);
        return "test";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam String email, Model model) {
        model.addAttribute("message",
                "If an account exists, a reset link will be sent.");
        return "Forgot";
    }
}