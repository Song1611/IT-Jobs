package com.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

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
    UUID id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    User user;
    
    @Column(nullable = false, length = 50)
    String type;
    
    @Column(nullable = false)
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
    
    @Column(name = "is_read")
    Boolean isRead = false;
    
    @Column(name = "read_at")
    LocalDateTime readAt;
    
    @Column(name = "action_url")
    String actionUrl;
    
    @Column(name = "created_at")
    LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isRead == null) isRead = false;
    }
}
