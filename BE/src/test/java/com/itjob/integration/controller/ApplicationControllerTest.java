package com.itjob.integration.controller;

import com.itjob.entity.Company;
import com.itjob.entity.Job;
import com.itjob.entity.User;
import com.itjob.enums.CompanyStatus;
import com.itjob.enums.JobStatus;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.JobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("IT - ApplicationController")
class ApplicationControllerTest extends AbstractControllerTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobRepository jobRepository;

    @Test
    @DisplayName("GET /api/applications/me -> 401 without token")
    void getMyApplicationsWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/applications/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/applications/me -> 200 for USER")
    void getMyApplicationsByUserReturns200() throws Exception {
        User user = newUser();
        mockMvc.perform(get("/api/applications/me")
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    @DisplayName("POST /api/applications -> 200 for USER")
    void applyForJobByUserReturns200() throws Exception {
        User employer = newEmployer();
        Company company = companyRepository.save(Company.builder().name("Co").slug("co-" + UUID.randomUUID()).status(CompanyStatus.ACTIVE.getValue()).createdBy(employer).build());
        Job job = jobRepository.save(Job.builder().company(company).title("Open Job").slug("open-" + UUID.randomUUID()).status(JobStatus.OPEN.getValue()).build());
        User candidate = newUser();

        String body = "{\"jobId\":\"" + job.getId() + "\"}";
        mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(candidate, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("pending"));
    }

    @Test
    @DisplayName("DELETE /api/applications/{id} -> 200 for the applicant")
    void withdrawApplicationByUserReturns200() throws Exception {
        User employer = newEmployer();
        Company company = companyRepository.save(Company.builder().name("Co").slug("co-" + UUID.randomUUID()).status(CompanyStatus.ACTIVE.getValue()).createdBy(employer).build());
        Job job = jobRepository.save(Job.builder().company(company).title("Withdraw Job").slug("wd-" + UUID.randomUUID()).status(JobStatus.OPEN.getValue()).build());
        User candidate = newUser();

        String applyBody = "{\"jobId\":\"" + job.getId() + "\"}";
        String createResult = mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer(candidate, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        String appId = objectMapper.readTree(createResult).path("result").path("id").asText();
        mockMvc.perform(delete("/api/applications/{id}", appId)
                        .header("Authorization", bearer(candidate, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Application withdrawn successfully"));
    }
}