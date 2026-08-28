package com.itjob.integration.service;

import com.itjob.constant.OtpConstant;
import com.itjob.dto.request.AuthenticationRequest;
import com.itjob.dto.request.ForgotPasswordRequest;
import com.itjob.dto.request.LogoutRequest;
import com.itjob.dto.request.RefreshRequest;
import com.itjob.dto.request.RegisterRequest;
import com.itjob.dto.request.ResendOtpRequest;
import com.itjob.dto.request.ResetPasswordRequest;
import com.itjob.dto.request.VerifyEmailRequest;
import com.itjob.dto.response.AuthenticationResponse;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.redis.RedisKeys;
import com.itjob.repository.RefreshTokenRepository;
import com.itjob.service.AuthenticationService;
import com.itjob.util.HashUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DisplayName("IT - AuthenticationService")
class AuthenticationServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("register -> creates disabled user and sends verify email with OTP")
    void registerCreatesDisabledUserAndSendsOtp() {
        // Arrange
        RegisterRequest request = register("candidate-" + UUID.randomUUID() + "@example.com");

        // Act
        authenticationService.register(request);

        // Assert
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getRoles()).isEmpty();
        assertThat(capturedOtp(request.getEmail())).hasSize(OtpConstant.OTP_LENGTH);
    }

    @Test
    @DisplayName("register -> throws USER_EXISTED for an already enabled user")
    void registerExistingEnabledUserThrows() {
        // Arrange
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        createEnabledUser(email);

        // Act & Assert
        RegisterRequest duplicate = register(email);
        assertThatThrownBy(() -> authenticationService.register(duplicate))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_EXISTED);
    }

    @Test
    @DisplayName("register -> re-registering a disabled user updates info and resends OTP")
    void registerDisabledUserResendsOtp() {
        // Arrange
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        authenticationService.register(register(email));
        stringRedisTemplate.delete(RedisKeys.otpCooldown(email));

        // Act
        RegisterRequest second = register(email);
        second.setFullName("Updated Name");
        authenticationService.register(second);

        // Assert
        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getFullName()).isEqualTo("Updated Name");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(2)).sendVerifyEmail(eq(email), captor.capture());
    }

    @Test
    @DisplayName("verifyEmail -> enables user with the correct OTP")
    void verifyEmailEnablesUser() {
        // Arrange
        RegisterRequest register = register("candidate-" + UUID.randomUUID() + "@example.com");
        authenticationService.register(register);
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail(register.getEmail());
        request.setOtp(capturedOtp(register.getEmail()));

        // Act
        authenticationService.verifyEmail(request);

        // Assert
        User user = userRepository.findByEmail(register.getEmail()).orElseThrow();
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("verifyEmail -> throws OTP_INVALID with wrong OTP and keeps user disabled")
    void verifyEmailWithWrongOtpThrows() {
        // Arrange
        RegisterRequest register = register("candidate-" + UUID.randomUUID() + "@example.com");
        authenticationService.register(register);
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail(register.getEmail());
        request.setOtp("000000");

        // Act & Assert
        assertThatThrownBy(() -> authenticationService.verifyEmail(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.OTP_INVALID);

        User user = userRepository.findByEmail(register.getEmail()).orElseThrow();
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("authenticate -> returns tokens and persists the refresh token")
    void authenticateReturnsTokens() {
        // Arrange
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        createEnabledUser(email);

        // Act
        AuthenticationResponse response = authenticationService.authenticate(authRequest(email, "password123"));

        // Assert
        assertThat(response.isAuthenticated()).isTrue();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(refreshTokenRepository.findByToken(UUID.fromString(response.getRefreshToken()))).isPresent();
    }

    @Test
    @DisplayName("authenticate -> throws UNAUTHENTICATED with wrong password")
    void authenticateWithWrongPasswordThrows() {
        // Arrange
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        createEnabledUser(email);

        // Act & Assert
        AuthenticationRequest request = authRequest(email, "wrong-password");
        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("authenticate -> throws USER_NOT_VERIFIED for a disabled user")
    void authenticateDisabledUserThrows() {
        // Arrange
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        userRepository.save(User.builder()
                .fullName("Candidate")
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .enabled(false)
                .build());

        // Act & Assert
        AuthenticationRequest request = authRequest(email, "password123");
        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_VERIFIED);
    }

    @Test
    @DisplayName("refreshToken -> returns new tokens and revokes the old refresh token")
    void refreshTokenReturnsNewTokensAndRevokesOldToken() {
        // Arrange
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        createEnabledUser(email);
        String oldRefresh = authenticationService.authenticate(authRequest(email, "password123")).getRefreshToken();

        // Act
        AuthenticationResponse refreshed = authenticationService.refreshToken(new RefreshRequest(oldRefresh));

        // Assert
        assertThat(refreshed.isAuthenticated()).isTrue();
        assertThat(refreshed.getAccessToken()).isNotBlank();
        assertThat(refreshed.getRefreshToken()).isNotBlank();
        assertThat(refreshed.getRefreshToken()).isNotEqualTo(oldRefresh);

        RefreshRequest reuseOld = new RefreshRequest(oldRefresh);
        assertThatThrownBy(() -> authenticationService.refreshToken(reuseOld))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.REFRESH_TOKEN_REVOKED);
    }

    @Test
    @DisplayName("logout -> blacklists refresh token so it cannot be reused")
    void logoutBlacklistsToken() {
        // Arrange
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        createEnabledUser(email);
        String refreshToken = authenticationService.authenticate(authRequest(email, "password123")).getRefreshToken();

        // Act
        authenticationService.logout(new LogoutRequest(refreshToken));

        // Assert
        RefreshRequest reuse = new RefreshRequest(refreshToken);
        assertThatThrownBy(() -> authenticationService.refreshToken(reuse))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.REFRESH_TOKEN_REVOKED);
    }

    @Test
    @DisplayName("refreshToken -> throws USER_DISABLED for a disabled user")
    void refreshTokenDisabledUserThrows() {
        // Arrange
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        createEnabledUser(email);
        String refreshToken = authenticationService.authenticate(authRequest(email, "password123")).getRefreshToken();

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setEnabled(false);
        userRepository.save(user);

        // Act & Assert
        RefreshRequest request = new RefreshRequest(refreshToken);
        assertThatThrownBy(() -> authenticationService.refreshToken(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_DISABLED);
    }

    @Test
    @DisplayName("logout -> is a no-op when the token is already blacklisted")
    void logoutTwiceIsNoOp() {
        // Arrange
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        createEnabledUser(email);
        String refreshToken = authenticationService.authenticate(authRequest(email, "password123")).getRefreshToken();
        authenticationService.logout(new LogoutRequest(refreshToken));

        // Act & Assert
        LogoutRequest secondLogout = new LogoutRequest(refreshToken);
        assertThatCode(() -> authenticationService.logout(secondLogout)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("verifyEmail -> throws USER_ALREADY_VERIFIED for an enabled user")
    void verifyEmailAlreadyVerifiedThrows() {
        // Arrange
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        createEnabledUser(email);
        seedOtp(email);

        // Act & Assert
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail(email);
        request.setOtp("123456");
        assertThatThrownBy(() -> authenticationService.verifyEmail(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_ALREADY_VERIFIED);
    }

    @Test
    @DisplayName("resendOtp -> resends a new OTP for a disabled user")
    void resendOtpResendsForDisabledUser() {
        // Arrange
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        authenticationService.register(register(email));
        stringRedisTemplate.delete(RedisKeys.otpCooldown(email));

        // Act
        ResendOtpRequest request = new ResendOtpRequest();
        request.setEmail(email);
        authenticationService.resendOtp(request);

        // Assert
        verify(emailService, times(2)).sendVerifyEmail(eq(email), anyString());
    }

    @Test
    @DisplayName("forgotPassword -> sends a reset OTP for an enabled user")
    void forgotPasswordSendsOtpForEnabledUser() {
        // Arrange
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        createEnabledUser(email);

        // Act
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(email);
        authenticationService.forgotPassword(request);

        // Assert
        verify(emailService).sendForgotPasswordOtp(eq(email), anyString());
    }

    @Test
    @DisplayName("resetPassword -> updates the password with a valid OTP")
    void resetPasswordUpdatesPassword() {
        // Arrange
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        createEnabledUser(email);
        seedOtp(email);

        // Act
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(email);
        request.setOtp("123456");
        request.setNewPassword("newpassword123");
        authenticationService.resetPassword(request);

        // Assert
        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(passwordEncoder.matches("newpassword123", user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("resetPassword -> throws OTP_INVALID for a wrong OTP")
    void resetPasswordInvalidOtpThrows() {
        // Arrange
        String email = "candidate-" + UUID.randomUUID() + "@example.com";
        createEnabledUser(email);

        // Act & Assert
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(email);
        request.setOtp("000000");
        request.setNewPassword("newpassword123");
        assertThatThrownBy(() -> authenticationService.resetPassword(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.OTP_INVALID);
    }

    private void seedOtp(String email) {
        stringRedisTemplate.opsForValue().set(
                RedisKeys.otp(email), HashUtil.sha256("123456"), OtpConstant.OTP_TTL);
    }

    private RegisterRequest register(String email) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword("password123");
        request.setFullName("Candidate");
        return request;
    }

    private AuthenticationRequest authRequest(String email, String password) {
        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername(email);
        request.setPassword(password);
        return request;
    }
}
