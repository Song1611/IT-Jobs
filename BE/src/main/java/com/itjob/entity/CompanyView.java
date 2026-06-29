package com.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_views")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class CompanyView {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    
    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    Company company;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    User user; // NULL for anonymous views
    
    @Column(name = "ip_address", length = 45)
    String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    String userAgent;
    
    @Column(name = "viewed_at")
    LocalDateTime viewedAt;
    
    @PrePersist
    protected void onCreate() {
        viewedAt = LocalDateTime.now();
    }
}
