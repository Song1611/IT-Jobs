package com.itjob.integration.controller;

import com.itjob.entity.Post;
import com.itjob.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("IT - ReactionController")
class ReactionControllerTest extends AbstractControllerTest {

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("POST /api/reactions/posts/{postId} -> 401 without a token")
    void toggleReactionWithoutTokenReturns401() throws Exception {
        var user = newUser();
        Post post = postRepository.save(Post.builder().author(user).content("Post").build());

        mockMvc.perform(post("/api/reactions/posts/{postId}", post.getId())
                        .param("type", "like"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/reactions/posts/{postId} -> 200 for an authenticated user")
    void toggleReactionReturns200() throws Exception {
        var user = newUser();
        Post post = postRepository.save(Post.builder().author(user).content("Post").build());

        mockMvc.perform(post("/api/reactions/posts/{postId}", post.getId())
                        .param("type", "like")
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.reacted").value(true));
    }

    @Test
    @DisplayName("POST /api/reactions/comments/{commentId} -> 200 for an authenticated user")
    void toggleCommentReactionReturns200() throws Exception {
        var user = newUser();
        Post post = postRepository.save(Post.builder().author(user).content("Post").build());
        var comment = mockMvc.perform(multipart("/api/posts/{postId}/comment", post.getId())
                        .param("Content", "Hi")
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String commentId = objectMapper.readTree(comment).path("result").path("id").asText();

        mockMvc.perform(post("/api/reactions/comments/{commentId}", commentId)
                        .param("type", "love")
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.reacted").value(true));
    }
}