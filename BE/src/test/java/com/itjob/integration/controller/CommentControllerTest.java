package com.itjob.integration.controller;

import com.itjob.entity.Post;
import com.itjob.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("IT - CommentController")
class CommentControllerTest extends AbstractControllerTest {

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("GET /api/posts/{postId}/comments -> 200 (public)")
    void getCommentsPublic() throws Exception {
        var user = newUser();
        Post post = postRepository.save(Post.builder().author(user).content("Post").build());

        mockMvc.perform(get("/api/posts/{postId}/comments", post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    @DisplayName("POST /api/posts/{postId}/comment -> 401 without a token")
    void addCommentWithoutTokenReturns401() throws Exception {
        var user = newUser();
        Post post = postRepository.save(Post.builder().author(user).content("Post").build());

        mockMvc.perform(multipart("/api/posts/{postId}/comment", post.getId())
                        .param("Content", "Nice"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/posts/{postId}/comment -> 200 for an authenticated user")
    void addCommentReturns200() throws Exception {
        var user = newUser();
        Post post = postRepository.save(Post.builder().author(user).content("Post").build());

        mockMvc.perform(multipart("/api/posts/{postId}/comment", post.getId())
                        .param("Content", "Nice post!")
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comment added successfully"))
                .andExpect(jsonPath("$.result.content").value("Nice post!"));
    }

    @Test
    @DisplayName("DELETE /api/posts/{postId}/comment/{commentId} -> 200 for the comment author")
    void deleteCommentReturns200() throws Exception {
        var user = newUser();
        Post post = postRepository.save(Post.builder().author(user).content("Post").build());
        var created = mockMvc.perform(multipart("/api/posts/{postId}/comment", post.getId())
                        .param("Content", "Remove me")
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String commentId = objectMapper.readTree(created).path("result").path("id").asText();

        mockMvc.perform(delete("/api/posts/{postId}/comment/{commentId}", post.getId(), commentId)
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comment deleted successfully"));
    }
}