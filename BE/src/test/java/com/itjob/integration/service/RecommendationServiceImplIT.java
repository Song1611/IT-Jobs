package com.itjob.integration.service;

import com.itjob.dto.request.JobRequest;
import com.itjob.entity.Skill;
import com.itjob.entity.User;
import com.itjob.repository.JobRepository;
import com.itjob.repository.SkillRepository;
import com.itjob.service.RecommendationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("IT - RecommendationService")
class RecommendationServiceImplIT extends AbstractServiceIntegrationTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private JobRepository jobRepository;

    // limit=1 keeps the exclusion deterministic: the matching job (highest score) is
    // always the single recommendation, while a non-matching job (score 0) can never
    // be returned. With a larger limit, zero-score ties would make the outcome depend
    // on how many other jobs exist in the shared database.
    @Test
    @Transactional
    @DisplayName("getRecommendedJobs -> recommends the matching job and excludes non-matching jobs")
    void recommendsMatchingJobsAndExcludesNonMatching() {
        // Arrange
        Skill java = skillRepository.save(Skill.builder().name("Java").build());
        Skill python = skillRepository.save(Skill.builder().name("Python").build());

        User candidate = createVerifiedUser("candidate-" + UUID.randomUUID() + "@example.com");
        candidate.setSkills(new HashSet<>(Set.of(java)));
        userRepository.save(candidate);

        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");

        JobRequest javaRequest = jobRequest("Java Developer");
        javaRequest.setSkillIds(Set.of(java.getId()));
        UUID javaJobId = jobService.createJob(companyId, javaRequest, employer.getId()).getId();

        JobRequest pythonRequest = jobRequest("Python Developer");
        pythonRequest.setSkillIds(Set.of(python.getId()));
        UUID pythonJobId = jobService.createJob(companyId, pythonRequest, employer.getId()).getId();

        // Act
        List<UUID> recommended = recommendationService.getRecommendedJobs(candidate.getId(), 1);

        // Assert
        assertThat(recommended)
                .contains(javaJobId)
                .doesNotContain(pythonJobId)
                .hasSizeLessThanOrEqualTo(1);
    }

    @Test
    @Transactional
    @DisplayName("getRecommendedJobs -> serves the cached result on repeat calls")
    void servesCachedResultsOnRepeatCalls() {
        // Arrange
        Skill java = skillRepository.save(Skill.builder().name("Java").build());
        User candidate = createVerifiedUser("candidate-" + UUID.randomUUID() + "@example.com");
        candidate.setSkills(new HashSet<>(Set.of(java)));
        userRepository.save(candidate);

        User employer = employerWithActiveCompany();
        UUID companyId = activeCompanyId(employer);
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        JobRequest request = jobRequest("Cached Job");
        request.setSkillIds(Set.of(java.getId()));
        UUID jobId = jobService.createJob(companyId, request, employer.getId()).getId();

        // Act
        List<UUID> first = recommendationService.getRecommendedJobs(candidate.getId(), 1);
        assertThat(first).contains(jobId);

        // Delete the job from DB — the cached recommendation must still be served
        jobRepository.deleteById(jobId);
        List<UUID> second = recommendationService.getRecommendedJobs(candidate.getId(), 1);

        // Assert
        assertThat(second).contains(jobId);
    }
}