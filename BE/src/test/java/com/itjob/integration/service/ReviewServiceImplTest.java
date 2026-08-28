package com.itjob.integration.service;

import com.itjob.dto.request.ReviewRequest;
import com.itjob.dto.response.ReviewResponse;
import com.itjob.entity.Company;
import com.itjob.entity.User;
import com.itjob.enums.CompanyStatus;
import com.itjob.enums.ReviewStatus;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.CompanyRepository;
import com.itjob.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("IT - ReviewService")
class ReviewServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("createReview -> creates a PENDING review for an active company")
    void createReviewCreatesPending() {
        User user = createVerifiedUser("reviewer-" + UUID.randomUUID() + "@example.com");
        authenticateAs(user.getId(), user.getEmail(), "USER");
        Company company = activeCompany("Review Co");

        ReviewResponse response = reviewService.createReview(company.getId(), user.getId(), reviewRequest());

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ReviewStatus.PENDING.getValue());
        assertThat(response.getRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("createReview -> throws COMPANY_NOT_ACTIVE for a pending company")
    void createReviewCompanyNotActiveThrows() {
        User user = createVerifiedUser("reviewer-" + UUID.randomUUID() + "@example.com");
        authenticateAs(user.getId(), user.getEmail(), "USER");
        Company company = companyRepository.save(Company.builder()
                .name("Pending Co").slug("pc-" + UUID.randomUUID()).status(CompanyStatus.PENDING.getValue()).build());

        UUID companyId = company.getId();
        UUID userId = user.getId();
        ReviewRequest request = reviewRequest();
        assertThatThrownBy(() -> reviewService.createReview(companyId, userId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_NOT_ACTIVE);
    }

    @Test
    @DisplayName("createReview -> throws ALREADY_REVIEWED for a duplicate review")
    void createReviewDuplicateThrows() {
        User user = createVerifiedUser("reviewer-" + UUID.randomUUID() + "@example.com");
        authenticateAs(user.getId(), user.getEmail(), "USER");
        Company company = activeCompany("Reviewed Co");
        reviewService.createReview(company.getId(), user.getId(), reviewRequest());

        UUID companyId = company.getId();
        UUID userId = user.getId();
        ReviewRequest request = reviewRequest();
        assertThatThrownBy(() -> reviewService.createReview(companyId, userId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_REVIEWED);
    }

    @Test
    @DisplayName("approveReview -> admin changes status to APPROVED")
    void approveReviewApproves() {
        User user = createVerifiedUser("reviewer-" + UUID.randomUUID() + "@example.com");
        authenticateAs(user.getId(), user.getEmail(), "USER");
        Company company = activeCompany("Approve Co");
        ReviewResponse created = reviewService.createReview(company.getId(), user.getId(), reviewRequest());

        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");
        ReviewResponse approved = reviewService.approveReview(created.getId(), admin.getId());

        assertThat(approved.getStatus()).isEqualTo(ReviewStatus.APPROVED.getValue());
    }

    @Test
    @DisplayName("rejectReview -> admin changes status to REJECTED")
    void rejectReviewRejects() {
        User user = createVerifiedUser("reviewer-" + UUID.randomUUID() + "@example.com");
        authenticateAs(user.getId(), user.getEmail(), "USER");
        Company company = activeCompany("Reject Co");
        ReviewResponse created = reviewService.createReview(company.getId(), user.getId(), reviewRequest());

        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");
        ReviewResponse rejected = reviewService.rejectReview(created.getId(), admin.getId(), "not helpful");

        assertThat(rejected.getStatus()).isEqualTo(ReviewStatus.REJECTED.getValue());
    }

    @Test
    @DisplayName("getCompanyReviews -> returns approved reviews only")
    void getCompanyReviewsReturnsApproved() {
        User user = createVerifiedUser("reviewer-" + UUID.randomUUID() + "@example.com");
        authenticateAs(user.getId(), user.getEmail(), "USER");
        Company company = activeCompany("List Co");
        ReviewResponse created = reviewService.createReview(company.getId(), user.getId(), reviewRequest());
        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");
        reviewService.approveReview(created.getId(), admin.getId());

        var page = reviewService.getCompanyReviews(company.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getItems()).allSatisfy(review ->
                assertThat(review.getStatus()).isEqualTo(ReviewStatus.APPROVED.getValue()));
    }

    @Test
    @DisplayName("getReviewById -> throws REVIEW_NOT_FOUND for a missing review")
    void getReviewByIdNotFoundThrows() {
        assertThatThrownBy(() -> reviewService.getReviewById(UUID.randomUUID()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
    }

    private Company activeCompany(String name) {
        return companyRepository.save(Company.builder()
                .name(name).slug(name.toLowerCase().replace(' ', '-') + "-" + UUID.randomUUID())
                .status(CompanyStatus.ACTIVE.getValue())
                .build());
    }

    private ReviewRequest reviewRequest() {
        return ReviewRequest.builder()
                .rating(5).salaryRating(4).cultureRating(5)
                .managementRating(4).workLifeBalanceRating(5)
                .title("Great place").comment("Really enjoyed it")
                .build();
    }
}