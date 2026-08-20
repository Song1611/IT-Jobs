package com.itjob.repository;

import com.itjob.entity.Attachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByPostId(UUID postId);

    List<Attachment> findByPostIdIn(List<UUID> postIds);

    List<Attachment> findByPostIdOrderByFileTypeAsc(UUID postId);

    @Query("SELECT a FROM Attachment a WHERE a.post IS NOT NULL AND a.post.author.id = :userId ORDER BY a.post.createdAt DESC")
    Page<Attachment> findByPostAuthorId(@Param("userId") UUID userId, Pageable pageable);
}
