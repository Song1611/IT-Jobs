package com.itjob.itjob.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
    String id;
    
    @ManyToOne
    @JoinColumn(name = "post_id")
    Post post;
    
    @ManyToOne
    @JoinColumn(name = "comment_id")
    Comment comment;
    
    String fileType; // 'image', 'video', 'audio', 'file'
    String fileUrl;
}
