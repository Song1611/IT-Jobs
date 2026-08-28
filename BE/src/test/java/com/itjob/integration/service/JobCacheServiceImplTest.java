package com.itjob.integration.service;

import com.itjob.entity.Company;
import com.itjob.entity.Job;
import com.itjob.entity.User;
import com.itjob.enums.CompanyStatus;
import com.itjob.enums.JobStatus;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.JobRepository;
import com.itjob.service.JobCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("IT - JobCacheService")
class JobCacheServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private JobCacheService jobCacheService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("getCachedJobById -> returns the cached job")
    void getCachedJobByIdReturnsJob() {
        Company company = companyRepository.save(Company.builder()
                .name("Co").slug("co-" + UUID.randomUUID()).status(CompanyStatus.ACTIVE.getValue()).build());
        Job job = jobRepository.save(Job.builder().company(company).title("Job").slug("j-" + UUID.randomUUID()).status(JobStatus.OPEN.getValue()).build());

        var response = jobCacheService.getCachedJobById(job.getId());

        assertThat(response.getTitle()).isEqualTo("Job");
    }

    @Test
    @DisplayName("getCachedJobById -> throws JOB_NOT_FOUND for a missing job")
    void getCachedJobByIdNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> jobCacheService.getCachedJobById(randomId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.JOB_NOT_FOUND);
    }
}