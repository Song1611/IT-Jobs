package com.itjob.controller;

import com.itjob.dto.request.AuthenticationRequest;
import com.itjob.dto.request.LogoutRequest;
import com.itjob.dto.request.RefreshRequest;
import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.AuthenticationResponse;
import com.itjob.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ApiResponse<AuthenticationResponse> login(
            @Valid
            @RequestBody AuthenticationRequest request) {

        log.info("Login request for user: {}", request.getUsername());

        AuthenticationResponse response =
                authenticationService.authenticate(request);

        return ApiResponse.<AuthenticationResponse>builder()
                .message("Login successful")
                .result(response)
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthenticationResponse> refreshToken(
            @RequestBody RefreshRequest request) {

        log.info("Refresh token request");

        AuthenticationResponse response =
                authenticationService.refreshToken(request);

        return ApiResponse.<AuthenticationResponse>builder()
                .message("Token refreshed successfully")
                .result(response)
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestBody LogoutRequest request) {

        log.info("Logout request");

        authenticationService.logout(request);

        return ApiResponse.<Void>builder()
                .message("Logout successful")
                .build();
    }
}