package com.itjob.integration.repository;

import com.itjob.config.AbstractPostgresIntegrationTest;
import com.itjob.entity.Company;
import com.itjob.entity.User;
import com.itjob.enums.CompanyStatus;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("IT - CompanyRepository")
class CompanyRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("save -> assigns defaults (status PENDING, isDeleted false, viewCount 0)")
    void saveAssignsDefaults() {
        Company saved = companyRepository.save(company("defaults-co"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(CompanyStatus.PENDING.getValue());
        assertThat(saved.getIsDeleted()).isFalse();
        assertThat(saved.getViewCount()).isZero();
        assertThat(saved.getFollowerCount()).isZero();
    }

    @Test
    @DisplayName("findTopCompanies -> returns non-deleted active companies ordered by follower count desc")
    void findTopCompaniesReturnsOrdered() {
        companyRepository.save(activatedCompany("Popular Co", 100, 50));
        companyRepository.save(activatedCompany("Less Popular Co", 10, 5));
        companyRepository.save(deletedCompany("Deleted Co"));

        var result = companyRepository.findTopCompanies(CompanyStatus.ACTIVE.getValue(), PageRequest.of(0, 10));

        assertThat(result).extracting(Company::getName).containsExactly("Popular Co", "Less Popular Co");
    }

    @Test
    @DisplayName("findActiveCompanies -> returns active non-deleted companies")
    void findActiveCompaniesReturnsActive() {
        companyRepository.save(activatedCompany("Active Co"));
        companyRepository.save(company("pending-co")); // default status PENDING

        var result = companyRepository.findActiveCompanies(CompanyStatus.ACTIVE.getValue(), PageRequest.of(0, 10));

        assertThat(result).extracting(Company::getName).containsExactly("Active Co");
    }

    @Test
    @DisplayName("findByStatusAndIsDeleted -> paginated")
    void findByStatusAndIsDeletedPaginated() {
        companyRepository.save(activatedCompany("Active Co 1"));
        companyRepository.save(activatedCompany("Active Co 2"));
        companyRepository.save(company("pending-co"));

        var result = companyRepository.findByStatusAndIsDeleted(CompanyStatus.ACTIVE.getValue(), false, PageRequest.of(0, 5));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByIsDeleted -> paginated")
    void findByIsDeletedPaginated() {
        companyRepository.save(activatedCompany("Not Deleted"));
        companyRepository.save(deletedCompany("Deleted Co"));

        var result = companyRepository.findByIsDeleted(false, PageRequest.of(0, 5));

        assertThat(result).extracting(Company::getName).containsExactly("Not Deleted");
    }

    @Test
    @DisplayName("findBySlugAndIsDeleted -> returns company when slug matches and not deleted")
    void findBySlugAndIsDeletedReturnsCompany() {
        companyRepository.save(Company.builder().name("Slug Co").slug("slug-co").status(CompanyStatus.ACTIVE.getValue()).build());

        Optional<Company> result = companyRepository.findBySlugAndIsDeleted("slug-co", false);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Slug Co");
    }

    @Test
    @DisplayName("findBySlug -> returns company regardless of isDeleted")
    void findBySlugReturnsCompany() {
        companyRepository.save(Company.builder().name("Deleted Co").slug("deleted-slug").status(CompanyStatus.ACTIVE.getValue()).isDeleted(true).build());
        // deleted company still findable by slug alone

        Optional<Company> result = companyRepository.findBySlug("deleted-slug");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Deleted Co");
    }

    @Test
    @DisplayName("countByStatus -> counts companies by status")
    void countByStatus() {
        companyRepository.save(activatedCompany("Active 1"));
        companyRepository.save(activatedCompany("Active 2"));
        companyRepository.save(company("pending-co"));

        assertThat(companyRepository.countByStatus(CompanyStatus.ACTIVE.getValue())).isEqualTo(2);
        assertThat(companyRepository.countByStatus(CompanyStatus.PENDING.getValue())).isEqualTo(1);
    }

    @Test
    @DisplayName("countByStatusAndIsDeleted -> counts active non-deleted companies")
    void countByStatusAndIsDeleted() {
        companyRepository.save(activatedCompany("Active 1"));
        companyRepository.save(activatedCompany("Active 2"));
        companyRepository.save(deletedCompany("Deleted Active"));

        long count = companyRepository.countByStatusAndIsDeleted(CompanyStatus.ACTIVE.getValue(), false);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("existsByCreatedByIdAndIsDeleted -> returns true when user has non-deleted company")
    void existsByCreatedByIdAndIsDeleted() {
        User user = userRepository.save(User.builder().fullName("Employer").email("emp-" + UUID.randomUUID() + "@example.com").password("hashed").build());
        Company company = activatedCompany("User Co");
        company.setCreatedBy(user);
        companyRepository.save(company);

        boolean exists = companyRepository.existsByCreatedByIdAndIsDeleted(user.getId(), false);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("findByIdAndIsDeleted -> returns non-deleted company")
    void findByIdAndIsDeleted() {
        Company company = companyRepository.save(activatedCompany("Findable Co"));

        Optional<Company> result = companyRepository.findByIdAndIsDeleted(company.getId(), false);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Findable Co");
    }

    @Test
    @DisplayName("incrementViewCount -> updates view count using COALESCE")
    void incrementViewCountUpdates() {
        Company company = companyRepository.saveAndFlush(activatedCompany("View Co"));

        int affected = companyRepository.incrementViewCount(company.getId(), 5L);
        companyRepository.flush();
        entityManager.clear();

        assertThat(affected).isEqualTo(1);
        assertThat(companyRepository.findById(company.getId()).orElseThrow().getViewCount()).isEqualTo(5);
    }

    private static Company company(String name) {
        return Company.builder().name(name).slug(name + "-" + UUID.randomUUID()).build();
    }

    private static Company activatedCompany(String name) {
        return Company.builder().name(name).slug(name + "-" + UUID.randomUUID()).status(CompanyStatus.ACTIVE.getValue()).build();
    }

    private static Company activatedCompany(String name, int followerCount, int viewCount) {
        return Company.builder().name(name).slug(name + "-" + UUID.randomUUID()).status(CompanyStatus.ACTIVE.getValue()).followerCount(followerCount).viewCount((long) viewCount).build();
    }

    private static Company deletedCompany(String name) {
        return Company.builder().name(name).slug(name + "-" + UUID.randomUUID()).status(CompanyStatus.ACTIVE.getValue()).isDeleted(true).build();
    }
}