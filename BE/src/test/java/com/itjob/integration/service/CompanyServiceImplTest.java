package com.itjob.integration.service;

import com.itjob.dto.request.CompanyRequest;
import com.itjob.dto.response.CompanyResponse;
import com.itjob.entity.User;
import com.itjob.enums.CompanyStatus;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.service.CompanyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("IT - CompanyService")
class CompanyServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private CompanyService companyService;

    @Test
    @DisplayName("createCompany -> creates a PENDING company for the employer")
    void createCompanyCreatesPending() {
        User user = createVerifiedUser("emp-" + UUID.randomUUID() + "@example.com");
        authenticateAs(user.getId(), user.getEmail(), "EMPLOYER");

        var response = companyService.createCompany(companyRequest("Tech Corp"), user.getId());

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(CompanyStatus.PENDING.getValue());
        assertThat(response.getJobCount()).isZero();
    }

    @Test
    @DisplayName("createCompany -> throws COMPANY_ALREADY_EXISTS when user already has a company")
    void createCompanyAlreadyExistsThrows() {
        User user = createVerifiedUser("emp-" + UUID.randomUUID() + "@example.com");
        authenticateAs(user.getId(), user.getEmail(), "EMPLOYER");
        companyService.createCompany(companyRequest("First Corp"), user.getId());

        CompanyRequest second = companyRequest("Second Corp");
        UUID userId = user.getId();
        assertThatThrownBy(() -> companyService.createCompany(second, userId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("getMyCompany -> returns the employer's pending company")
    void getMyCompanyReturnsCompany() {
        User user = createVerifiedUser("emp-" + UUID.randomUUID() + "@example.com");
        authenticateAs(user.getId(), user.getEmail(), "EMPLOYER");
        companyService.createCompany(companyRequest("My Company"), user.getId());

        var response = companyService.getMyCompany(user.getId());

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("My Company");
        assertThat(response.getStatus()).isEqualTo(CompanyStatus.PENDING.getValue());
    }

    @Test
    @DisplayName("getMyCompany -> throws COMPANY_NOT_FOUND when user has no company")
    void getMyCompanyNotFoundThrows() {
        User user = createVerifiedUser("emp-" + UUID.randomUUID() + "@example.com");

        UUID userId = user.getId();
        assertThatThrownBy(() -> companyService.getMyCompany(userId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);
    }

    @Test
    @DisplayName("approveCompany -> employer creates, admin approves to ACTIVE")
    void approveCompanyActivates() {
        User employer = createVerifiedUser("emp-" + UUID.randomUUID() + "@example.com");
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var created = companyService.createCompany(companyRequest("Approve Co"), employer.getId());

        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");
        var approved = companyService.approveCompany(created.getId(), admin.getId());

        assertThat(approved.getStatus()).isEqualTo(CompanyStatus.ACTIVE.getValue());
    }

    @Test
    @DisplayName("approveCompany -> throws COMPANY_ALREADY_PROCESSED if already approved")
    void approveCompanyAlreadyProcessedThrows() {
        User employer = createVerifiedUser("emp-" + UUID.randomUUID() + "@example.com");
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var created = companyService.createCompany(companyRequest("Already Co"), employer.getId());

        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");
        companyService.approveCompany(created.getId(), admin.getId());

        UUID createdId = created.getId();
        UUID adminId = admin.getId();
        assertThatThrownBy(() -> companyService.approveCompany(createdId, adminId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_ALREADY_PROCESSED);
    }

    @Test
    @DisplayName("approveCompany -> throws COMPANY_NOT_FOUND for a non-existent company")
    void approveCompanyNotFoundThrows() {
        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");

        UUID randomId = UUID.randomUUID();
        UUID adminId = admin.getId();
        assertThatThrownBy(() -> companyService.approveCompany(randomId, adminId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMPANY_NOT_FOUND);
    }

    @Test
    @DisplayName("getTopCompanies -> returns the approved company among active companies only")
    void getTopCompaniesReturnsApproved() {
        User employer = createVerifiedUser("emp-" + UUID.randomUUID() + "@example.com");
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var created = companyService.createCompany(companyRequest("Top Co"), employer.getId());

        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");
        var approved = companyService.approveCompany(created.getId(), admin.getId());
        assertThat(approved.getStatus()).isEqualTo(CompanyStatus.ACTIVE.getValue());

        var top = companyService.getTopCompanies(100);

        assertThat(top)
                .extracting(com.itjob.dto.response.CompanyResponse::getId)
                .contains(created.getId());
        assertThat(top).allSatisfy(company ->
                assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTIVE.getValue()));

        var persisted = companyService.getCompanyById(created.getId());
        assertThat(persisted.getStatus()).isEqualTo(CompanyStatus.ACTIVE.getValue());
    }

    @Test
    @DisplayName("getActiveCompanies -> returns only active companies")
    void getActiveCompaniesReturnsOnlyActive() {
        User employer = createVerifiedUser("emp-" + UUID.randomUUID() + "@example.com");
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var created = companyService.createCompany(companyRequest("Active Only Co"), employer.getId());
        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");
        companyService.approveCompany(created.getId(), admin.getId());

        var page = companyService.getActiveCompanies(PageRequest.of(0, 100));

        assertThat(page.getItems())
                .extracting(CompanyResponse::getId)
                .contains(created.getId());
        assertThat(page.getItems()).allSatisfy(company ->
                assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTIVE.getValue()));
    }

    @Test
    @DisplayName("getCompanyBySlug -> returns the company by its slug")
    void getCompanyBySlugReturnsCompany() {
        User employer = createVerifiedUser("emp-" + UUID.randomUUID() + "@example.com");
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var created = companyService.createCompany(companyRequest("Slug Lookup Co"), employer.getId());
        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");
        companyService.approveCompany(created.getId(), admin.getId());

        String slug = created.getSlug();
        CompanyResponse found = companyService.getCompanyBySlug(slug);

        assertThat(found.getId()).isEqualTo(created.getId());
    }

    @Test
    @DisplayName("updateCompany -> employer updates the company name")
    void updateCompanyUpdatesName() {
        User employer = createVerifiedUser("emp-" + UUID.randomUUID() + "@example.com");
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var created = companyService.createCompany(companyRequest("Old Name Co"), employer.getId());

        CompanyResponse updated = companyService.updateCompany(
                created.getId(), companyRequest("New Name Co"), employer.getId());

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getName()).isEqualTo("New Name Co");
    }

    @Test
    @DisplayName("getAllCompanies -> admin can list all companies")
    void getAllCompaniesReturnsAll() {
        User employer = createVerifiedUser("emp-" + UUID.randomUUID() + "@example.com");
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        companyService.createCompany(companyRequest("Admin View Co"), employer.getId());

        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");

        var page = companyService.getAllCompanies(null, PageRequest.of(0, 100));

        assertThat(page.getItems()).extracting("name").contains("Admin View Co");
    }

    @Test
    @DisplayName("rejectCompany -> admin rejects a pending company")
    void rejectCompanyRejectsPending() {
        User employer = createVerifiedUser("emp-" + UUID.randomUUID() + "@example.com");
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var created = companyService.createCompany(companyRequest("Reject Co"), employer.getId());

        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");
        CompanyResponse rejected = companyService.rejectCompany(created.getId(), admin.getId(), "invalid docs");

        assertThat(rejected.getStatus()).isEqualTo(CompanyStatus.REJECTED.getValue());
    }

    @Test
    @DisplayName("suspendCompany -> admin suspends an active company")
    void suspendCompanySuspendsActive() {
        User employer = createVerifiedUser("emp-" + UUID.randomUUID() + "@example.com");
        authenticateAs(employer.getId(), employer.getEmail(), "EMPLOYER");
        var created = companyService.createCompany(companyRequest("Suspend Co"), employer.getId());
        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");
        companyService.approveCompany(created.getId(), admin.getId());

        CompanyResponse suspended = companyService.suspendCompany(created.getId(), admin.getId(), "violation");

        assertThat(suspended.getStatus()).isEqualTo(CompanyStatus.SUSPENDED.getValue());
    }
}