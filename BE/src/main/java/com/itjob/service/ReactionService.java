package com.itjob.service;

import com.itjob.dto.response.ReactionResponse;
import com.itjob.enums.ReactionEntity;

import java.util.UUID;

public interface ReactionService {

    ReactionResponse togglePostReaction(UUID postId, UUID userId, String reactionType);

    ReactionResponse toggleCommentReaction(UUID commentId, UUID userId, String reactionType);

    long getPendingReactionDelta(ReactionEntity entity, UUID id);
}
