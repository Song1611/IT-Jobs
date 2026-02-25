package com.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;
    
    @ManyToOne
    @JoinColumn(name = "company_id")
    Company company;
    
    Integer rating; // 1-5
    Integer salaryRating;
    Integer cultureRating;
    Integer managementRating;
    Integer workLifeBalanceRating;
    
    String title;
    
    @Column(columnDefinition = "TEXT")
    String pros;
    
    @Column(columnDefinition = "TEXT")
    String cons;
    
    @Column(columnDefinition = "TEXT")
    String advice;
    
    @Column(columnDefinition = "TEXT")
    String comment;
    
    Boolean isVerifiedEmployee = false;
    String workPosition;
    String workDuration;
    
    String status = "pending"; // 'pending', 'approved', 'rejected'
    Boolean isAnonymous = false;
    Integer helpfulCount = 0;
    
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "pending";
        if (isVerifiedEmployee == null) isVerifiedEmployee = false;
        if (isAnonymous == null) isAnonymous = false;
        if (helpfulCount == null) helpfulCount = 0;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
