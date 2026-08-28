package com.itjob.integration.service;

import com.itjob.dto.request.ApplicationRequest;
import com.itjob.dto.request.JobRequest;
import com.itjob.dto.response.JobResponse;
import com.itjob.entity.Job;
import com.itjob.entity.Skill;
import com.itjob.entity.User;
import com.itjob.enums.JobStatus;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.redis.RedisKeys;
import com.itjob.repository.JobRepository;
import com.itjob.repository.SkillRepository;
import com.itjob.service.ApplicationService;
import com.itjob.service.TrendingJobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("IT - JobService")
class JobServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TrendingJobService trendingJobService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SkillRepository skillRepository;

    @Test
    @DisplayName("createJob -> creates an open job for an active owned company")
    void createJobCreatesOpenJob() {
        User employer = employerWithActiveCompany();
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        JobResponse created = jobService.createJob(
                activeCompanyId(employer), jobRequest("Backend Engineer"), employer.getId());

        assertThat(created.getId()).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Backend Engineer");
        assertThat(created.getStatus()).isEqualTo(JobStatus.OPEN.getValue());
        assertThat(created.getViewCount()).isZero();
        assertThat(created.getApplicationCount()).isZero();
    }

    @Test
    @DisplayName("createJob -> throws COMPANY_NOT_ACTIVE for a pending company")
    void createJobForPendingCompanyThrows() {
        User employer = createVerifiedUser("emp-" + UUID.randomUUID() + "@example.com");
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var pending = companyService.createCompany(companyRequest("Pending Co"), employer.getId());

        UUID pendingId = pending.getId();
        JobRequest request = jobRequest("Job A");
        UUID employerId = employer.getId();
        assertThatThrownBy(() -> jobService.createJob(pendingId, request, employerId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_NOT_ACTIVE);
    }

    @Test
    @DisplayName("createJob -> throws FORBIDDEN when company belongs to another user")
    void createJobForOtherCompanyThrows() {
        User owner = employerWithActiveCompany();
        authenticateAs(owner.getId(), owner.getEmail(), "EMPLOYER");
        var companyId = activeCompanyId(owner);

        User other = createVerifiedUser("other-" + UUID.randomUUID() + "@example.com");
        authenticateAs(other.getId(), other.getEmail(), "EMPLOYER");

        JobRequest request = jobRequest("Job B");
        UUID otherId = other.getId();
        assertThatThrownBy(() -> jobService.createJob(companyId, request, otherId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("getFeaturedJobs -> returns open jobs up to the limit")
    void getFeaturedJobsReturnsOpenJobs() {
        User employer = employerWithActiveCompany();
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        jobService.createJob(activeCompanyId(employer), jobRequest("Featured Job"), employer.getId());

        var featured = jobService.getFeaturedJobs(10);

        assertThat(featured).extracting("title").contains("Featured Job");
    }

    @Test
    @DisplayName("getFeaturedJobs -> throws INVALID_LIMIT for non-positive limit")
    void getFeaturedJobsInvalidLimitThrows() {
        assertThatThrownBy(() -> jobService.getFeaturedJobs(0))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_LIMIT);
    }

    // @Transactional keeps the JPA session open while searchJobs maps lazy skills,
    // mirroring the request-scoped session (OSIV) production runs under.
    @Test
    @Transactional
    @DisplayName("searchJobs -> returns open jobs and excludes draft jobs")
    void searchJobsReturnsOpenJobs() {
        User employer = employerWithActiveCompany();
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        UUID companyId = activeCompanyId(employer);
        jobService.createJob(companyId, jobRequest("Open Job"), employer.getId());

        JobRequest draft = jobRequest("Draft Job");
        draft.setStatus(JobStatus.DRAFT.getValue());
        jobService.createJob(companyId, draft, employer.getId());

        var page = jobService.searchJobs(null, PageRequest.of(0, 100));

        assertThat(page.getItems())
                .extracting(JobResponse::getTitle)
                .contains("Open Job")
                .doesNotContain("Draft Job");
    }

    @Test
    @DisplayName("getJobById -> returns the job and records a view")
    void getJobByIdReturnsJob() {
        User employer = employerWithActiveCompany();
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var created = jobService.createJob(activeCompanyId(employer), jobRequest("Viewed Job"), employer.getId());

        JobResponse response = jobService.getJobById(created.getId(), null);

        assertThat(response.getId()).isEqualTo(created.getId());
        assertThat(response.getTitle()).isEqualTo("Viewed Job");
        assertThat(response.getCompany()).isNotNull();
        assertThat(response.getViewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("getJobById -> throws JOB_NOT_FOUND for a non-existent job")
    void getJobByIdNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> jobService.getJobById(randomId, null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.JOB_NOT_FOUND);
    }

    @Test
    @DisplayName("approveJob -> changes a draft job to OPEN")
    void approveJobOpensDraft() {
        User employer = employerWithActiveCompany();
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        JobRequest draft = jobRequest("Draft Job");
        draft.setStatus(JobStatus.DRAFT.getValue());
        var created = jobService.createJob(activeCompanyId(employer), draft, employer.getId());

        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");
        jobService.approveJob(created.getId(), admin.getId());

        JobResponse after = jobService.getJobById(created.getId(), null);
        assertThat(after.getStatus()).isEqualTo(JobStatus.OPEN.getValue());
    }

    @Test
    @DisplayName("approveJob -> throws JOB_ALREADY_PROCESSED for an open job")
    void approveJobAlreadyProcessedThrows() {
        User employer = employerWithActiveCompany();
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var created = jobService.createJob(activeCompanyId(employer), jobRequest("Open Job"), employer.getId());

        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");

        UUID createdId = created.getId();
        UUID adminId = admin.getId();
        assertThatThrownBy(() -> jobService.approveJob(createdId, adminId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.JOB_ALREADY_PROCESSED);
    }

    @Test
    @DisplayName("getCompanyJobs -> returns the company's jobs, optionally filtered by status")
    void getCompanyJobsReturnsJobsFilteredByStatus() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        jobService.createJob(companyId, jobRequest("Open Job"), employer.getId());

        JobRequest draft = jobRequest("Draft Job");
        draft.setStatus(JobStatus.DRAFT.getValue());
        jobService.createJob(companyId, draft, employer.getId());

        var all = jobService.getCompanyJobs(companyId, null, PageRequest.of(0, 10));
        var open = jobService.getCompanyJobs(companyId, JobStatus.OPEN.getValue(), PageRequest.of(0, 10));

        assertThat(all.getTotalElements()).isEqualTo(2);
        assertThat(open.getItems()).extracting(JobResponse::getTitle).containsExactly("Open Job");
    }

    @Test
    @DisplayName("updateJob -> employer updates the title of own job")
    void updateJobUpdatesTitle() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var created = jobService.createJob(companyId, jobRequest("Original Title"), employer.getId());

        JobResponse updated = jobService.updateJob(
                created.getId(), companyId, jobRequest("Updated Title"), employer.getId());

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
    }

    @Test
    @DisplayName("updateJob -> throws FORBIDDEN when the company belongs to another user")
    void updateJobForOtherCompanyThrows() {
        User owner = employerWithActiveCompany();
        UUID companyId = activeCompanyId(owner);
        authenticateAs(owner.getId(), owner.getEmail(), "EMPLOYER");
        var created = jobService.createJob(companyId, jobRequest("Owned Job"), owner.getId());

        User other = createVerifiedUser("other-" + UUID.randomUUID() + "@example.com");
        authenticateAs(other.getId(), other.getEmail(), "EMPLOYER");

        UUID jobId = created.getId();
        UUID otherId = other.getId();
        JobRequest hijackRequest = jobRequest("Hijack");
        assertThatThrownBy(() -> jobService.updateJob(jobId, companyId, hijackRequest, otherId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("deleteJob -> closes the job (soft delete)")
    void deleteJobClosesJob() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var created = jobService.createJob(companyId, jobRequest("To Delete"), employer.getId());

        jobService.deleteJob(created.getId(), companyId, employer.getId());

        Job job = jobRepository.findById(created.getId()).orElseThrow();
        assertThat(job.getStatus()).isEqualTo(JobStatus.CLOSED.getValue());
    }

    @Test
    @DisplayName("getAllJobs -> admin can list jobs")
    void getAllJobsReturnsAllJobs() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        jobService.createJob(companyId, jobRequest("Admin List Job"), employer.getId());

        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");

        var page = jobService.getAllJobs(null, PageRequest.of(0, 100));

        assertThat(page.getItems()).extracting("title").contains("Admin List Job");
    }

    @Test
    @DisplayName("rejectJob -> admin rejects a draft job")
    void rejectJobRejectsDraft() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        JobRequest draft = jobRequest("To Reject");
        draft.setStatus(JobStatus.DRAFT.getValue());
        var created = jobService.createJob(companyId, draft, employer.getId());

        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");
        jobService.rejectJob(created.getId(), admin.getId(), "not a real job");

        JobResponse after = jobService.getJobById(created.getId(), null);
        assertThat(after.getStatus()).isEqualTo(JobStatus.REJECTED.getValue());
    }

    @Test
    @DisplayName("getFeaturedJobs -> throws LIMIT_EXCEEDED above the max limit")
    void getFeaturedJobsAboveMaxLimitThrows() {
        assertThatThrownBy(() -> jobService.getFeaturedJobs(101))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.LIMIT_EXCEEDED);
    }

    @Test
    @Transactional
    @DisplayName("getTrendingJobs -> falls back to featured jobs when nothing is trending")
    void getTrendingJobsFallsBackToFeatured() {
        // trending scores from other tests share the daily zset, so clear it first
        stringRedisTemplate.delete(RedisKeys.trendingDailyKey());

        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        jobService.createJob(companyId, jobRequest("Fallback Job"), employer.getId());

        var trending = jobService.getTrendingJobs(7);

        assertThat(trending).extracting(JobResponse::getTitle).contains("Fallback Job");
    }

    @Test
    @Transactional
    @DisplayName("getTrendingJobs -> returns jobs recorded as trending")
    void getTrendingJobsReturnsTrending() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var created = jobService.createJob(companyId, jobRequest("Trending Job"), employer.getId());

        trendingJobService.recordScore(created.getId(), 10.0);

        var trending = jobService.getTrendingJobs(10);

        assertThat(trending).extracting(JobResponse::getId).contains(created.getId());
    }

    @Test
    @Transactional
    @DisplayName("searchJobs -> applies a provided filter")
    void searchJobsAppliesFilter() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        jobService.createJob(companyId, jobRequest("Filtered Job"), employer.getId());

        String[] filters = {"status@open"};
        var page = jobService.searchJobs(filters, PageRequest.of(0, 100));

        assertThat(page.getItems()).extracting(JobResponse::getTitle).contains("Filtered Job");
    }

    @Test
    @Transactional
    @DisplayName("getJobById -> marks job as applied and records recent view for the current user")
    void getJobByIdForCurrentUserRecordsViewAndApplied() {
        User employer = employerWithActiveCompany();
        UUID jobId = createOpenJob(employer, "Applied Job");

        User candidate = createVerifiedUser("candidate-" + UUID.randomUUID() + "@example.com");
        authenticateAs(candidate.getId(), candidate.getEmail(), "USER");
        applicationService.applyForJob(ApplicationRequest.builder().jobId(jobId).build(), candidate.getId());

        JobResponse response = jobService.getJobById(jobId, candidate.getId());

        assertThat(response.getIsApplied()).isTrue();
        assertThat(jobService.getRecentlyViewedJobs(candidate.getId(), 10))
                .extracting(JobResponse::getId)
                .contains(jobId);
    }

    @Test
    @DisplayName("getCompanyJobs -> empty when the company has no jobs")
    void getCompanyJobsEmptyWhenNoJobs() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");

        var page = jobService.getCompanyJobs(companyId, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getItems()).isEmpty();
    }

    @Test
    @DisplayName("createJob -> throws COMPANY_NOT_FOUND for a non-existent company")
    void createJobCompanyNotFoundThrows() {
        User employer = createVerifiedUser("emp-" + UUID.randomUUID() + "@example.com");
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");

        UUID randomCompanyId = UUID.randomUUID();
        UUID employerId = employer.getId();
        JobRequest request = jobRequest("Job");
        assertThatThrownBy(() -> jobService.createJob(randomCompanyId, request, employerId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);
    }

    @Test
    @DisplayName("updateJob -> throws JOB_NOT_FOUND for a non-existent job")
    void updateJobNotFoundThrows() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");

        UUID randomJobId = UUID.randomUUID();
        UUID employerId = employer.getId();
        JobRequest request = jobRequest("Updated");
        assertThatThrownBy(() -> jobService.updateJob(randomJobId, companyId, request, employerId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.JOB_NOT_FOUND);
    }

    @Test
    @DisplayName("deleteJob -> throws JOB_NOT_FOUND for a non-existent job")
    void deleteJobNotFoundThrows() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");

        UUID randomJobId = UUID.randomUUID();
        UUID employerId = employer.getId();
        assertThatThrownBy(() -> jobService.deleteJob(randomJobId, companyId, employerId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.JOB_NOT_FOUND);
    }

    @Test
    @DisplayName("rejectJob -> throws JOB_ALREADY_PROCESSED for an open job")
    void rejectJobAlreadyProcessedThrows() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var created = jobService.createJob(companyId, jobRequest("Open Job"), employer.getId());

        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");

        UUID createdId = created.getId();
        UUID adminId = admin.getId();
        assertThatThrownBy(() -> jobService.rejectJob(createdId, adminId, "reason"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.JOB_ALREADY_PROCESSED);
    }

    @Test
    @DisplayName("getAllJobs -> filters by status")
    void getAllJobsFiltersByStatus() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        jobService.createJob(companyId, jobRequest("Open Only Job"), employer.getId());

        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");

        var page = jobService.getAllJobs(JobStatus.OPEN.getValue(), PageRequest.of(0, 100));

        assertThat(page.getItems()).extracting(JobResponse::getTitle).contains("Open Only Job");
    }

    @Test
    @Transactional
    @DisplayName("createJob -> attaches the requested skills to the job")
    void createJobWithSkills() {
        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        Skill skill = skillRepository.save(Skill.builder().name("Java").build());

        JobRequest request = jobRequest("Skilled Job");
        request.setSkillIds(Set.of(skill.getId()));
        JobResponse created = jobService.createJob(companyId, request, employer.getId());

        assertThat(created.getId()).isNotNull();
        Job persisted = jobRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getSkills()).extracting(Skill::getName).contains("Java");
    }
}