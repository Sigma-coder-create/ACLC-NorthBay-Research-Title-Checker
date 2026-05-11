package com.aclc.web.controller;

import com.aclc.web.model.PasswordResetToken;
import com.aclc.web.model.ResearchTitle;
import com.aclc.web.model.Section;
import com.aclc.web.repository.ResearchTitleRepository;
import com.aclc.web.repository.SectionRepository;
import com.aclc.web.repository.UserRepository;
import com.aclc.web.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.aclc.web.service.EmailService;
import com.aclc.web.service.SimilarityService;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import com.aclc.web.model.User;
import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
public class AuthController {

    private final UserService userService;
    private final ResearchTitleRepository researchTitleRepo;
    private final SectionRepository sectionRepo;
    private final UserRepository userRepo;  
    private final EmailService emailService;
    private final SimilarityService similarityService;

    public AuthController(UserService userService,
                          ResearchTitleRepository researchTitleRepo,
                          SectionRepository sectionRepo,
                          UserRepository userRepo,
                          EmailService emailService,
                          SimilarityService similarityService) {
        this.userService = userService;
        this.researchTitleRepo = researchTitleRepo;
        this.sectionRepo = sectionRepo;
        this.userRepo = userRepo;
        this.emailService = emailService;
        this.similarityService = similarityService;
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
                            @RequestParam(defaultValue = "dashboard") String view,
                            @RequestParam(required = false) String submitted,
                            @RequestParam(required = false) String warning,
                            @RequestParam(defaultValue = "0") int hardBindPage,
                            @RequestParam(defaultValue = "10") int hardBindSize,
                            HttpSession session,
                            Model model) {

        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        // ---- Fetch logged‑in user's email from database ----
        String username = (String) session.getAttribute("user");
        System.out.println("DEBUG: logged in as '" + username + "'");

        userRepo.findByUsername(username).ifPresentOrElse(user -> {
            model.addAttribute("loggedInUsername", user.getUsername());
            model.addAttribute("loggedInEmail", user.getEmail());
            System.out.println("DEBUG: email = " + user.getEmail());
        }, () -> {
            System.out.println("DEBUG: user not found for '" + username + "'");
        });

        // ---- Existing paging / counting logic ----
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

        // Visitor counter
        if (session.getAttribute("counted") == null) {
            Integer visitors = (Integer) session.getServletContext().getAttribute("visitorCount");
            if (visitors == null) visitors = 0;
            visitors++;
            session.getServletContext().setAttribute("visitorCount", visitors);
            session.setAttribute("counted", true);
        }
        Integer visitors = (Integer) session.getServletContext().getAttribute("visitorCount");
        if (visitors == null) visitors = 0;

        // Hard Bind paged list
        Page<ResearchTitle> hardBindPageObj = researchTitleRepo
                .findByStatusAndRecordState("Hard Bind", "ACTIVE",
                        PageRequest.of(hardBindPage, hardBindSize, Sort.by("lastUpdated").descending()));

        // Add all attributes to the model
        model.addAttribute("hardBindTitles", hardBindPageObj);
        model.addAttribute("researchTitles", researchTitles);
        model.addAttribute("sections", sectionRepo.findAll());
        model.addAttribute("currentSectionId", sectionId);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentDirection", direction);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("currentView", view);
        model.addAttribute("visitorCount", visitors);
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

    @PostMapping("/submit-research")
    public String submitResearch(@RequestParam String researchTitle,
                                @RequestParam String schoolYear,
                                @RequestParam String strand,
                                @RequestParam(required = false) Integer sectionId,
                                @RequestParam(required = false, defaultValue = "no") String software,
                                @RequestParam(required = false, defaultValue = "no") String webpage,
                                @RequestParam(required = false, defaultValue = "no") String researchPaper,
                                HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        String trimmedTitle = researchTitle.trim();   // ← missing line
        if (trimmedTitle.isEmpty()) {
            return "redirect:/main?view=forms&warning=" + encode("Title cannot be empty.");
        }

        // ---- Mandatory duplicate detection ----
        List<SimilarityService.DuplicateInfo> duplicates = similarityService.findDuplicates(trimmedTitle);
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

        // ----- Original saving logic (unchanged) -----
        ResearchTitle rt = new ResearchTitle();
        rt.setResearchTitle(trimmedTitle);
        rt.setSchoolYear(schoolYear);
        rt.setStrand(strand);
        rt.setSoftware(software);
        rt.setWebpage(webpage);
        rt.setResearchPaper(researchPaper);
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
        User user = userRepo.findByUsernameOrEmail(email, email).orElse(null);
        if (user == null) {
            model.addAttribute("message", "If an account exists, a reset link has been sent.");
            return "Forgot";
        }

        // Generate token and set expiry
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1)); // 1 hour validity
        userRepo.save(user);

        // Send email
        try {
            emailService.sendResetEmail(user.getEmail(), token);
        } catch (Exception e) {
            System.err.println("Failed to send reset email: " + e.getMessage());
        }

        model.addAttribute("message", "If an account exists, a reset link has been sent.");
        return "Forgot";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        User user = userRepo.findByResetToken(token).orElse(null);
        if (user == null || user.getResetTokenExpiry() == null ||
                user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Invalid or expired reset link.");
            return "Forgot";
        }
        model.addAttribute("token", token);
        return "ResetPassword";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam String token,
                                    @RequestParam String password,
                                    @RequestParam String confirmPassword,
                                    Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("token", token);
            return "ResetPassword";
        }
        if (password.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters.");
            model.addAttribute("token", token);
            return "ResetPassword";
        }

        // Look up the user by the token in the database
        User user = userRepo.findByResetToken(token).orElse(null);
        if (user == null || user.getResetTokenExpiry() == null ||
                user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Invalid or expired reset link.");
            return "Forgot";
        }

        // Update the password
        user.setPasswordHash(new BCryptPasswordEncoder().encode(password));

        // 🔥 Clear the reset token so it can't be used again
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userRepo.save(user);

        return "redirect:/login?resetSuccess";
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8.toString());
        } catch (java.io.UnsupportedEncodingException e) {
            return value;   // should never happen for UTF-8
        }
    }
}
