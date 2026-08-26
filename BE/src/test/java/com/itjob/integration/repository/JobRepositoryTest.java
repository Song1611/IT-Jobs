package com.itjob.integration.repository;

import com.itjob.config.AbstractPostgresIntegrationTest;
import com.itjob.entity.Company;
import com.itjob.entity.Job;
import com.itjob.entity.Skill;
import com.itjob.enums.CompanyStatus;
import com.itjob.enums.JobStatus;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.JobRepository;
import com.itjob.repository.SkillRepository;
import com.itjob.repository.projection.CompanyJobCountProjection;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("IT - JobRepository")
class JobRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("save -> assigns defaults (viewCount, quantity, salaryCurrency)")
    void saveAssignsDefaults() {
        // Arrange
        Company company = companyRepository.save(activatedCompany("defaults-co"));

        // Act
        Job saved = jobRepository.save(job(company, JobStatus.OPEN.getValue()));

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getViewCount()).isZero();
        assertThat(saved.getApplicationCount()).isZero();
        assertThat(saved.getQuantity()).isEqualTo(1);
        assertThat(saved.getSalaryCurrency()).isEqualTo("VND");
    }

    @Test
    @DisplayName("findById -> loads company and skills eagerly")
    void findByIdLoadsCompanyAndSkills() {
        // Arrange
        Skill skill = skillRepository.save(Skill.builder().name("Java").build());
        Company company = companyRepository.save(activatedCompany("eager-co"));
        Job job = jobRepository.save(job(company, JobStatus.OPEN.getValue()));
        job.setSkills(new HashSet<>(Set.of(skill)));
        jobRepository.save(job);

        UUID id = job.getId();
        jobRepository.flush();

        // Act
        Job found = jobRepository.findById(id).orElseThrow();

        // Assert
        assertThat(found.getCompany().getName()).isEqualTo("eager-co");
        assertThat(found.getSkills()).extracting(Skill::getName).containsExactly("Java");
    }

    @Test
    @DisplayName("findFeaturedJobs -> returns open jobs ordered by createdAt desc")
    void findFeaturedJobsReturnsOpenJobs() {
        // Arrange
        Company company = companyRepository.save(activatedCompany("featured-co"));
        Job older = jobRepository.save(job(company, JobStatus.OPEN.getValue(), "Older Job"));
        Job newer = jobRepository.save(job(company, JobStatus.OPEN.getValue(), "Newer Job"));
        jobRepository.save(job(company, JobStatus.DRAFT.getValue(), "Draft Job"));
        older.setCreatedAt(LocalDateTime.now().minusDays(1));
        newer.setCreatedAt(LocalDateTime.now());
        jobRepository.flush();

        // Act
        List<Job> result = jobRepository.findFeaturedJobs(JobStatus.OPEN.getValue(), PageRequest.of(0, 10));

        // Assert
        assertThat(result).extracting(Job::getTitle).containsExactly("Newer Job", "Older Job");
    }

    @Test
    @DisplayName("findLatestOpenJobs -> returns open jobs with company and skills")
    void findLatestOpenJobsReturnsOpenJobs() {
        // Arrange
        Skill skill = skillRepository.save(Skill.builder().name("Spring").build());
        Company company = companyRepository.save(activatedCompany("open-co"));
        Job job = jobRepository.save(job(company, JobStatus.OPEN.getValue(), "Open Job"));
        job.setSkills(new HashSet<>(Set.of(skill)));
        jobRepository.save(job);
        jobRepository.save(job(company, JobStatus.CLOSED.getValue(), "Closed Job"));

        List<Job> result = jobRepository.findLatestOpenJobs(JobStatus.OPEN.getValue(), PageRequest.of(0, 10));

        assertThat(result).extracting(Job::getTitle).containsExactly("Open Job");
        assertThat(result.getFirst().getCompany().getName()).isEqualTo("open-co");
        assertThat(result.getFirst().getSkills()).extracting(Skill::getName).containsExactly("Spring");
    }

    @Test
    @DisplayName("findByCompanyId -> paginates correctly")
    void findByCompanyIdPaginated() {
        // Arrange
        Company company = companyRepository.save(activatedCompany("paginated-co"));
        jobRepository.save(job(company, JobStatus.OPEN.getValue(), "Job 1"));
        jobRepository.save(job(company, JobStatus.OPEN.getValue(), "Job 2"));
        jobRepository.save(job(company, JobStatus.OPEN.getValue(), "Job 3"));

        // Act
        var firstPage = jobRepository.findByCompanyId(company.getId(), PageRequest.of(0, 2));
        var secondPage = jobRepository.findByCompanyId(company.getId(), PageRequest.of(1, 2));

        // Assert
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.isFirst()).isTrue();
        assertThat(firstPage.isLast()).isFalse();
        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.isLast()).isTrue();
    }

    @Test
    @DisplayName("findBySlug -> returns job when slug exists")
    void findBySlugReturnsJob() {
        // Arrange
        Company company = companyRepository.save(activatedCompany("slug-co"));
        jobRepository.save(Job.builder().company(company).title("Unique Slug Job").slug("unique-slug-job").status(JobStatus.OPEN.getValue()).build());

        Optional<Job> result = jobRepository.findBySlug("unique-slug-job");

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Unique Slug Job");
    }

    @Test
    @DisplayName("findBySlug -> empty when slug does not exist")
    void findBySlugReturnsEmptyWhenNotFound() {
        // Act
        Optional<Job> result = jobRepository.findBySlug("non-existent-slug");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByCompanyIdAndStatus -> returns jobs with company and status")
    void findByCompanyIdAndStatus() {
        // Arrange
        Company company = companyRepository.save(activatedCompany("co-status"));
        jobRepository.save(job(company, JobStatus.OPEN.getValue(), "Open Job"));
        jobRepository.save(job(company, JobStatus.DRAFT.getValue(), "Draft Job"));

        // Act
        List<Job> result = jobRepository.findByCompanyIdAndStatus(
                company.getId(), JobStatus.OPEN.getValue(), PageRequest.of(0, 10)).getContent();

        // Assert
        assertThat(result).hasSize(1).extracting(Job::getTitle).containsExactly("Open Job");
    }

    @Test
    @DisplayName("findByStatus -> returns jobs with requested status")
    void findByStatus() {
        // Arrange
        Company company = companyRepository.save(activatedCompany("status-co"));
        jobRepository.save(job(company, JobStatus.OPEN.getValue(), "Open Job"));
        jobRepository.save(job(company, JobStatus.OPEN.getValue(), "Another Open"));
        jobRepository.save(job(company, JobStatus.CLOSED.getValue(), "Closed Job"));

        // Act
        List<Job> result = jobRepository.findByStatus(JobStatus.OPEN.getValue(), PageRequest.of(0, 5)).getContent();

        // Assert
        assertThat(result).hasSize(2).extracting(Job::getTitle)
                .containsExactlyInAnyOrder("Open Job", "Another Open");
    }

    @Test
    @DisplayName("countByCompanyId -> returns correct count")
    void countByCompanyId() {
        // Arrange
        Company company = companyRepository.save(activatedCompany("count-co"));
        jobRepository.save(job(company, JobStatus.OPEN.getValue()));
        jobRepository.save(job(company, JobStatus.OPEN.getValue()));

        long count = jobRepository.countByCompanyId(company.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countByCompanyIdAndStatus -> returns correct count")
    void countByCompanyIdAndStatus() {
        // Arrange
        Company company = companyRepository.save(activatedCompany("count-status-co"));
        jobRepository.save(job(company, JobStatus.OPEN.getValue()));
        jobRepository.save(job(company, JobStatus.OPEN.getValue()));
        jobRepository.save(job(company, JobStatus.CLOSED.getValue()));

        long open = jobRepository.countByCompanyIdAndStatus(company.getId(), JobStatus.OPEN.getValue());
        long closed = jobRepository.countByCompanyIdAndStatus(company.getId(), JobStatus.CLOSED.getValue());

        assertThat(open).isEqualTo(2);
        assertThat(closed).isEqualTo(1);
    }

    @Test
    @DisplayName("countJobsByCompanyIdsAndStatus -> returns projection with job counts")
    void countJobsByCompanyIdsAndStatusReturnsProjection() {
        // Arrange
        Company company = companyRepository.save(activatedCompany("proj-co"));
        jobRepository.save(job(company, JobStatus.OPEN.getValue()));
        jobRepository.save(job(company, JobStatus.OPEN.getValue()));

        List<CompanyJobCountProjection> projections =
                jobRepository.countJobsByCompanyIdsAndStatus(List.of(company.getId()), JobStatus.OPEN.getValue());

        assertThat(projections).hasSize(1);
        assertThat(projections.getFirst().getCompanyId()).isEqualTo(company.getId());
        assertThat(projections.getFirst().getJobCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("incrementViewCount -> updates view count using COALESCE")
    void incrementViewCountUpdates() {
        // Arrange
        Company company = companyRepository.save(activatedCompany("view-co"));
        Job job = jobRepository.saveAndFlush(job(company, JobStatus.OPEN.getValue()));

        int affected = jobRepository.incrementViewCount(job.getId(), 3L);
        jobRepository.flush();
        entityManager.clear();

        assertThat(affected).isEqualTo(1);
        assertThat(jobRepository.findById(job.getId()).orElseThrow().getViewCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("findAll -> applies specification filter")
    void findAllAppliesSpecification() {
        // Arrange
        Company company = companyRepository.save(activatedCompany("spec-co"));
        jobRepository.save(job(company, JobStatus.OPEN.getValue(), "Backend Engineer"));
        jobRepository.save(job(company, JobStatus.OPEN.getValue(), "Frontend Engineer"));

        List<Job> result = jobRepository.findAll(
                (root, query, cb) -> cb.like(root.get("title"), "%Backend%"));

        assertThat(result).hasSize(1).extracting(Job::getTitle).containsExactly("Backend Engineer");
    }

    private static Company activatedCompany(String name) {
        return Company.builder().name(name).slug(name + "-" + UUID.randomUUID()).status(CompanyStatus.ACTIVE.getValue()).build();
    }

    private static Job job(Company company, String status) {
        return job(company, status, "Job " + UUID.randomUUID());
    }

    private static Job job(Company company, String status, String title) {
        return Job.builder().company(company).title(title).slug(title.toLowerCase().replace(' ', '-') + "-" + UUID.randomUUID()).status(status).build();
    }
}