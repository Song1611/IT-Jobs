package com.itjob.service;

import com.itjob.dto.request.ReviewRequest;
import com.itjob.dto.response.PageResponse;
import com.itjob.dto.response.ReviewResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

    // Public APIs
    PageResponse<ReviewResponse> getCompanyReviews(UUID companyId, Pageable pageable);

    ReviewResponse getReviewById(UUID id);

    // Authenticated User APIs
    ReviewResponse createReview(UUID companyId, UUID userId, ReviewRequest request);

    ReviewResponse updateReview(UUID id, UUID userId, ReviewRequest request);

    void deleteReview(UUID id, UUID userId);

    PageResponse<ReviewResponse> getMyReviews(UUID userId, Pageable pageable);

    // Admin APIs
    PageResponse<ReviewResponse> getAllReviews(String status, Pageable pageable);

    ReviewResponse approveReview(UUID id, UUID adminId);

    ReviewResponse rejectReview(UUID id, UUID adminId, String reason);
}
