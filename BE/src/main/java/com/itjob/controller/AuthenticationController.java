package com.itjob.controller;

import com.itjob.dto.request.*;
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
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request for user: {}", request.getEmail());
        authenticationService.register(request);
        return ApiResponse.<Void>builder()
                .message("Registration successful. Please check your email for verification code.")
                .build();
    }

    @PostMapping("/verify-email")
    @RateLimit(key = "verify-email", limit = 5, duration = 60)
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        log.info("Verify email request for: {}", request.getEmail());
        authenticationService.verifyEmail(request);
        return ApiResponse.<Void>builder()
                .message("Email verified successfully")
                .build();
    }

    @PostMapping("/resend-otp")
    @RateLimit(key = "resend-otp", limit = 3, duration = 60)
    public ApiResponse<Void> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        log.info("Resend OTP request for: {}", request.getEmail());
        authenticationService.resendOtp(request);
        return ApiResponse.<Void>builder()
                .message("OTP resent successfully")
                .build();
    }

    @PostMapping("/forgot-password")
    @RateLimit(key = "forgot-password", limit = 3, duration = 300)
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.getEmail());
        authenticationService.forgotPassword(request);
        return ApiResponse.<Void>builder()
                .message("If the email exists, a password reset code has been sent")
                .build();
    }

    @PostMapping("/reset-password")
    @RateLimit(key = "reset-password", limit = 5, duration = 60)
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request for email: {}", request.getEmail());
        authenticationService.resetPassword(request);
        return ApiResponse.<Void>builder()
                .message("Password reset successfully")
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

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.<Void>builder()
                .message("Logout successful")
                .build();
    }
}
