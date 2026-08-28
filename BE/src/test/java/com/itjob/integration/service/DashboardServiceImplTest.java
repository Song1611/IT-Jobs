package com.itjob.integration.service;

import com.itjob.entity.Company;
import com.itjob.entity.Job;
import com.itjob.entity.User;
import com.itjob.enums.CompanyStatus;
import com.itjob.enums.JobStatus;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.JobRepository;
import com.itjob.service.DashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("IT - DashboardService")
class DashboardServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobRepository jobRepository;

    @Test
    @DisplayName("getHRDashboardStats -> counts active jobs and applications for a company")
    void hrDashboardCountsJobs() {
        User employer = newEmployer();
        Company company = companyRepository.save(Company.builder()
                .name("HR Co").slug("hr-" + UUID.randomUUID()).status(CompanyStatus.ACTIVE.getValue()).createdBy(employer).build());
        jobRepository.save(Job.builder().company(company).title("Job A").slug("ja-" + UUID.randomUUID()).status(JobStatus.OPEN.getValue()).build());
        jobRepository.save(Job.builder().company(company).title("Job B").slug("jb-" + UUID.randomUUID()).status(JobStatus.OPEN.getValue()).build());

        var stats = dashboardService.getHRDashboardStats(company.getId());

        assertThat(stats.getTotalActiveJobs()).isEqualTo(2L);
        assertThat(stats.getTotalApplications()).isZero();
    }

    @Test
    @DisplayName("getAdminDashboardStats -> counts companies and jobs system-wide")
    void adminDashboardCountsData() {
        User employer = newEmployer();
        Company company = companyRepository.save(Company.builder()
                .name("Admin Co").slug("admin-" + UUID.randomUUID()).status(CompanyStatus.ACTIVE.getValue()).createdBy(employer).build());
        jobRepository.save(Job.builder().company(company).title("Job").slug("j-" + UUID.randomUUID()).status(JobStatus.OPEN.getValue()).build());

        var stats = dashboardService.getAdminDashboardStats();

        assertThat(stats.getTotalCompanies()).isPositive();
        assertThat(stats.getTotalJobs()).isPositive();
        assertThat(stats.getTotalUsers()).isPositive();
    }

    private User newEmployer() {
        return userRepository.save(User.builder()
                .fullName("Employer")
                .email("emp-" + UUID.randomUUID() + "@example.com")
                .password(passwordEncoder.encode("password123"))
                .enabled(true)
                .build());
    }
}