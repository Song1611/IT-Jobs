package com.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    
    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    Post post;
    
    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    User author;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    String content;
    
    @ManyToOne
    @JoinColumn(name = "responding_to_comment_id")
    Comment respondingToComment;
    
    @Column(name = "reaction_count")
    Integer reactionCount = 0;
    
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (reactionCount == null) reactionCount = 0;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
