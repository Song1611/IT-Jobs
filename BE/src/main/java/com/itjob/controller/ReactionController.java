package com.itjob.controller;

import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.ReactionResponse;
import com.itjob.service.JwtService;
import com.itjob.service.ReactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reactions")
@RequiredArgsConstructor
@Slf4j
public class ReactionController {

    private final ReactionService reactionService;
    private final JwtService jwtService;

    @PostMapping("/posts/{postId}")
    public ApiResponse<ReactionResponse> togglePostReaction(
            @PathVariable UUID postId,
            @RequestParam String type,
            Authentication authentication) {
        UUID userId = jwtService.extractUserId(authentication);
        log.info("Toggle reaction on post {} by user {} type {}", postId, userId, type);
        return ApiResponse.<ReactionResponse>builder()
                .result(reactionService.togglePostReaction(postId, userId, type))
                .build();
    }

    @PostMapping("/comments/{commentId}")
    public ApiResponse<ReactionResponse> toggleCommentReaction(
            @PathVariable UUID commentId,
            @RequestParam String type,
            Authentication authentication) {
        UUID userId = jwtService.extractUserId(authentication);
        log.info("Toggle reaction on comment {} by user {} type {}", commentId, userId, type);
        return ApiResponse.<ReactionResponse>builder()
                .result(reactionService.toggleCommentReaction(commentId, userId, type))
                .build();
    }
}
