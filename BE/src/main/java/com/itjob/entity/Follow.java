package com.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "follows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@IdClass(Follow.FollowId.class)
public class Follow {
    @Id
    @Column(name = "user_id")
    UUID userId;
    
    @Id
    @Column(name = "company_id")
    UUID companyId;
    
    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    User user;
    
    @ManyToOne
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    Company company;
    
    @Column(name = "created_at")
    LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FollowId implements Serializable {
        UUID userId;
        UUID companyId;
    }
}
