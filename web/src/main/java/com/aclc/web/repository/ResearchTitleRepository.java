package com.aclc.web.repository;
import com.aclc.web.model.ResearchTitle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ResearchTitleRepository extends JpaRepository<ResearchTitle, Integer> {
    Page<ResearchTitle> findBySectionId(Integer sectionId, Pageable pageable);
    Page<ResearchTitle> findByRecordState(String recordState, Pageable pageable);

// Find ACTIVE titles within a specific section
    Page<ResearchTitle> findBySectionIdAndRecordState(Integer sectionId, String recordState, Pageable pageable);
    long countByRecordState(String recordState);

    // Count ACTIVE and Approved research titles
    long countByRecordStateAndStatus(String recordState, String status);
}