package com.itjob.fixture;

import com.itjob.entity.Company;
import com.itjob.entity.Job;
import com.itjob.entity.User;
import com.itjob.enums.CompanyStatus;
import com.itjob.enums.JobStatus;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.JobRepository;
import com.itjob.repository.UserRepository;

import java.util.UUID;

/**
 * Creates and persists valid test entities with minimal defaults.
 * Overloads allow tests to customize commonly relevant fields.
 */
public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static User user(UserRepository repo, String email) {
        return user(repo, email, "Candidate");
    }

    public static User user(UserRepository repo, String email, String fullName) {
        return repo.save(User.builder()
                .fullName(fullName)
                .email(email)
                .password("hashed")
                .build());
    }

    public static Company activeCompany(CompanyRepository repo, String name) {
        return company(repo, name, CompanyStatus.ACTIVE);
    }

    public static Company company(CompanyRepository repo, String name, CompanyStatus status) {
        return repo.save(Company.builder()
                .name(name)
                .slug(slug(name))
                .status(status.getValue())
                .build());
    }

    public static Job job(JobRepository repo, Company company, String title) {
        return job(repo, company, title, JobStatus.OPEN);
    }

    public static Job job(JobRepository repo, Company company, String title, JobStatus status) {
        return repo.save(Job.builder()
                .company(company)
                .title(title)
                .slug(slug(title))
                .status(status.getValue())
                .build());
    }

    private static String slug(String value) {
        return value.toLowerCase().replace(' ', '-') + "-" + UUID.randomUUID();
    }
}
