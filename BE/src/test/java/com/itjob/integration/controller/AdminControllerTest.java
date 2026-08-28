package com.itjob.integration.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @ValueSource(strings = {"/api/admin/users", "/api/admin/companies", "/api/admin/jobs"})
    @DisplayName("GET /api/admin/{resource} -> 200 for ADMIN")
    void adminListEndpointReturns200ForAdmin(String path) throws Exception {
        var admin = newAdmin();
        mockMvc.perform(get(path)
                        .header("Authorization", bearer(admin, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items").isArray());
    }
}