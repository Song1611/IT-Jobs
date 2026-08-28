package com.itjob.integration.controller;

import com.itjob.entity.Company;
import com.itjob.enums.CompanyStatus;
import com.itjob.repository.CompanyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("IT - ReviewController")
class ReviewControllerTest extends AbstractControllerTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("GET /api/companies/{companyId}/reviews -> 200 (public)")
    void getCompanyReviewsPublic() throws Exception {
        mockMvc.perform(get("/api/companies/{companyId}/reviews", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    @DisplayName("POST /api/companies/{companyId}/reviews -> 401 without a token")
    void createReviewWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/companies/{companyId}/reviews", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"title\":\"T\",\"comment\":\"C\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/companies/{companyId}/reviews -> 200 for an authenticated user")
    void createReviewReturns200() throws Exception {
        var user = newUser();
        Company company = companyRepository.save(Company.builder()
                .name("Co").slug("co-" + UUID.randomUUID())
                .status(CompanyStatus.ACTIVE.getValue()).build());
        String body = "{\"rating\":5,\"salaryRating\":4,\"title\":\"Great\",\"comment\":\"Loved it\"}";

        mockMvc.perform(post("/api/companies/{companyId}/reviews", company.getId())
                        .header("Authorization", bearer(user, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Review created successfully"))
                .andExpect(jsonPath("$.result.status").value("pending"));
    }
}