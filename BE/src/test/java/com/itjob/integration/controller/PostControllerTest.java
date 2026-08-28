package com.itjob.integration.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("IT - PostController")
class PostControllerTest extends AbstractControllerTest {

    @Test
    @DisplayName("GET /api/posts -> 200 (public)")
    void getAllPostsPublic() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    @DisplayName("POST /api/posts -> 401 without a token")
    void createPostWithoutTokenReturns401() throws Exception {
        mockMvc.perform(multipart("/api/posts").param("Content", "Hello"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/posts -> 200 for an authenticated user")
    void createPostReturns200() throws Exception {
        var user = newUser();
        mockMvc.perform(multipart("/api/posts")
                        .param("Content", "My new post")
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Post created successfully"))
                .andExpect(jsonPath("$.result.content").value("My new post"));
    }

    @Test
    @DisplayName("DELETE /api/posts/{id} -> 200 for the owner")
    void deletePostReturns200() throws Exception {
        var user = newUser();
        var created = mockMvc.perform(multipart("/api/posts")
                        .param("Content", "To delete")
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String postId = objectMapper.readTree(created).path("result").path("id").asText();

        mockMvc.perform(delete("/api/posts/{id}", postId)
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Post deleted successfully"));
    }
}