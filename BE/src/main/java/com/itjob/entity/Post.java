package com.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

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
    UUID id;
    
    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    User author;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    Company company;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    String content;
    
    @ManyToOne
    @JoinColumn(name = "responding_to_post_id")
    Post respondingToPost;
    
    @Column(name = "response_ordinal", length = 4)
    String responseOrdinal;
    
    @Column(name = "view_count")
    Integer viewCount = 0;
    
    @Column(name = "comment_count")
    Integer commentCount = 0;
    
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
        if (viewCount == null) viewCount = 0;
        if (commentCount == null) commentCount = 0;
        if (reactionCount == null) reactionCount = 0;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
