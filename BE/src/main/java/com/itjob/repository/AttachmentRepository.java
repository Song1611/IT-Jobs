package com.itjob.repository;

import com.itjob.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByPostId(UUID postId);

    List<Attachment> findByPostIdIn(List<UUID> postIds);

    List<Attachment> findByPostIdOrderByFileTypeAsc(UUID postId);
}
