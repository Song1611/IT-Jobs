package com.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@IdClass(CompanyMember.CompanyMemberId.class)
public class CompanyMember {
    @Id
    @Column(name = "company_id")
    UUID companyId;
    
    @Id
    @Column(name = "user_id")
    UUID userId;
    
    @ManyToOne
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    Company company;
    
    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    User user;
    
    @Column(length = 20, nullable = false)
    String status = "active"; // 'pending', 'active', 'rejected'
    
    @Column(name = "joined_at")
    LocalDateTime joinedAt;
    
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "active";
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyMemberId implements Serializable {
        UUID companyId;
        UUID userId;
    }
}
