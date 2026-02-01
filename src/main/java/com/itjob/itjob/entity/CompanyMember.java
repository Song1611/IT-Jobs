package com.itjob.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDateTime;

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
    @ManyToOne
    @JoinColumn(name = "company_id")
    Company company;
    
    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;
    
    String status = "active"; // 'pending', 'active', 'rejected'
    LocalDateTime joinedAt;
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
        Company company;
        User user;
    }
}
