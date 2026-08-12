package com.itjob.repository;

import com.itjob.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query("SELECT c.reactionCount FROM Comment c WHERE c.id = :id")
    Optional<Integer> getReactionCountById(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE Comment c SET c.reactionCount = COALESCE(c.reactionCount, 0) + :delta WHERE c.id = :id")
    int incrementReactionCount(@Param("id") UUID id, @Param("delta") long delta);

    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId ORDER BY c.createdAt DESC")
    Page<Comment> findByPostId(@Param("postId") UUID postId, Pageable pageable);
}
