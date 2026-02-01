package com.itjob.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

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
    String id;
    
    @ManyToOne
    @JoinColumn(name = "post_id")
    Post post;
    
    @ManyToOne
    @JoinColumn(name = "author_id")
    User author;
    
    @Column(columnDefinition = "TEXT")
    String content;
    
    @ManyToOne
    @JoinColumn(name = "responding_to_comment_id")
    Comment respondingToComment;
    
    Integer reactionCount = 0;
    
    LocalDateTime createdAt;
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
