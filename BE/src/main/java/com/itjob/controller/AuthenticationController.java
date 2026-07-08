package com.itjob.controller;

import com.itjob.dto.request.AuthenticationRequest;
import com.itjob.dto.request.LogoutRequest;
import com.itjob.dto.request.RefreshRequest;
import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.AuthenticationResponse;
import com.itjob.service.AuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Value("${jwt.refresh-token-duration}")
    private long refreshTokenDuration;

    @PostMapping("/login")
    public ApiResponse<AuthenticationResponse> login(
            @Valid
            @RequestBody AuthenticationRequest request,
            HttpServletResponse httpResponse) {

        log.info("Login request for user: {}", request.getUsername());

        AuthenticationResponse response =
                authenticationService.authenticate(request);

        // Set refresh token as HttpOnly Cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                .httpOnly(true)
                .secure(false) // Set to true in production
                .path("/")
                .maxAge(refreshTokenDuration)
                .sameSite("Lax")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.<AuthenticationResponse>builder()
                .message("Login successful")
                .result(response)
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthenticationResponse> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse httpResponse) {

        log.info("Refresh token request");

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new RuntimeException("Refresh token is missing from cookies");
        }

        RefreshRequest request = new RefreshRequest(refreshToken);
        AuthenticationResponse response =
                authenticationService.refreshToken(request);

        // Update the refresh token cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                .httpOnly(true)
                .secure(false) // Set to true in production
                .path("/")
                .maxAge(refreshTokenDuration)
                .sameSite("Lax")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.<AuthenticationResponse>builder()
                .message("Token refreshed successfully")
                .result(response)
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse httpResponse) {

        log.info("Logout request");

        if (refreshToken != null && !refreshToken.trim().isEmpty()) {
            LogoutRequest request = new LogoutRequest(refreshToken);
            authenticationService.logout(request);
        }

        // Clear the refresh token cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0) // Expire immediately
                .sameSite("Lax")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.<Void>builder()
                .message("Logout successful")
                .build();
    }
}