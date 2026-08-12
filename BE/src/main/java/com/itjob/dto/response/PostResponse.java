package com.itjob.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostResponse {
    UUID id;
    UserBriefResponse user;
    CompanyBriefResponse company;
    String content;
    List<AttachmentResponse> attachments;
    PostInteractionResponse interaction;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
