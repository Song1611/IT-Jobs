package com.itjob.service.impl;

import com.itjob.annotation.DistributedLock;
import com.itjob.redis.CacheName;
import com.itjob.dto.request.ReviewRequest;
import com.itjob.dto.response.ReviewResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.entity.Company;
import com.itjob.entity.Review;
import com.itjob.entity.User;
import com.itjob.enums.CompanyStatus;
import com.itjob.enums.ReviewStatus;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.mapper.ReviewMapper;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.ReviewRepository;
import com.itjob.repository.UserRepository;
import com.itjob.service.ReviewService;
import com.itjob.util.PageResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    @Override
    @Cacheable(value = CacheName.REVIEW_BY_COMPANY,
               key = "T(com.itjob.util.CacheKeyGenerator).forIdWithPageable(#companyId, #pageable)")
    public PageResponse<ReviewResponse> getCompanyReviews(UUID companyId, Pageable pageable) {
        log.debug("Fetching approved reviews for company {} from database", companyId);
        Page<Review> reviewPage = reviewRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(
                companyId, ReviewStatus.APPROVED.getValue(), pageable);
        return buildPageResponse(reviewPage);
    }

    @Override
    @Cacheable(value = CacheName.REVIEW_DETAIL, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)")
    public ReviewResponse getReviewById(UUID id) {
        log.debug("Fetching review {} from database", id);
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));
        return reviewMapper.toReviewResponse(review);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Caching(evict = {
            @CacheEvict(value = CacheName.REVIEW_BY_COMPANY, allEntries = true),
            @CacheEvict(value = CacheName.DASHBOARD_ADMIN,
                        key = "T(com.itjob.util.CacheKeyGenerator).forAdminDashboard()")
    })
    public ReviewResponse createReview(UUID companyId, UUID userId, ReviewRequest request) {
        log.debug("Creating review for company {} by user {}", companyId, userId);

        Company company = companyRepository.findByIdAndIsDeleted(companyId, false)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

        if (!CompanyStatus.ACTIVE.getValue().equals(company.getStatus())) {
            throw new AppException(ErrorCode.COMPANY_NOT_ACTIVE);
        }

        if (reviewRepository.existsByUserIdAndCompanyId(userId, companyId)) {
            throw new AppException(ErrorCode.ALREADY_REVIEWED);
        }

        User user = userRepository.getReferenceById(userId);

        Review review = reviewMapper.toReview(request);
        review.setCompany(company);
        review.setUser(user);
        review.setStatus(ReviewStatus.PENDING.getValue());

        review = reviewRepository.save(review);

        log.debug("Review created successfully with id: {}", review.getId());
        return reviewMapper.toReviewResponse(review);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Caching(evict = {
            @CacheEvict(value = CacheName.REVIEW_DETAIL, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)"),
            @CacheEvict(value = CacheName.REVIEW_BY_COMPANY, allEntries = true)
    })
    public ReviewResponse updateReview(UUID id, UUID userId, ReviewRequest request) {
        log.debug("Updating review {} by user {}", id, userId);

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        if (!ReviewStatus.PENDING.getValue().equals(review.getStatus())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        reviewMapper.updateReview(review, request);

        log.debug("Review updated successfully");
        return reviewMapper.toReviewResponse(review);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Caching(evict = {
            @CacheEvict(value = CacheName.REVIEW_DETAIL, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)"),
            @CacheEvict(value = CacheName.REVIEW_BY_COMPANY, allEntries = true)
    })
    public void deleteReview(UUID id, UUID userId) {
        log.debug("Deleting review {} by user {}", id, userId);

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        reviewRepository.delete(review);
        log.debug("Review deleted successfully");
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public PageResponse<ReviewResponse> getMyReviews(UUID userId, Pageable pageable) {
        log.debug("Fetching reviews for user {} from database", userId);
        Page<Review> reviewPage = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return buildPageResponse(reviewPage);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<ReviewResponse> getAllReviews(String status, Pageable pageable) {
        log.debug("Admin fetching all reviews with status: {}", status);
        Page<Review> reviewPage;

        if (status != null && !status.isEmpty()) {
            ReviewStatus.fromValue(status);
            reviewPage = reviewRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            reviewPage = reviewRepository.findAll(pageable);
        }

        return buildPageResponse(reviewPage);
    }

    @Override
    @DistributedLock(key = "'review:approve:' + #id", waitTime = 0, leaseTime = 10)
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @Caching(
            put = {
                    @CachePut(value = CacheName.REVIEW_DETAIL,
                              key = "T(com.itjob.util.CacheKeyGenerator).forId(#result.id)")
            },
            evict = {
                    @CacheEvict(value = CacheName.REVIEW_BY_COMPANY, allEntries = true),
                    @CacheEvict(value = CacheName.DASHBOARD_ADMIN,
                                key = "T(com.itjob.util.CacheKeyGenerator).forAdminDashboard()")
            })
    public ReviewResponse approveReview(UUID id, UUID adminId) {
        log.debug("Approving review {} by admin {}", id, adminId);

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        review.setStatus(ReviewStatus.APPROVED.getValue());

        log.info("Review {} approved by admin {}", id, adminId);
        return reviewMapper.toReviewResponse(review);
    }

    @Override
    @DistributedLock(key = "'review:reject:' + #id", waitTime = 0, leaseTime = 10)
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @Caching(
            put = {
                    @CachePut(value = CacheName.REVIEW_DETAIL,
                              key = "T(com.itjob.util.CacheKeyGenerator).forId(#result.id)")
            },
            evict = {
                    @CacheEvict(value = CacheName.REVIEW_BY_COMPANY, allEntries = true),
                    @CacheEvict(value = CacheName.DASHBOARD_ADMIN,
                                key = "T(com.itjob.util.CacheKeyGenerator).forAdminDashboard()")
            })
    public ReviewResponse rejectReview(UUID id, UUID adminId, String reason) {
        log.debug("Rejecting review {} by admin {}: {}", id, adminId, reason);

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        review.setStatus(ReviewStatus.REJECTED.getValue());

        log.info("Review {} rejected by admin {}", id, adminId);
        return reviewMapper.toReviewResponse(review);
    }

    private PageResponse<ReviewResponse> buildPageResponse(Page<Review> reviewPage) {
        return PageResponseUtil.build(reviewPage, reviewMapper::toReviewResponse);
    }
}
