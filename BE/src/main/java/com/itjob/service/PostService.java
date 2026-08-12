package com.itjob.service;

import com.itjob.dto.response.PageResponse;
import com.itjob.dto.response.PostResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface PostService {

    PageResponse<PostResponse> getAll(Pageable pageable);

    PostResponse getById(UUID id);

    PageResponse<PostResponse> getByUser(UUID userId, Pageable pageable);

    PageResponse<PostResponse> getByCompany(UUID companyId, Pageable pageable);

    PostResponse create(UUID userId, String content, UUID companyId, List<MultipartFile> images, MultipartFile video);

    PostResponse update(UUID postId, UUID userId, String content);

    PostResponse updateWithImages(UUID postId, UUID userId, String content, List<MultipartFile> images);

    void delete(UUID postId, UUID userId);
}
