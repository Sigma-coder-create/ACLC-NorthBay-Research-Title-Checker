package com.aclc.web.model;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "aclc_research_titles")
public class ResearchTitle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "`Research Title`")
    private String researchTitle;

    @Column(name = "`SY-YR`")
    private String schoolYear;

    @Column(name = "status")
    private String status;

    @Column(name = "`Approved by`")
    private String approvedBy;

    @Column(name = "Applied")
    private String applied;

    @Column(name = "Strand")
    private String strand;

    @Column(name = "Software")
    private String software;

    @Column(name = "Webpage")
    private String webpage;

    @Column(name = "`Research Paper`")
    private String researchPaper;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "record_state")
    private String recordState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;

    // getters and setters (add them all or generate with your IDE)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getResearchTitle() { return researchTitle; }
    public void setResearchTitle(String researchTitle) { this.researchTitle = researchTitle; }

    public String getSchoolYear() { return schoolYear; }
    public void setSchoolYear(String schoolYear) { this.schoolYear = schoolYear; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getApplied() { return applied; }
    public void setApplied(String applied) { this.applied = applied; }

    public String getStrand() { return strand; }
    public void setStrand(String strand) { this.strand = strand; }

    public String getSoftware() { return software; }
    public void setSoftware(String software) { this.software = software; }

    public String getWebpage() { return webpage; }
    public void setWebpage(String webpage) { this.webpage = webpage; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getRecordState() { return recordState; }
    public void setRecordState(String recordState) { this.recordState = recordState; }

    public Section getSection() { return section; }
    public void setSection(Section section) { this.section = section; }

    public String getResearchPaper() { return researchPaper; }
    public void setResearchPaper(String researchPaper) { this.researchPaper = researchPaper; }
}