package com.itjob.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReactionResponse {
    String entityType;
    String entityId;
    String reactionType;
    long reactionCount;
    boolean reacted;
}
