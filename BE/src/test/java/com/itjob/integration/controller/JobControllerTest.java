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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("IT - JobController")
class JobControllerTest extends AbstractControllerTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobRepository jobRepository;

    @Test
    @DisplayName("GET /api/jobs/featured -> 200 (public)")
    void getFeaturedJobsPublic() throws Exception {
        mockMvc.perform(get("/api/jobs/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("GET /api/jobs/{id} -> 200 (public)")
    void getJobByIdPublic() throws Exception {
        Job job = jobRepository.save(Job.builder().company(accompany()).title("Public Job").slug("pub-job-" + UUID.randomUUID()).status(JobStatus.OPEN.getValue()).build());

        mockMvc.perform(get("/api/jobs/{id}", job.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.title").value("Public Job"));
    }

    @Test
    @DisplayName("POST /api/jobs/company/{companyId} -> 403 without token (public path hits @PreAuthorize)")
    void createJobWithoutTokenReturns403() throws Exception {
        String body = "{\"title\":\"Job\",\"description\":\"D\",\"workLocation\":\"HCM\"}";
        mockMvc.perform(post("/api/jobs/company/{id}", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/jobs/company/{companyId} -> 200 for EMPLOYER")
    void createJobByEmployerReturns200() throws Exception {
        User employer = newEmployer();
        Company company = companyRepository.save(Company.builder().name("Co").slug("co-" + UUID.randomUUID()).status(CompanyStatus.ACTIVE.getValue()).createdBy(employer).build());

        String body = "{\"title\":\"New Job\",\"description\":\"D\",\"workLocation\":\"HCM\",\"quantity\":1,\"status\":\"open\"}";
        mockMvc.perform(post("/api/jobs/company/{id}", company.getId())
                        .header("Authorization", bearer(employer, "EMPLOYER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.title").value("New Job"));
    }

    @Test
    @DisplayName("POST /api/jobs/company/{companyId} -> 403 for USER with a valid body")
    void createJobByUserReturns403() throws Exception {
        User user = newUser();
        String body = "{\"title\":\"J\",\"description\":\"D\",\"workLocation\":\"HCM\",\"quantity\":1,\"status\":\"open\"}";
        mockMvc.perform(post("/api/jobs/company/{id}", UUID.randomUUID())
                        .header("Authorization", bearer(user, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/jobs/{id}/approve -> 403 for EMPLOYER")
    void approveJobByEmployerReturns403() throws Exception {
        User employer = newEmployer();
        mockMvc.perform(put("/api/jobs/{id}/approve", UUID.randomUUID())
                        .header("Authorization", bearer(employer, "EMPLOYER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/jobs/admin/all -> 200 for ADMIN")
    void getAllJobsByAdminReturns200() throws Exception {
        User admin = newAdmin();
        mockMvc.perform(get("/api/jobs/admin/all")
                        .header("Authorization", bearer(admin, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items").isArray());
    }

    private Company accompany() {
        return companyRepository.save(Company.builder().name("Co").slug("co-" + UUID.randomUUID()).status(CompanyStatus.ACTIVE.getValue()).build());
    }
}