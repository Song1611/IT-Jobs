package com.itjob.integration.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("IT - SkillController")
class SkillControllerTest extends AbstractControllerTest {

    @Test
    @DisplayName("GET /api/skills -> 200 (public)")
    void getAllSkillsPublic() throws Exception {
        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("POST /api/skills -> 403 for USER")
    void createSkillByUserReturns403() throws Exception {
        var user = newUser();
        mockMvc.perform(post("/api/skills")
                        .header("Authorization", bearer(user, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/skills -> 200 for ADMIN")
    void createSkillByAdminReturns200() throws Exception {
        var admin = newAdmin();
        mockMvc.perform(post("/api/skills")
                        .header("Authorization", bearer(admin, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Go-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Skill created successfully"));
    }

    @Test
    @DisplayName("POST /api/skills -> 400 for a blank name")
    void createSkillBlankNameReturns400() throws Exception {
        var admin = newAdmin();
        mockMvc.perform(post("/api/skills")
                        .header("Authorization", bearer(admin, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/skills/{id} -> 200 for ADMIN")
    void updateSkillByAdminReturns200() throws Exception {
        var admin = newAdmin();
        var created = mockMvc.perform(post("/api/skills")
                        .header("Authorization", bearer(admin, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Old-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String skillId = objectMapper.readTree(created).path("result").path("id").asText();

        mockMvc.perform(put("/api/skills/{id}", skillId)
                        .header("Authorization", bearer(admin, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Skill updated successfully"));
    }

    @Test
    @DisplayName("DELETE /api/skills/{id} -> 200 for ADMIN")
    void deleteSkillByAdminReturns200() throws Exception {
        var admin = newAdmin();
        var created = mockMvc.perform(post("/api/skills")
                        .header("Authorization", bearer(admin, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ToDelete-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String skillId = objectMapper.readTree(created).path("result").path("id").asText();

        mockMvc.perform(delete("/api/skills/{id}", skillId)
                        .header("Authorization", bearer(admin, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Skill deleted successfully"));
    }
}