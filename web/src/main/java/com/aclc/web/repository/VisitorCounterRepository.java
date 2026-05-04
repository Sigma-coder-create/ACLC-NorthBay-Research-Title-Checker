package com.aclc.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.aclc.web.model.VisitorCounter;

public interface VisitorCounterRepository extends JpaRepository<VisitorCounter, Long> {
}