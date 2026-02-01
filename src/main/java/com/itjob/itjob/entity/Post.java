package com.itjob.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    
    @ManyToOne
    @JoinColumn(name = "author_id")
    User author;
    
    @Column(columnDefinition = "TEXT")
    String content;
    
    @ManyToOne
    @JoinColumn(name = "responding_to_post_id")
    Post respondingToPost;
    
    String responseOrdinal;
    
    Integer viewCount = 0;
    Integer commentCount = 0;
    Integer reactionCount = 0;
    
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (viewCount == null) viewCount = 0;
        if (commentCount == null) commentCount = 0;
        if (reactionCount == null) reactionCount = 0;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
