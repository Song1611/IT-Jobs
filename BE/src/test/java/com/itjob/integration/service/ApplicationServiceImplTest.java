package com.itjob.integration.service;

import com.itjob.dto.request.ApplicationRequest;
import com.itjob.dto.response.ApplicationResponse;
import com.itjob.entity.User;
import com.itjob.enums.ApplicationStatus;
import com.itjob.enums.JobStatus;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.ApplicationRepository;
import com.itjob.repository.JobRepository;
import com.itjob.service.ApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("IT - ApplicationService")
class ApplicationServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Test
    @DisplayName("applyForJob -> creates a PENDING application and increments job count")
    void applyForJobCreatesApplication() {
        User employer = employerWithActiveCompany();
        UUID jobId = createOpenJob(employer, "Apply Job");

        User candidate = createCandidate();
        authenticateAsCandidate(candidate);

        ApplicationResponse response = applicationService.applyForJob(
                applyRequest(jobId), candidate.getId());

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.PENDING.getValue());
        assertThat(jobRepository.findById(jobId).orElseThrow().getApplicationCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("applyForJob -> throws ALREADY_APPLIED for a duplicate application")
    void applyForJobDuplicateThrows() {
        User employer = employerWithActiveCompany();
        UUID jobId = createOpenJob(employer, "Duplicate Job");

        User candidate = createCandidate();
        authenticateAsCandidate(candidate);
        applicationService.applyForJob(applyRequest(jobId), candidate.getId());

        ApplicationRequest duplicateRequest = applyRequest(jobId);
        UUID candidateId = candidate.getId();
        assertThatThrownBy(() -> applicationService.applyForJob(duplicateRequest, candidateId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_APPLIED);
    }

    @Test
    @DisplayName("applyForJob -> throws JOB_NOT_OPEN for a draft job")
    void applyForJobNotOpenThrows() {
        User employer = employerWithActiveCompany();
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var request = jobRequest("Draft Apply Job");
        request.setStatus(JobStatus.DRAFT.getValue());
        UUID jobId = jobService.createJob(activeCompanyId(employer), request, employer.getId()).getId();

        User candidate = createCandidate();
        authenticateAsCandidate(candidate);

        ApplicationRequest applyRequest = applyRequest(jobId);
        UUID candidateId = candidate.getId();
        assertThatThrownBy(() -> applicationService.applyForJob(applyRequest, candidateId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.JOB_NOT_OPEN);
    }

    @Test
    @DisplayName("getMyApplications -> returns the candidate's application for the right job")
    void getMyApplicationsReturnsApplications() {
        User employer = employerWithActiveCompany();
        UUID jobId = createOpenJob(employer, "My Job");

        User candidate = createCandidate();
        authenticateAsCandidate(candidate);
        applicationService.applyForJob(applyRequest(jobId), candidate.getId());

        var page = applicationService.getMyApplications(candidate.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getItems())
                .singleElement()
                .satisfies(application -> {
                    assertThat(application.getJob().getId()).isEqualTo(jobId);
                    assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING.getValue());
                });
    }

    @Test
    @DisplayName("withdrawApplication -> changes status to WITHDRAWN and decrements count")
    void withdrawApplicationChangesStatusAndDecrementsCount() {
        User employer = employerWithActiveCompany();
        UUID jobId = createOpenJob(employer, "Withdraw Job");

        User candidate = createCandidate();
        authenticateAsCandidate(candidate);
        var applied = applicationService.applyForJob(
                applyRequest(jobId), candidate.getId());

        applicationService.withdrawApplication(applied.getId(), candidate.getId());

        ApplicationResponse withdrawn = applicationService.getApplicationById(applied.getId(), candidate.getId());
        assertThat(withdrawn.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN.getValue());
        assertThat(jobRepository.findById(jobId).orElseThrow().getApplicationCount()).isZero();
    }

    @Test
    @DisplayName("withdrawApplication -> throws CANNOT_WITHDRAW_APPLICATION when not pending")
    void withdrawNonPendingThrows() {
        User employer = employerWithActiveCompany();
        UUID jobId = createOpenJob(employer, "Approved Job");

        User candidate = createCandidate();
        authenticateAsCandidate(candidate);
        var applied = applicationService.applyForJob(
                applyRequest(jobId), candidate.getId());

        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        applicationService.updateApplicationStatus(
                applied.getId(), activeCompanyId(employer), ApplicationStatus.APPROVED.getValue(), null);

authenticateAsCandidate(candidate);

        UUID appliedId = applied.getId();
        UUID candidateId = candidate.getId();
        assertThatThrownBy(() -> applicationService.withdrawApplication(appliedId, candidateId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_WITHDRAW_APPLICATION);
    }

    @Test
    @DisplayName("applyForJob -> throws JOB_NOT_FOUND for a non-existent job")
    void applyForJobJobNotFoundThrows() {
        User candidate = createCandidate();
        authenticateAsCandidate(candidate);

        ApplicationRequest request = applyRequest(UUID.randomUUID());
        UUID candidateId = candidate.getId();
        assertThatThrownBy(() -> applicationService.applyForJob(request, candidateId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.JOB_NOT_FOUND);
    }

    @Test
    @DisplayName("getApplicationById -> throws APPLICATION_NOT_FOUND for a non-existent application")
    void getApplicationByIdNotFoundThrows() {
        User candidate = createCandidate();
        authenticateAsCandidate(candidate);

        UUID candidateId = candidate.getId();
        UUID nonExistentId = UUID.randomUUID();
        assertThatThrownBy(() -> applicationService.getApplicationById(nonExistentId, candidateId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("withdrawApplication -> throws APPLICATION_NOT_FOUND for a non-existent application")
    void withdrawApplicationNotFoundThrows() {
        User candidate = createCandidate();
        authenticateAsCandidate(candidate);

        UUID candidateId = candidate.getId();
        UUID nonExistentId = UUID.randomUUID();
        assertThatThrownBy(() -> applicationService.withdrawApplication(nonExistentId, candidateId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("getApplicationById -> throws FORBIDDEN for another candidate's application")
    void getApplicationByIdAnotherCandidateThrows() {
        User employer = employerWithActiveCompany();
        UUID jobId = createOpenJob(employer, "Private Job");

        User owner = createCandidate();
        authenticateAsCandidate(owner);
        var applied = applicationService.applyForJob(
                applyRequest(jobId), owner.getId());

        User other = createCandidate();
        authenticateAsCandidate(other);
        UUID appliedId = applied.getId();
        UUID otherId = other.getId();
        assertThatThrownBy(() -> applicationService.getApplicationById(appliedId, otherId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("withdrawApplication -> throws FORBIDDEN for another candidate's application")
    void withdrawApplicationAnotherCandidateThrows() {
        User employer = employerWithActiveCompany();
        UUID jobId = createOpenJob(employer, "Guarded Job");

        User owner = createCandidate();
        authenticateAsCandidate(owner);
        var applied = applicationService.applyForJob(
                applyRequest(jobId), owner.getId());

        User other = createCandidate();
        authenticateAsCandidate(other);
        UUID appliedId = applied.getId();
        UUID otherId = other.getId();
        assertThatThrownBy(() -> applicationService.withdrawApplication(appliedId, otherId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("updateApplicationStatus -> throws APPLICATION_NOT_FOUND for a non-existent application")
    void updateApplicationStatusNotFoundThrows() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");

        UUID randomId = UUID.randomUUID();
        String approvedStatus = ApplicationStatus.APPROVED.getValue();
        assertThatThrownBy(() -> applicationService.updateApplicationStatus(
                randomId, companyId, approvedStatus, null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("updateApplicationStatus -> throws FORBIDDEN when employer does not own the job company")
    void updateApplicationStatusByOtherEmployerThrows() {
        User employer1 = employerWithActiveCompany();
        UUID jobId = createOpenJob(employer1, "Company A Job");

        User candidate = createCandidate();
        authenticateAsCandidate(candidate);
        var applied = applicationService.applyForJob(
                applyRequest(jobId), candidate.getId());

        User employer2 = employerWithActiveCompany();
        UUID company2Id = activeCompanyId(employer2);
        authenticateAs(employer2.getId(), employer2.getEmail(), "EMPLOYER");

        UUID appliedId = applied.getId();
        String approvedStatus = ApplicationStatus.APPROVED.getValue();
        assertThatThrownBy(() -> applicationService.updateApplicationStatus(
                appliedId, company2Id, approvedStatus, null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("updateApplicationStatus -> employer updates application status")
    void updateApplicationStatusByEmployer() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        UUID jobId = createOpenJob(employer, "Status Job");

        User candidate = createCandidate();
        authenticateAsCandidate(candidate);
        var applied = applicationService.applyForJob(
                applyRequest(jobId), candidate.getId());

        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        ApplicationResponse updated = applicationService.updateApplicationStatus(
                applied.getId(), companyId, ApplicationStatus.REVIEWING.getValue(), "reviewing now");

        assertThat(updated.getStatus()).isEqualTo(ApplicationStatus.REVIEWING.getValue());
        assertThat(updated.getHrNotes()).isEqualTo("reviewing now");
        assertThat(updated.getReviewedAt()).isNotNull();
    }

    private User createCandidate() {
        return createVerifiedUser("candidate-" + UUID.randomUUID() + "@example.com");
    }

    private void authenticateAsCandidate(User user) {
        authenticateAs(user.getId(), user.getEmail(), "USER");
    }

    private ApplicationRequest applyRequest(UUID jobId) {
        return ApplicationRequest.builder().jobId(jobId).build();
    }

    @Test
    @DisplayName("getJobApplications -> employer views applications for a job")
    void getJobApplicationsReturnsApplications() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        UUID jobId = createOpenJob(employer, "Job With Apps");

        User candidate = createCandidate();
        authenticateAsCandidate(candidate);
        applicationService.applyForJob(applyRequest(jobId), candidate.getId());

        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var page = applicationService.getJobApplications(jobId, companyId, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getJobApplications -> throws FORBIDDEN when the job is not in the company")
    void getJobApplicationsOtherCompanyThrows() {
        User employer1 = employerWithActiveCompany();
        UUID jobId = createOpenJob(employer1, "Private Job");

        User employer2 = employerWithActiveCompany();
        UUID company2Id = activeCompanyId(employer2);
        authenticateAs(employer2.getId(), employer2.getEmail(), "EMPLOYER");

        assertThatThrownBy(() -> applicationService.getJobApplications(
                jobId, company2Id, null, PageRequest.of(0, 10)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("getCompanyApplications -> employer views all applications for the company")
    void getCompanyApplicationsReturnsApplications() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        UUID jobId = createOpenJob(employer, "Company Apps Job");

        User candidate = createCandidate();
        authenticateAsCandidate(candidate);
        applicationService.applyForJob(applyRequest(jobId), candidate.getId());

        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var page = applicationService.getCompanyApplications(companyId, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("updateApplicationStatus REJECTED -> sets the status to rejected")
    void updateApplicationStatusRejected() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        UUID jobId = createOpenJob(employer, "Reject Candidate Job");

        User candidate = createCandidate();
        authenticateAsCandidate(candidate);
        var applied = applicationService.applyForJob(applyRequest(jobId), candidate.getId());

        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        ApplicationResponse rejected = applicationService.updateApplicationStatus(
                applied.getId(), companyId, ApplicationStatus.REJECTED.getValue(), "not qualified");

        assertThat(rejected.getStatus()).isEqualTo(ApplicationStatus.REJECTED.getValue());
    }

    @Test
    @DisplayName("markAsViewed -> marks the application as viewed by the employer")
    void markAsViewedMarksApplication() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        UUID jobId = createOpenJob(employer, "Mark Viewed Job");

        User candidate = createCandidate();
        authenticateAsCandidate(candidate);
        var applied = applicationService.applyForJob(applyRequest(jobId), candidate.getId());

        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        applicationService.markAsViewed(applied.getId(), companyId);

        var persisted = applicationRepository.findById(applied.getId()).orElseThrow();
        assertThat(persisted.getViewedByEmployer()).isTrue();
        assertThat(persisted.getViewedAt()).isNotNull();
    }
}