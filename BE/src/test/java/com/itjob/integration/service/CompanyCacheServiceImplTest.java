package com.itjob.integration.service;

import com.itjob.entity.Company;
import com.itjob.enums.CompanyStatus;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.CompanyRepository;
import com.itjob.service.CompanyCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("IT - CompanyCacheService")
class CompanyCacheServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private CompanyCacheService companyCacheService;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("getCachedCompanyById -> returns the cached company")
    void getCachedCompanyByIdReturnsCompany() {
        Company company = companyRepository.save(Company.builder()
                .name("Cached Co").slug("cc-" + UUID.randomUUID()).status(CompanyStatus.ACTIVE.getValue()).build());

        var response = companyCacheService.getCachedCompanyById(company.getId());

        assertThat(response.getName()).isEqualTo("Cached Co");
    }

    @Test
    @DisplayName("getCachedCompanyById -> throws COMPANY_NOT_FOUND for a missing company")
    void getCachedCompanyByIdNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> companyCacheService.getCachedCompanyById(randomId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);
    }

    @Test
    @DisplayName("getCachedCompanyBySlug -> returns the cached company")
    void getCachedCompanyBySlugReturnsCompany() {
        Company company = companyRepository.save(Company.builder()
                .name("Slug Co").slug("slug-co-" + UUID.randomUUID()).status(CompanyStatus.ACTIVE.getValue()).build());

        var response = companyCacheService.getCachedCompanyBySlug(company.getSlug());

        assertThat(response.getName()).isEqualTo("Slug Co");
    }

    @Test
    @DisplayName("getCachedCompanyBySlug -> throws COMPANY_NOT_FOUND for a missing slug")
    void getCachedCompanyBySlugNotFoundThrows() {
        assertThatThrownBy(() -> companyCacheService.getCachedCompanyBySlug("missing-slug"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);
    }
}