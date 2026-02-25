package com.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    
    @ManyToOne
    @JoinColumn(name = "company_id")
    Company company;
    
    String title;
    String slug;
    
    @Column(columnDefinition = "TEXT")
    String description;
    
    String type;
    String level;
    String experience;
    Integer quantity = 1;
    
    BigDecimal salaryMin;
    BigDecimal salaryMax;
    String salaryCurrency = "VND";
    String salaryType = "monthly";
    Boolean isNegotiable = false;
    
    @Column(columnDefinition = "TEXT")
    String workLocation;
    
    @Column(columnDefinition = "TEXT")
    String benefits;
    
    @Column(columnDefinition = "TEXT")
    String requirements;
    
    Integer viewCount = 0;
    Integer applicationCount = 0;
    LocalDate deadline;
    String status = "open";
    
    @ManyToOne
    @JoinColumn(name = "created_by")
    User createdBy;
    
    @ManyToOne
    @JoinColumn(name = "updated_by")
    User updatedBy;
    
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    @ManyToMany
    @JoinTable(
        name = "skill_jobs",
        joinColumns = @JoinColumn(name = "job_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    Set<Skill> skills;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (quantity == null) quantity = 1;
        if (salaryCurrency == null) salaryCurrency = "VND";
        if (salaryType == null) salaryType = "monthly";
        if (isNegotiable == null) isNegotiable = false;
        if (viewCount == null) viewCount = 0;
        if (applicationCount == null) applicationCount = 0;
        if (status == null) status = "open";
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
