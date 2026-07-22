package com.itjob.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogResponse {
    UUID id;
    UserBriefResponse author;
    BlogCategoryResponse category;
    String title;
    String excerpt;
    String content;
    String readTime;
    String image;
    String slug;
    Integer viewCount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
