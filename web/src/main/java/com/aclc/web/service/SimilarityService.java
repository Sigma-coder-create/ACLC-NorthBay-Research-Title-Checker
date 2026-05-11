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

    public SimilarityService(ResearchTitleRepository titleRepo) {
        this.titleRepo = titleRepo;
    }

    /**
     * Load all active titles (excluding Hard Bind) and initialise the similarity engine.
     */
    @PostConstruct
    public void init() {
        List<ResearchTitle> activeTitles = titleRepo.findByRecordState("ACTIVE");
        List<String> titles = new ArrayList<>();
        for (ResearchTitle rt : activeTitles) {
            if (rt.getResearchTitle() != null && !rt.getResearchTitle().trim().isEmpty()) {
                titles.add(rt.getResearchTitle());
            }
        }
        util.initialize(titles);
    }

    /**
     * Check a new title for duplicates.
     * @param newTitle the title to check
     * @return a list of existing titles that have a similarity score >= 0.7, each with its score.
     */
    public List<DuplicateInfo> findDuplicates(String newTitle) {
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