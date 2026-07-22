package com.itjob.entity;

import com.itjob.enums.CompanyStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    
    @Column(nullable = false, length = 150)
    String name;

    @Column(unique = true)
    String slug;

    @Column(length = 100)
    String email;

    @Column(length = 15)
    String phone;

    String website;

    @Column(name = "company_size", length = 20)
    String companySize; // '1-50', '51-200', '201-500', '501-1000', '1000+'

    @Column(length = 100)
    String industry;

    @Column(length = 100)
    String nationality;

    @Column(name = "founded_year")
    Integer foundedYear;

    @Column(name = "work_modes", columnDefinition = "TEXT")
    String workModes; // JSON: ["on-site", "remote", "hybrid"]

    @Column(name = "employment_types", columnDefinition = "TEXT")
    String employmentTypes; // JSON: ["full-time", "part-time", "contract", "internship"]

    String avatar;

    @Column(name = "cover_image")
    String coverImage;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(columnDefinition = "TEXT")
    String benefits;

    @Column(columnDefinition = "TEXT")
    String address;

    @Column(length = 20)
    String status; // 'pending', 'active', 'rejected', 'suspended'

    @Column(name = "verified_at")
    LocalDateTime verifiedAt;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    String rejectReason;

    @Column(name = "view_count")
    Long viewCount = 0L;

    @Column(name = "follower_count")
    Integer followerCount = 0;

    @Column(name = "is_deleted")
    Boolean isDeleted = false;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    User createdBy;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = CompanyStatus.PENDING.getValue();
        if (viewCount == null) viewCount = 0L;
        if (followerCount == null) followerCount = 0;
        if (isDeleted == null) isDeleted = false;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
