package com.itjob.integration.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("IT - AdminController")
class AdminControllerTest extends AbstractControllerTest {

    @Test
    @DisplayName("GET /api/admin/users -> 401 without token")
    void getAllUsersWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/admin/users -> 403 for USER")
    void getAllUsersByUserReturns403() throws Exception {
        var user = newUser();
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/users -> 200 for ADMIN")
    void getAllUsersByAdminReturns200() throws Exception {
        var admin = newAdmin();
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(admin, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    @DisplayName("GET /api/admin/companies -> 200 for ADMIN")
    void getAllCompaniesByAdminReturns200() throws Exception {
        var admin = newAdmin();
        mockMvc.perform(get("/api/admin/companies")
                        .header("Authorization", bearer(admin, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    @DisplayName("GET /api/admin/jobs -> 200 for ADMIN")
    void getAllJobsByAdminReturns200() throws Exception {
        var admin = newAdmin();
        mockMvc.perform(get("/api/admin/jobs")
                        .header("Authorization", bearer(admin, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items").isArray());
    }
}