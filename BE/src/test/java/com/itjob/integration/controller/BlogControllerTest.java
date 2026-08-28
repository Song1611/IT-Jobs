package com.itjob.integration.controller;

import com.itjob.entity.BlogCategory;
import com.itjob.repository.BlogCategoryRepository;
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

@DisplayName("IT - BlogController")
class BlogControllerTest extends AbstractControllerTest {

    @Autowired
    private BlogCategoryRepository categoryRepository;

    @Test
    @DisplayName("GET /api/blogs/recent -> 200 (public)")
    void getRecentBlogsPublic() throws Exception {
        mockMvc.perform(get("/api/blogs/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("GET /api/blogs -> 200 (public)")
    void getAllBlogsPublic() throws Exception {
        mockMvc.perform(get("/api/blogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    @DisplayName("POST /api/blogs -> 400 for an invalid body (missing category)")
    void createBlogInvalidBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/blogs")
                        .header("Authorization", bearer(newUser(), "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"content\":\"C\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/blogs -> 200 for an authenticated user")
    void createBlogReturns200() throws Exception {
        var user = newUser();
        BlogCategory category = categoryRepository.save(
                BlogCategory.builder().name("Cat-" + UUID.randomUUID()).build());
        String body = "{\"categoryId\":\"" + category.getId() + "\",\"title\":\"My Blog\",\"content\":\"Body\"}";

        mockMvc.perform(post("/api/blogs")
                        .header("Authorization", bearer(user, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Blog created successfully"))
                .andExpect(jsonPath("$.result.title").value("My Blog"));
    }

    @Test
    @DisplayName("DELETE /api/blogs/{id} -> 200 for the owner")
    void deleteBlogReturns200() throws Exception {
        var user = newUser();
        BlogCategory category = categoryRepository.save(
                BlogCategory.builder().name("Cat-" + UUID.randomUUID()).build());
        String body = "{\"categoryId\":\"" + category.getId() + "\",\"title\":\"Delete Me\",\"content\":\"Body\"}";
        var created = mockMvc.perform(post("/api/blogs")
                        .header("Authorization", bearer(user, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String blogId = objectMapper.readTree(created).path("result").path("id").asText();

        mockMvc.perform(delete("/api/blogs/{id}", blogId)
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Blog deleted successfully"));
    }
}