package com.itjob.integration.security;

import com.itjob.integration.controller.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("IT - SecurityConfig")
class SecurityConfigTest extends AbstractControllerTest {

    @Test
    @DisplayName("public endpoints -> accessible without authentication")
    void publicEndpointsAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/jobs/featured")).andExpect(status().isOk());
        mockMvc.perform(get("/api/companies/top")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("protected endpoints -> 401 without a token")
    void protectedEndpointsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/applications/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("authenticated request -> protected endpoint succeeds")
    void authenticatedRequestSucceeds() throws Exception {
        var user = newUser();
        mockMvc.perform(get("/api/applications/me")
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isOk());
    }
}