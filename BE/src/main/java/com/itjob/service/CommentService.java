package com.itjob.service;

import com.itjob.dto.response.CommentResponse;
import com.itjob.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CommentService {

    CommentResponse create(UUID postId, UUID userId, String content);

    void delete(UUID postId, UUID commentId, UUID userId);

    PageResponse<CommentResponse> getByPost(UUID postId, Pageable pageable);
}
