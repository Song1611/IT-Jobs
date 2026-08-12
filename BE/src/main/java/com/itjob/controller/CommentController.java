package com.itjob.controller;

import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.CommentResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.service.CommentService;
import com.itjob.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/{postId}/comments")
    public ApiResponse<PageResponse<CommentResponse>> getComments(
            @PathVariable UUID postId,
            Pageable pageable) {
        log.info("Getting comments for post: {}", postId);
        return ApiResponse.<PageResponse<CommentResponse>>builder()
                .message("Comments retrieved successfully")
                .result(commentService.getByPost(postId, pageable))
                .build();
    }

    @PostMapping(value = "/{postId}/comment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CommentResponse> addComment(
            @PathVariable UUID postId,
            @RequestParam("Content") String content) {
        UUID userId = SecurityUtil.getCurrentUserId();
        log.info("Adding comment to post {} by user {}", postId, userId);
        return ApiResponse.<CommentResponse>builder()
                .message("Comment added successfully")
                .result(commentService.create(postId, userId, content))
                .build();
    }

    @DeleteMapping("/{postId}/comment/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            @RequestParam(required = false) UUID userId) {
        UUID currentUserId = userId != null ? userId : SecurityUtil.getCurrentUserId();
        log.info("Deleting comment {} on post {} by user {}", commentId, postId, currentUserId);
        commentService.delete(postId, commentId, currentUserId);
        return ApiResponse.<Void>builder()
                .message("Comment deleted successfully")
                .build();
    }
}
