package com.itjob.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

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
    String id;
    
    @ManyToOne
    @JoinColumn(name = "job_id")
    Job job;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;
    
    String cvUrl;
    
    @Column(columnDefinition = "TEXT")
    String coverLetter;
    
    String status = "pending";
    
    LocalDateTime appliedAt;
    LocalDateTime reviewedAt;
    LocalDateTime interviewAt;
    LocalDateTime respondedAt;
    
    @Column(columnDefinition = "TEXT")
    String hrNotes;
    
    @Column(columnDefinition = "TEXT")
    String rejectionReason;
    
    Integer rating;
    Boolean viewedByEmployer = false;
    LocalDateTime viewedAt;
    
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        appliedAt = LocalDateTime.now();
        if (status == null) status = "pending";
        if (viewedByEmployer == null) viewedByEmployer = false;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
