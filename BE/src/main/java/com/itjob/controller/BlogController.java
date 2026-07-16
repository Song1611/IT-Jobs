package com.itjob.controller;

import com.itjob.dto.request.BlogRequest;
import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.BlogBriefResponse;
import com.itjob.dto.response.BlogResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.service.BlogService;
import com.itjob.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
@Slf4j
public class BlogController {
    
    private final BlogService blogService;
    private final JwtService jwtService;
    
    /**
     * Public APIs
     */
    
    @GetMapping("/recent")
    public ApiResponse<List<BlogBriefResponse>> getRecentBlogs(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Getting recent blogs, limit: {}", limit);
        
        return ApiResponse.<List<BlogBriefResponse>>builder()
                .message("Recent blogs retrieved successfully")
                .result(blogService.getRecentBlogs(limit))
                .build();
    }
    
    /**
     * Search blogs using Specification pattern with filter array
     * Examples:
     * - GET /api/blogs?filter=title~programming
     * - GET /api/blogs?filter=title~java&filter=categoryId:uuid
     * Supported operators:
     * - : (EQUALITY)       → filter=categoryId:uuid
     * - ~ (LIKE)           → filter=title~java
     * - ! (NOT_EQUAL)      → filter=id!uuid
     * - > (GREATER)        → filter=createdAt>2024-01-01
     * OR Logic: Use ' prefix
     * - GET /api/blogs?filter='title~java&filter='title~python
     */
    @GetMapping
    public ApiResponse<PageResponse<BlogBriefResponse>> getAllBlogs(
            @RequestParam(required = false) String[] filter,
            Pageable pageable) {
        
        log.info("Getting all blogs with filters");
        
        return ApiResponse.<PageResponse<BlogBriefResponse>>builder()
                .message("Blogs retrieved successfully")
                .result(blogService.getAllBlogs(filter, pageable))
                .build();
    }
    
    @GetMapping("/category/{categoryId}")
    public ApiResponse<PageResponse<BlogBriefResponse>> getBlogsByCategory(
            @PathVariable UUID categoryId,
            Pageable pageable) {
        
        log.info("Getting blogs by category: {}", categoryId);
        
        return ApiResponse.<PageResponse<BlogBriefResponse>>builder()
                .message("Blogs retrieved successfully")
                .result(blogService.getBlogsByCategory(categoryId, pageable))
                .build();
    }
    
    @GetMapping("/{id}")
    public ApiResponse<BlogResponse> getBlogById(@PathVariable UUID id) {
        
        log.info("Getting blog by id: {}", id);
        
        return ApiResponse.<BlogResponse>builder()
                .message("Blog retrieved successfully")
                .result(blogService.getBlogById(id))
                .build();
    }
    
    /**
     * Authenticated User APIs
     */
    
    @GetMapping("/me")
    public ApiResponse<PageResponse<BlogBriefResponse>> getMyBlogs(
            Authentication authentication,
            Pageable pageable) {
        
        UUID userId = jwtService.extractUserId(authentication);
        
        log.info("Getting blogs for user: {}", userId);
        
        return ApiResponse.<PageResponse<BlogBriefResponse>>builder()
                .message("Your blogs retrieved successfully")
                .result(blogService.getMyBlogs(userId, pageable))
                .build();
    }
    
    @PostMapping
    public ApiResponse<BlogResponse> createBlog(
            @Valid @RequestBody BlogRequest request,
            Authentication authentication) {
        
        UUID userId = jwtService.extractUserId(authentication);
        
        log.info("Creating blog for user: {}", userId);
        
        return ApiResponse.<BlogResponse>builder()
                .message("Blog created successfully")
                .result(blogService.createBlog(userId, request))
                .build();
    }
    
    @PutMapping("/{id}")
    public ApiResponse<BlogResponse> updateBlog(
            @PathVariable UUID id,
            @Valid @RequestBody BlogRequest request,
            Authentication authentication) {
        
        UUID userId = jwtService.extractUserId(authentication);
        
        log.info("Updating blog {} by user {}", id, userId);
        
        return ApiResponse.<BlogResponse>builder()
                .message("Blog updated successfully")
                .result(blogService.updateBlog(id, userId, request))
                .build();
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBlog(
            @PathVariable UUID id,
            Authentication authentication) {
        
        UUID userId = jwtService.extractUserId(authentication);
        
        log.info("Deleting blog {} by user {}", id, userId);
        
        blogService.deleteBlog(id, userId);
        
        return ApiResponse.<Void>builder()
                .message("Blog deleted successfully")
                .build();
    }
}
