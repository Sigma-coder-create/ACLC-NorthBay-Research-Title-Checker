package com.aclc.web.repository;
import com.aclc.web.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SectionRepository extends JpaRepository<Section, Integer> {
}