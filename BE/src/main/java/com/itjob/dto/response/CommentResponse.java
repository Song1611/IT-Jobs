package com.itjob.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentResponse {
    UUID id;
    UserBriefResponse user;
    String content;
    int reactionCount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
