package com.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class CompanyImage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    
    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    Company company;
    
    @Column(name = "image_url", nullable = false)
    String imageUrl;
    
    @Column(name = "image_type", length = 20)
    String imageType = "other"; // 'office', 'team', 'event', 'other'
    
    String caption;
    
    @Column(name = "display_order")
    Integer displayOrder = 0;
    
    @Column(name = "created_at")
    LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (imageType == null) imageType = "other";
        if (displayOrder == null) displayOrder = 0;
    }
}
