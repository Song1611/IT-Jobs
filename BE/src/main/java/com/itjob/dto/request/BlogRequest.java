package com.itjob.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogRequest {
    
    @NotNull(message = "Category ID is required")
    UUID categoryId;
    
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    String title;
    
    @Size(max = 500, message = "Excerpt must not exceed 500 characters")
    String excerpt;
    
    @NotBlank(message = "Content is required")
    String content;
    
    @Size(max = 20, message = "Read time must not exceed 20 characters")
    String readTime;
    
    String image;
}
