package com.itjob.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostInteractionResponse {
    long totalLikes;
    @JsonProperty("isLikedByCurrentUser")
    boolean likedByCurrentUser;
    long totalComments;
}
