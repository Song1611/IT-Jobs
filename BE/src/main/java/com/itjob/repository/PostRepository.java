package com.itjob.repository;

import com.itjob.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    @Query("SELECT p.reactionCount FROM Post p WHERE p.id = :id")
    Optional<Integer> getReactionCountById(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.reactionCount = COALESCE(p.reactionCount, 0) + :delta WHERE p.id = :id")
    int incrementReactionCount(@Param("id") UUID id, @Param("delta") long delta);

    @Query("SELECT p FROM Post p WHERE p.respondingToPost IS NULL ORDER BY p.createdAt DESC")
    Page<Post> findAllTopLevel(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.author.id = :userId AND p.respondingToPost IS NULL ORDER BY p.createdAt DESC")
    Page<Post> findByAuthorIdTopLevel(@Param("userId") UUID userId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.viewCount = COALESCE(p.viewCount, 0) + :count WHERE p.id = :id")
    int incrementViewCount(@Param("id") UUID id, @Param("count") long count);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.commentCount = CASE " +
            "WHEN COALESCE(p.commentCount, 0) + :delta < 0 THEN 0 " +
            "ELSE COALESCE(p.commentCount, 0) + :delta END WHERE p.id = :id")
    int incrementCommentCount(@Param("id") UUID id, @Param("delta") long delta);

    @Query("SELECT p FROM Post p WHERE p.company.id = :companyId AND p.respondingToPost IS NULL ORDER BY p.createdAt DESC")
    Page<Post> findByCompanyIdTopLevel(@Param("companyId") UUID companyId, Pageable pageable);

    @Query("SELECT p.id, COALESCE(p.reactionCount, 0), COALESCE(p.commentCount, 0) FROM Post p WHERE p.id IN :ids")
    List<Object[]> getInteractionCounts(@Param("ids") List<UUID> ids);
}
