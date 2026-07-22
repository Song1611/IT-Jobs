package com.itjob.entity;

import com.itjob.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    
    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    Job job;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    User user;
    
    @Column(name = "cv_url")
    String cvUrl;
    
    @Column(name = "cover_letter", columnDefinition = "TEXT")
    String coverLetter;
    
    @Column(length = 20)
    String status = ApplicationStatus.PENDING.getValue();
    
    @Column(name = "applied_at")
    LocalDateTime appliedAt;
    
    @Column(name = "reviewed_at")
    LocalDateTime reviewedAt;
    
    @Column(name = "interview_at")
    LocalDateTime interviewAt;
    
    @Column(name = "responded_at")
    LocalDateTime respondedAt;
    
    @Column(name = "hr_notes", columnDefinition = "TEXT")
    String hrNotes;
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    String rejectionReason;
    
    Integer rating;
    
    @Column(name = "viewed_by_employer")
    Boolean viewedByEmployer = false;
    
    @Column(name = "viewed_at")
    LocalDateTime viewedAt;
    
    @Column(name = "created_at")
    LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        appliedAt = LocalDateTime.now();
        if (status == null) status = ApplicationStatus.PENDING.getValue();
        if (viewedByEmployer == null) viewedByEmployer = false;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
