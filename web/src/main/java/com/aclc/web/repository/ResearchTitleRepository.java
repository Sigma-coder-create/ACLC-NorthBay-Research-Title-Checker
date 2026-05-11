package com.aclc.web.repository;

import com.aclc.web.model.ResearchTitle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;   // ← ADD THIS

public interface ResearchTitleRepository extends JpaRepository<ResearchTitle, Integer> {

    Page<ResearchTitle> findBySectionId(Integer sectionId, Pageable pageable);
    Page<ResearchTitle> findByRecordState(String recordState, Pageable pageable);

    // Find ACTIVE titles within a specific section
    Page<ResearchTitle> findBySectionIdAndRecordState(Integer sectionId, String recordState, Pageable pageable);
    long countByRecordState(String recordState);

    // Count ACTIVE and Approved research titles
    long countByRecordStateAndStatus(String recordState, String status);
    long countByRecordStateAndStatusNot(String recordState, String status);
    // Find by status (used for Hard Bind view)
    @Query("SELECT r FROM ResearchTitle r WHERE r.status = :status AND r.recordState = 'ACTIVE'")
    List<ResearchTitle> findByStatus(@Param("status") String status);

    // NEW: Exclude a status (used to hide Hard Bind from main table)
    @Query("SELECT r FROM ResearchTitle r WHERE r.recordState = :recordState AND r.status != :excludedStatus")
    Page<ResearchTitle> findByRecordStateAndStatusNot(
            @Param("recordState") String recordState,
            @Param("excludedStatus") String excludedStatus,
            Pageable pageable);

    @Query("SELECT r FROM ResearchTitle r WHERE r.section.id = :sectionId AND r.recordState = :recordState AND r.status != :excludedStatus")
    Page<ResearchTitle> findBySectionIdAndRecordStateAndStatusNot(
            @Param("sectionId") Integer sectionId,
            @Param("recordState") String recordState,
            @Param("excludedStatus") String excludedStatus,
            Pageable pageable);
    
    @Query("SELECT r FROM ResearchTitle r WHERE r.status = :status AND r.recordState = :recordState")
    Page<ResearchTitle> findByStatusAndRecordState(@Param("status") String status,
                                               @Param("recordState") String recordState,
                                               Pageable pageable);

    @Query("SELECT r FROM ResearchTitle r WHERE r.recordState = :recordState")
    List<ResearchTitle> findByRecordState(@Param("recordState") String recordState);
}