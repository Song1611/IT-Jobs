package com.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    
    @ManyToOne
    @JoinColumn(name = "post_id")
    Post post;
    
    @ManyToOne
    @JoinColumn(name = "comment_id")
    Comment comment;
    
    @Column(name = "file_type", length = 20)
    String fileType; // 'image', 'video', 'audio', 'file'
    
    @Column(name = "file_url", nullable = false)
    String fileUrl;
    
    @Column(name = "file_public_id", length = 500)
    String filePublicId;
}
