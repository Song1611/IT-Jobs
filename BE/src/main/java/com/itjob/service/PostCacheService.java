package com.itjob.service;

import com.itjob.dto.response.PageResponse;
import com.itjob.dto.response.PostResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PostCacheService {

    PostResponse getCachedPostDetail(UUID postId);

    PageResponse<PostResponse> getCachedPostList(Pageable pageable);

    PageResponse<PostResponse> getCachedPostsByCompany(UUID companyId, Pageable pageable);

    PageResponse<PostResponse> getCachedPostsByUser(UUID userId, Pageable pageable);

    void evictPost(UUID postId);

    void evictAll();
}