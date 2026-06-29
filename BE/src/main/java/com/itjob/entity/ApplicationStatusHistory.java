package com.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "application_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ApplicationStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    
    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    Application application;
    
    @Column(name = "old_status", length = 20)
    String oldStatus;
    
    @Column(name = "new_status", nullable = false, length = 20)
    String newStatus;
    
    @ManyToOne
    @JoinColumn(name = "changed_by")
    User changedBy;
    
    @Column(columnDefinition = "TEXT")
    String notes;
    
    @Column(name = "changed_at")
    LocalDateTime changedAt;
    
    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}
