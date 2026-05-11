package com.aclc.web.service;

import com.aclc.web.model.ResearchTitle;
import com.aclc.web.repository.ResearchTitleRepository;
import com.aclc.web.util.SimilarityUtil;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Service
public class SimilarityService {

    private final ResearchTitleRepository titleRepo;
    private final SimilarityUtil util = new SimilarityUtil();
    private volatile boolean ready = false;

    public SimilarityService(ResearchTitleRepository titleRepo) {
        this.titleRepo = titleRepo;
    }

    @PostConstruct
    public void init() {    
        new Thread(() -> {
            try {
                List<ResearchTitle> activeTitles = titleRepo.findByRecordState("ACTIVE");
                List<String> titles = new ArrayList<>();
                for (ResearchTitle rt : activeTitles) {
                    if (rt.getResearchTitle() != null && !rt.getResearchTitle().trim().isEmpty()) {
                        titles.add(rt.getResearchTitle());
                    }
                }
                util.initialize(titles);
                ready = true;
                System.out.println("[SimilarityService] Initialized with " + titles.size() + " titles.");
            } catch (Exception e) {
                System.err.println("[SimilarityService] Failed to initialize – duplicate detection disabled: " + e.getMessage());
            }
        }).start();
    }

    public List<DuplicateInfo> findDuplicates(String newTitle) {
        if (!ready) {
            System.err.println("[SimilarityService] Not ready – skipping duplicate check.");
            return new ArrayList<>();
        }
        List<DuplicateInfo> duplicates = new ArrayList<>();
        List<ResearchTitle> activeTitles = titleRepo.findByRecordState("ACTIVE");
        for (ResearchTitle rt : activeTitles) {
            if (rt.getResearchTitle() == null || rt.getResearchTitle().trim().isEmpty()) continue;
            double score = util.calculateSimilarity(newTitle, rt.getResearchTitle());
            if (score >= 0.7) {
                duplicates.add(new DuplicateInfo(rt.getResearchTitle(), rt.getSchoolYear(), score));
            }
        }
        return duplicates;
    }

    public static class DuplicateInfo {
        public String title;
        public String schoolYear;
        public double similarity;

        public DuplicateInfo(String title, String schoolYear, double similarity) {
            this.title = title;
            this.schoolYear = schoolYear;
            this.similarity = similarity;
        }
    }
}