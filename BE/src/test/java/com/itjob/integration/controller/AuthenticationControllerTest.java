package com.itjob.integration.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("IT - AuthenticationController")
class AuthenticationControllerTest extends AbstractControllerTest {

    private static final AtomicInteger IP_COUNTER = new AtomicInteger(100);

    private String testIp;

    @BeforeEach
    void setUpIp() {
        // unique X-Forwarded-For per test keeps the @RateLimit counters isolated
        testIp = "10.0.0." + IP_COUNTER.incrementAndGet();
    }

    @Test
    @DisplayName("register -> 200 and sends a verification email")
    void registerReturns200() throws Exception {
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        String body = """
                {"email": "%s", "password": "password123", "fullName": "Candidate"}
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Registration successful. Please check your email for verification code."));

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerifyEmail(eq(email), otpCaptor.capture());
        assertThat(otpCaptor.getValue()).hasSize(6);
    }

    @Test
    @DisplayName("register -> 400 for an invalid email")
    void registerInvalidEmailReturns400() throws Exception {
        String body = """
                {"email": "not-an-email", "password": "password123", "fullName": "Candidate"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("register + verify-email -> 200 for the full flow")
    void registerAndVerifyEmailReturns200() throws Exception {
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        String registerBody = """
                {"email": "%s", "password": "password123", "fullName": "Candidate"}
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk());

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerifyEmail(eq(email), otpCaptor.capture());
        String otp = otpCaptor.getValue();

        String verifyBody = """
                {"email": "%s", "otp": "%s"}
                """.formatted(email, otp);
        mockMvc.perform(post("/api/auth/verify-email")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));
    }

    @Test
    @DisplayName("login -> 200 with a refresh token cookie")
    void loginReturns200WithCookie() throws Exception {
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        userRepository.save(enabledUser(email));

        String body = """
                {"username": "%s", "password": "password123"}
                """.formatted(email);
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.authenticated").value(true))
                .andExpect(header().string("Set-Cookie", containsString("refreshToken")));
    }

    @Test
    @DisplayName("login -> 401 for the wrong password")
    void loginWrongPasswordReturns401() throws Exception {
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        userRepository.save(enabledUser(email));

        String body = """
                {"username": "%s", "password": "wrong-password"}
                """.formatted(email);
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("logout -> 200 with a valid token")
    void logoutWithTokenReturns200() throws Exception {
        var user = newUser();
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", bearer(user, "USER"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    @Test
    @DisplayName("logout -> 401 without a token")
    void logoutWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login -> 429 after exceeding the rate limit")
    void loginRateLimitedReturns429() throws Exception {
        String body = """
                {"username": "nobody@example.com", "password": "whatever"}
                """;
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", testIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests());
    }

    private com.itjob.entity.User enabledUser(String email) {
        return userRepository.save(com.itjob.entity.User.builder()
                .fullName("Candidate")
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .enabled(true)
                .build());
    }
}
