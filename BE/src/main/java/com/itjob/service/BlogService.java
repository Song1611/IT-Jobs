package com.itjob.service;

import com.itjob.dto.request.BlogRequest;
import com.itjob.dto.response.BlogBriefResponse;
import com.itjob.dto.response.BlogCategoryResponse;
import com.itjob.dto.response.BlogResponse;
import com.itjob.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface BlogService {
    
    // Public APIs
    List<BlogCategoryResponse> getAllCategories();
    
    List<BlogBriefResponse> getRecentBlogs(int limit);
    
    PageResponse<BlogBriefResponse> getAllBlogs(String[] filters, Pageable pageable);
    
    PageResponse<BlogBriefResponse> getBlogsByCategory(UUID categoryId, Pageable pageable);
    
    BlogResponse getBlogById(UUID id);
    
    // Authenticated User APIs
    PageResponse<BlogBriefResponse> getMyBlogs(UUID userId, Pageable pageable);
    
    BlogResponse createBlog(UUID userId, BlogRequest request);
    
    BlogResponse updateBlog(UUID id, UUID userId, BlogRequest request);
    
    void deleteBlog(UUID id, UUID userId);
}
