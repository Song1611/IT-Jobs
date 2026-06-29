package com.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

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
    UUID id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    User user;
    
    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    Company company;
    
    Integer rating; // 1-5
    
    @Column(name = "salary_rating")
    Integer salaryRating;
    
    @Column(name = "culture_rating")
    Integer cultureRating;
    
    @Column(name = "management_rating")
    Integer managementRating;
    
    @Column(name = "work_life_balance_rating")
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
    
    @Column(name = "is_verified_employee")
    Boolean isVerifiedEmployee = false;
    
    @Column(name = "work_position", length = 100)
    String workPosition;
    
    @Column(name = "work_duration", length = 50)
    String workDuration;
    
    @Column(length = 20)
    String status = "pending"; // 'pending', 'approved', 'rejected'
    
    @Column(name = "is_anonymous")
    Boolean isAnonymous = false;
    
    @Column(name = "helpful_count")
    Integer helpfulCount = 0;
    
    @Column(name = "created_at")
    LocalDateTime createdAt;
    
    @Column(name = "updated_at")
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
