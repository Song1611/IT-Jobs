package com.itjob.entity;

import com.itjob.enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

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
    UUID id;
    
    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    Company company;
    
    @Column(nullable = false, length = 150)
    String title;
    
    @Column(unique = true)
    String slug;
    
    @Column(columnDefinition = "TEXT")
    String description;
    
    @Column(length = 20)
    String type;
    
    @Column(length = 20)
    String level;
    
    @Column(length = 50)
    String experience;
    
    Integer quantity = 1;
    
    @Column(name = "salary_min", precision = 15, scale = 2)
    BigDecimal salaryMin;
    
    @Column(name = "salary_max", precision = 15, scale = 2)
    BigDecimal salaryMax;
    
    @Column(name = "salary_currency", length = 10)
    String salaryCurrency = "VND";
    
    @Column(name = "salary_type", length = 20)
    String salaryType = "monthly";
    
    @Column(name = "is_negotiable")
    Boolean isNegotiable = false;
    
    @Column(name = "work_location", columnDefinition = "TEXT")
    String workLocation;
    
    @Column(columnDefinition = "TEXT")
    String benefits;
    
    @Column(columnDefinition = "TEXT")
    String requirements;
    
    @Column(name = "view_count")
    Long viewCount = 0L;
    
    @Column(name = "application_count")
    Integer applicationCount = 0;
    
    LocalDate deadline;
    
    @Column(length = 20)
    String status = JobStatus.OPEN.getValue();
    
    @ManyToOne
    @JoinColumn(name = "created_by")
    User createdBy;
    
    @ManyToOne
    @JoinColumn(name = "updated_by")
    User updatedBy;
    
    @Column(name = "created_at")
    LocalDateTime createdAt;
    
    @Column(name = "updated_at")
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
        if (viewCount == null) viewCount = 0L;
        if (applicationCount == null) applicationCount = 0;
        if (status == null) status = JobStatus.OPEN.getValue();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
