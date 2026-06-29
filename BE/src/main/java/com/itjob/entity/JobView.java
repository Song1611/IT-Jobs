package com.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_views")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class JobView {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    
    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    Job job;
    
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
