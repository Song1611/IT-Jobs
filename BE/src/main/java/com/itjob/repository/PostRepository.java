package com.itjob.repository;

import com.itjob.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
}
