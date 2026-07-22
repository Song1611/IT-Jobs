package com.itjob.controller;

import com.itjob.dto.request.AuthenticationRequest;
import com.itjob.dto.request.LogoutRequest;
import com.itjob.dto.request.RefreshRequest;
import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.AuthenticationResponse;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.annotation.RateLimit;
import com.itjob.service.AuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Value("${jwt.refresh-token-duration}")
    private long refreshTokenDuration;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @PostMapping("/login")
    @RateLimit(key = "login", limit = 5, duration = 60)
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
                .secure(cookieSecure)
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
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        RefreshRequest request = new RefreshRequest(refreshToken);
        AuthenticationResponse response =
                authenticationService.refreshToken(request);

        // Update the refresh token cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
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

    @PostMapping("/register")
    @RateLimit(key = "register", limit = 3, duration = 60)
    public ApiResponse<Void> register(@Valid @RequestBody AuthenticationRequest request) {
        log.info("Register request for user: {}", request.getUsername());
        return ApiResponse.<Void>builder()
                .message("Register endpoint not yet implemented")
                .build();
    }

    @PostMapping("/forgot-password")
    @RateLimit(key = "forgot-password", limit = 3, duration = 300)
    public ApiResponse<Void> forgotPassword(@RequestParam String email) {
        log.info("Forgot password request for email: {}", email);
        return ApiResponse.<Void>builder()
                .message("Forgot password endpoint not yet implemented")
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
                .secure(cookieSecure)
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