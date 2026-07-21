package com.itjob.controller;

import com.itjob.dto.request.ReviewRequest;
import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.dto.response.ReviewResponse;
import com.itjob.service.JwtService;
import com.itjob.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;
    private final JwtService jwtService;

    @GetMapping("/companies/{companyId}/reviews")
    public ApiResponse<PageResponse<ReviewResponse>> getCompanyReviews(
            @PathVariable UUID companyId,
            Pageable pageable) {

        log.info("Getting reviews for company: {}", companyId);

        return ApiResponse.<PageResponse<ReviewResponse>>builder()
                .message("Company reviews retrieved successfully")
                .result(reviewService.getCompanyReviews(companyId, pageable))
                .build();
    }

    @GetMapping("/reviews/{id}")
    public ApiResponse<ReviewResponse> getReviewById(@PathVariable UUID id) {

        log.info("Getting review by id: {}", id);

        return ApiResponse.<ReviewResponse>builder()
                .message("Review retrieved successfully")
                .result(reviewService.getReviewById(id))
                .build();
    }

    @PostMapping("/companies/{companyId}/reviews")
    public ApiResponse<ReviewResponse> createReview(
            @PathVariable UUID companyId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {

        UUID userId = jwtService.extractUserId(authentication);

        log.info("Creating review for company: {} by user: {}", companyId, userId);

        return ApiResponse.<ReviewResponse>builder()
                .message("Review created successfully")
                .result(reviewService.createReview(companyId, userId, request))
                .build();
    }

    @PutMapping("/reviews/{id}")
    public ApiResponse<ReviewResponse> updateReview(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {

        UUID userId = jwtService.extractUserId(authentication);

        log.info("Updating review {} by user {}", id, userId);

        return ApiResponse.<ReviewResponse>builder()
                .message("Review updated successfully")
                .result(reviewService.updateReview(id, userId, request))
                .build();
    }

    @DeleteMapping("/reviews/{id}")
    public ApiResponse<Void> deleteReview(
            @PathVariable UUID id,
            Authentication authentication) {

        UUID userId = jwtService.extractUserId(authentication);

        log.info("Deleting review {} by user {}", id, userId);

        reviewService.deleteReview(id, userId);

        return ApiResponse.<Void>builder()
                .message("Review deleted successfully")
                .build();
    }

    @GetMapping("/reviews/me")
    public ApiResponse<PageResponse<ReviewResponse>> getMyReviews(
            Authentication authentication,
            Pageable pageable) {

        UUID userId = jwtService.extractUserId(authentication);

        log.info("Getting reviews for user: {}", userId);

        return ApiResponse.<PageResponse<ReviewResponse>>builder()
                .message("Your reviews retrieved successfully")
                .result(reviewService.getMyReviews(userId, pageable))
                .build();
    }
}
