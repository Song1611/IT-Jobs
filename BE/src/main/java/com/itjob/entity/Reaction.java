package com.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Reaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    
    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    Post post;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    User user;
    
    @Column(name = "reaction_type", nullable = false, length = 10)
    String reactionType; // 'like', 'love', 'haha', 'wow', 'sad', 'angry'
    
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
