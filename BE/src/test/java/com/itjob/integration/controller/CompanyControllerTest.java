package com.itjob.integration.controller;

import com.itjob.entity.Company;
import com.itjob.entity.User;
import com.itjob.enums.CompanyStatus;
import com.itjob.repository.CompanyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("IT - CompanyController")
class CompanyControllerTest extends AbstractControllerTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("GET /api/companies/top -> 200 (public)")
    void getTopCompaniesPublic() throws Exception {
        mockMvc.perform(get("/api/companies/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("GET /api/companies/{id} -> 200 (public)")
    void getCompanyByIdPublic() throws Exception {
        Company company = companyRepository.save(Company.builder().name("Public Co").slug("pc-" + UUID.randomUUID()).status(CompanyStatus.ACTIVE.getValue()).build());

        mockMvc.perform(get("/api/companies/{id}", company.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Public Co"));
    }

    @Test
    @DisplayName("GET /api/companies/me -> 403 without token (public path hits @PreAuthorize)")
    void getMyCompanyWithoutTokenReturns403() throws Exception {
        mockMvc.perform(get("/api/companies/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/companies/me -> 200 for EMPLOYER")
    void getMyCompanyByEmployerReturns200() throws Exception {
        User employer = newEmployer();
        companyRepository.save(Company.builder().name("My Co").slug("my-" + UUID.randomUUID()).status(CompanyStatus.PENDING.getValue()).createdBy(employer).build());

        mockMvc.perform(get("/api/companies/me")
                        .header("Authorization", bearer(employer, "EMPLOYER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("My Co"));
    }

    @Test
    @DisplayName("POST /api/companies -> 200 for EMPLOYER")
    void createCompanyByEmployerReturns200() throws Exception {
        User employer = newEmployer();
        String body = "{\"name\":\"New Company\"}";
        mockMvc.perform(post("/api/companies")
                        .header("Authorization", bearer(employer, "EMPLOYER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("New Company"));
    }

    @Test
    @DisplayName("POST /api/companies -> 403 for USER")
    void createCompanyByUserReturns403() throws Exception {
        User user = newUser();
        mockMvc.perform(post("/api/companies")
                        .header("Authorization", bearer(user, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"N\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/companies/{id}/approve -> 200 for ADMIN")
    void approveCompanyByAdminReturns200() throws Exception {
        User employer = newEmployer();
        Company company = companyRepository.save(Company.builder().name("Approve Co").slug("ac-" + UUID.randomUUID()).status(CompanyStatus.PENDING.getValue()).createdBy(employer).build());
        User admin = newAdmin();

        mockMvc.perform(put("/api/companies/{id}/approve", company.getId())
                        .header("Authorization", bearer(admin, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Company approved successfully"));
    }

    @Test
    @DisplayName("PUT /api/companies/{id}/approve -> 403 for USER")
    void approveCompanyByUserReturns403() throws Exception {
        User user = newUser();
        mockMvc.perform(put("/api/companies/{id}/approve", UUID.randomUUID())
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isForbidden());
    }
}