package com.itjob.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;
    
    String type; // 'application_status', 'new_job', 'company_update', 'review_reply'
    String title;
    
    @Column(columnDefinition = "TEXT")
    String message;
    
    @ManyToOne
    @JoinColumn(name = "related_job_id")
    Job relatedJob;
    
    @ManyToOne
    @JoinColumn(name = "related_company_id")
    Company relatedCompany;
    
    @ManyToOne
    @JoinColumn(name = "related_application_id")
    Application relatedApplication;
    
    Boolean isRead = false;
    LocalDateTime readAt;
    String actionUrl;
    
    LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isRead == null) isRead = false;
    }
}
