package com.itjob.controller;

import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.dto.response.PostResponse;
import com.itjob.enums.ViewEntity;
import com.itjob.service.PostService;
import com.itjob.service.ViewCountService;
import com.itjob.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;
    private final ViewCountService viewCountService;

    @GetMapping
    public ApiResponse<PageResponse<PostResponse>> getAllPosts(Pageable pageable) {
        log.info("Getting all posts");
        return ApiResponse.<PageResponse<PostResponse>>builder()
                .message("Posts retrieved successfully")
                .result(postService.getAll(pageable))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<PostResponse> getPostById(@PathVariable UUID id) {
        log.info("Getting post by id: {}", id);
        recordView(id);
        return ApiResponse.<PostResponse>builder()
                .message("Post retrieved successfully")
                .result(postService.getById(id))
                .build();
    }

    private void recordView(UUID postId) {
        String viewerId = null;
        if (SecurityUtil.isAuthenticated()) {
            try {
                viewerId = SecurityUtil.getCurrentUserId().toString();
            } catch (Exception ignored) {
                // anonymous
            }
        }
        // Best-effort; Redis failures fall back to DB via sync, no API error.
        try {
            viewCountService.incrementView(ViewEntity.POST, postId, viewerId);
        } catch (Exception e) {
            log.warn("Failed to increment view for post {}: {}", postId, e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<PageResponse<PostResponse>> getPostsByUser(
            @PathVariable UUID userId,
            Pageable pageable) {
        log.info("Getting posts by user: {}", userId);
        return ApiResponse.<PageResponse<PostResponse>>builder()
                .message("Posts retrieved successfully")
                .result(postService.getByUser(userId, pageable))
                .build();
    }

    @GetMapping("/company/{companyId}")
    public ApiResponse<PageResponse<PostResponse>> getPostsByCompany(
            @PathVariable UUID companyId,
            Pageable pageable) {
        log.info("Getting posts by company: {}", companyId);
        return ApiResponse.<PageResponse<PostResponse>>builder()
                .message("Posts retrieved successfully")
                .result(postService.getByCompany(companyId, pageable))
                .build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PostResponse> createPost(
            @RequestParam("Content") String content,
            @RequestParam(required = false) UUID CompanyId,
            @RequestParam(required = false) List<MultipartFile> Images,
            @RequestParam(required = false) MultipartFile Video) {
        UUID userId = SecurityUtil.getCurrentUserId();
        log.info("Creating post for user: {}", userId);
        return ApiResponse.<PostResponse>builder()
                .message("Post created successfully")
                .result(postService.create(userId, content, CompanyId, Images, Video))
                .build();
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<PostResponse> updatePost(
            @PathVariable UUID id,
            @RequestBody PostUpdateRequest request) {
        UUID userId = SecurityUtil.getCurrentUserId();
        log.info("Updating post {} by user {}", id, userId);
        return ApiResponse.<PostResponse>builder()
                .message("Post updated successfully")
                .result(postService.update(id, userId, request.content()))
                .build();
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PostResponse> updatePostWithImages(
            @PathVariable UUID id,
            @RequestParam("Content") String content,
            @RequestParam(required = false) List<MultipartFile> Images) {
        UUID userId = SecurityUtil.getCurrentUserId();
        log.info("Updating post {} with images by user {}", id, userId);
        return ApiResponse.<PostResponse>builder()
                .message("Post updated successfully")
                .result(postService.updateWithImages(id, userId, content, Images))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePost(@PathVariable UUID id) {
        UUID userId = SecurityUtil.getCurrentUserId();
        log.info("Deleting post {} by user {}", id, userId);
        postService.delete(id, userId);
        return ApiResponse.<Void>builder()
                .message("Post deleted successfully")
                .build();
    }

    public record PostUpdateRequest(String content) {}
}
