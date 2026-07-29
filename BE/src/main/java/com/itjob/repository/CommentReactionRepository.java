package com.itjob.repository;

import com.itjob.entity.CommentReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, UUID> {

    Optional<CommentReaction> findByCommentIdAndUserId(UUID commentId, UUID userId);

    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

    long countByCommentId(UUID commentId);
}
