package com.aclc.web.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class VisitorCounter {

    @Id
    private Long id = 1L;

    private Long count;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
}