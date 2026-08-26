package com.itjob.integration.repository;

import com.itjob.config.AbstractPostgresIntegrationTest;
import com.itjob.entity.Application;
import com.itjob.entity.Company;
import com.itjob.entity.Job;
import com.itjob.entity.User;
import com.itjob.enums.ApplicationStatus;
import com.itjob.fixture.TestDataFactory;
import com.itjob.repository.ApplicationRepository;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.JobRepository;
import com.itjob.repository.UserRepository;
import com.itjob.repository.projection.JobApplicationCountProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("IT - ApplicationRepository")
class ApplicationRepositoryTest extends AbstractPostgresIntegrationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 1, 10, 0);

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("save -> assigns defaults (status PENDING, appliedAt, timestamps)")
    void saveAssignsDefaults() {
        User user = TestDataFactory.user(userRepository, "candidate@example.com");
        Job job = openJob(openCompany());

        Application saved = applicationRepository.save(application(user, job));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.PENDING.getValue());
        assertThat(saved.getAppliedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getViewedByEmployer()).isFalse();
    }

    @Test
    @DisplayName("existsByJobIdAndUserId -> true when user applied")
    void existsByJobIdAndUserIdTrueWhenApplied() {
        User user = TestDataFactory.user(userRepository, "candidate@example.com");
        Job job = openJob(openCompany());
        applicationRepository.save(application(user, job));

        boolean exists = applicationRepository.existsByJobIdAndUserId(job.getId(), user.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByJobIdAndUserId -> false when user did not apply")
    void existsByJobIdAndUserIdFalseWhenNotApplied() {
        User user = TestDataFactory.user(userRepository, "candidate@example.com");
        Job job = openJob(openCompany());

        boolean exists = applicationRepository.existsByJobIdAndUserId(job.getId(), user.getId());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findByJobIdAndUserId -> returns application when exists")
    void findByJobIdAndUserIdReturnsApplication() {
        User user = TestDataFactory.user(userRepository, "candidate@example.com");
        Job job = openJob(openCompany());
        applicationRepository.save(application(user, job));

        var result = applicationRepository.findByJobIdAndUserId(job.getId(), user.getId());

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("findByJobIdAndUserId -> empty when no application")
    void findByJobIdAndUserIdReturnsEmpty() {
        User user = TestDataFactory.user(userRepository, "candidate@example.com");
        Job job = openJob(openCompany());

        var result = applicationRepository.findByJobIdAndUserId(job.getId(), user.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUserIdOrderByAppliedAtDesc -> returns newest first with fixed timestamps")
    void findByUserIdOrderByAppliedAtDesc() {
        User user = TestDataFactory.user(userRepository, "candidate@example.com");
        Application oldest = applicationRepository.save(application(user, openJob(openCompany())));
        oldest.setAppliedAt(NOW.minusDays(2));
        Application middle = applicationRepository.save(application(user, openJob(openCompany())));
        middle.setAppliedAt(NOW.minusDays(1));
        Application newest = applicationRepository.save(application(user, openJob(openCompany())));
        newest.setAppliedAt(NOW);
        applicationRepository.flush();

        var page = applicationRepository.findByUserIdOrderByAppliedAtDesc(user.getId(), PageRequest.of(0, 5));

        assertThat(page.getContent())
                .extracting(Application::getAppliedAt)
                .isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @DisplayName("findByUserIdOrderByAppliedAtDesc -> paginates correctly")
    void findByUserIdOrderByAppliedAtDescPaginates() {
        User user = TestDataFactory.user(userRepository, "candidate@example.com");
        applicationRepository.save(application(user, openJob(openCompany())));
        applicationRepository.save(application(user, openJob(openCompany())));
        applicationRepository.save(application(user, openJob(openCompany())));

        var firstPage = applicationRepository.findByUserIdOrderByAppliedAtDesc(
                user.getId(), PageRequest.of(0, 2));
        var secondPage = applicationRepository.findByUserIdOrderByAppliedAtDesc(
                user.getId(), PageRequest.of(1, 2));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.isLast()).isTrue();
    }

    @Test
    @DisplayName("findByUserId -> returns all applications of user")
    void findByUserIdReturnsList() {
        User user = TestDataFactory.user(userRepository, "candidate@example.com");
        applicationRepository.save(application(user, openJob(openCompany())));
        applicationRepository.save(application(user, openJob(openCompany())));

        List<Application> result = applicationRepository.findByUserId(user.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByJobIdAndStatusOrderByAppliedAtDesc -> filters by status")
    void findByJobIdAndStatusOrderByAppliedAtDesc() {
        User user = TestDataFactory.user(userRepository, "candidate@example.com");
        Job job = openJob(openCompany());
        Application approved = applicationRepository.save(application(user, job));
        approved.setStatus(ApplicationStatus.APPROVED.getValue());
        applicationRepository.save(approved);
        applicationRepository.save(application(
                TestDataFactory.user(userRepository, "other-" + UUID.randomUUID() + "@example.com"), job));

        var result = applicationRepository.findByJobIdAndStatusOrderByAppliedAtDesc(
                job.getId(), ApplicationStatus.APPROVED.getValue(), PageRequest.of(0, 5));

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("findByCompanyId -> returns applications through job company")
    void findByCompanyIdReturnsApplications() {
        User user = TestDataFactory.user(userRepository, "candidate@example.com");
        Company company = openCompany();
        Job job = openJob(company);
        applicationRepository.save(application(user, job));

        var result = applicationRepository.findByCompanyId(company.getId(), PageRequest.of(0, 5));

        assertThat(result).hasSize(1);
        assertThat(result.getContent().getFirst().getJob().getCompany().getId()).isEqualTo(company.getId());
    }

    @Test
    @DisplayName("countByCompanyId -> counts applications across company jobs")
    void countByCompanyId() {
        User user = TestDataFactory.user(userRepository, "candidate@example.com");
        Company company = openCompany();
        applicationRepository.save(application(user, openJob(company)));
        applicationRepository.save(application(user, openJob(company)));

        long count = applicationRepository.countByCompanyId(company.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countNewApplicationsByCompanyId -> counts applications after given time")
    void countNewApplicationsByCompanyId() {
        User user = TestDataFactory.user(userRepository, "candidate@example.com");
        Company company = openCompany();
        Job job = openJob(company);
        Application application = applicationRepository.save(application(user, job));
        application.setAppliedAt(NOW.minusDays(1));
        applicationRepository.flush();

        long since = applicationRepository.countNewApplicationsByCompanyId(
                company.getId(), ApplicationStatus.PENDING.getValue(), NOW.minusDays(2));
        long after = applicationRepository.countNewApplicationsByCompanyId(
                company.getId(), ApplicationStatus.PENDING.getValue(), NOW);

        assertThat(since).isEqualTo(1);
        assertThat(after).isZero();
    }

    @Test
    @DisplayName("countApplicationsByJobIds -> returns projection with per-job counts")
    void countApplicationsByJobIdsReturnsProjection() {
        User user = TestDataFactory.user(userRepository, "candidate@example.com");
        Job job1 = openJob(openCompany());
        Job job2 = openJob(openCompany());
        applicationRepository.save(application(user, job1));
        applicationRepository.save(application(user, job1));
        applicationRepository.save(application(user, job2));

        List<JobApplicationCountProjection> projections =
                applicationRepository.countApplicationsByJobIds(List.of(job1.getId(), job2.getId()));

        assertThat(projections).hasSize(2);
        JobApplicationCountProjection job1Count = projections.stream()
                .filter(p -> p.getJobId().equals(job1.getId())).findFirst().orElseThrow();
        JobApplicationCountProjection job2Count = projections.stream()
                .filter(p -> p.getJobId().equals(job2.getId())).findFirst().orElseThrow();
        assertThat(job1Count.getApplicationCount()).isEqualTo(2);
        assertThat(job2Count.getApplicationCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("countByJobId / countByUserId -> return correct counts")
    void countByJobAndUser() {
        User user = TestDataFactory.user(userRepository, "candidate@example.com");
        Job job = openJob(openCompany());
        applicationRepository.save(application(user, job));

        assertThat(applicationRepository.countByJobId(job.getId())).isEqualTo(1);
        assertThat(applicationRepository.countByUserId(user.getId())).isEqualTo(1);
    }

    private Company openCompany() {
        return TestDataFactory.activeCompany(companyRepository, "ac-" + UUID.randomUUID());
    }

    private Job openJob(Company company) {
        return TestDataFactory.job(jobRepository, company, "Job " + UUID.randomUUID());
    }

    private Application application(User user, Job job) {
        return Application.builder().job(job).user(user).build();
    }
}