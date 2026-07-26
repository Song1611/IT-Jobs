package com.itjob.service.impl;

import com.itjob.annotation.DistributedLock;
import com.itjob.dto.request.*;
import com.itjob.dto.response.AuthenticationResponse;
import com.itjob.entity.RefreshToken;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.UserRepository;
import com.itjob.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenBlacklistService blacklistService;
    private final OtpService otpService;
    private final EmailService emailService;

    @Override
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.debug("Authenticating user: {}", request.getUsername());

        User user = userRepository.findByEmail(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Authentication not successful: {}", request.getUsername());
                    return new AppException(ErrorCode.UNAUTHENTICATED);
                });

        if (!user.isEnabled()) {
            log.warn("Unverified user {} attempted to login", user.getEmail());
            throw new AppException(ErrorCode.USER_NOT_VERIFIED);
        }

        boolean matched = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!matched) {
            log.warn("Invalid password for user: {}", request.getUsername());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        log.debug("User authenticated successfully: {}", request.getUsername());

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    @Override
    @DistributedLock(key = "'refresh:' + #request.refreshToken")
    @Transactional
    public AuthenticationResponse refreshToken(RefreshRequest request) {
        log.debug("Refreshing access token");

        if (blacklistService.isBlacklisted(request.getRefreshToken())) {
            log.warn("Refresh token is blacklisted");
            throw new AppException(ErrorCode.REFRESH_TOKEN_REVOKED);
        }

        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());

        User user = userRepository.findByEmail(refreshToken.getUsername())
                .orElseThrow(() -> {
                    log.warn("Authentication failed for user: {}", refreshToken.getUsername());
                    return new AppException(ErrorCode.UNAUTHENTICATED);

                });

        if (!user.isEnabled()) {
            log.warn("Disabled user {} attempted to refresh token", user.getEmail());
            throw new AppException(ErrorCode.USER_DISABLED);
        }

        RefreshToken revokedToken = refreshTokenService.revokeRefreshToken(request.getRefreshToken());
        blacklistService.blacklist(request.getRefreshToken(), revokedToken.getExpiryTime());

        String accessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = refreshTokenService.createRefreshToken(user);

        log.debug("Access token refreshed successfully for user: {}", user.getEmail());

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .authenticated(true)
                .build();
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        log.debug("Logging out user");

        if (blacklistService.isBlacklisted(request.getRefreshToken())) {
            log.debug("Refresh token already blacklisted");
            return;
        }

        RefreshToken token = refreshTokenService.revokeRefreshToken(request.getRefreshToken());
        blacklistService.blacklist(request.getRefreshToken(), token.getExpiryTime());

        log.debug("User logged out successfully");
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        log.debug("Registering user: {}", request.getEmail());

        User existing = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (existing != null) {
            if (existing.isEnabled()) {
                throw new AppException(ErrorCode.USER_EXISTED);
            }

            existing.setFullName(request.getFullName());
            existing.setPassword(passwordEncoder.encode(request.getPassword()));
            userRepository.save(existing);

            String otp = otpService.generateAndStore(existing.getEmail());
            emailService.sendVerifyEmail(existing.getEmail(), otp);

            log.debug("User re-registered successfully: {}", request.getEmail());
            return;
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(false)
                .build();

        userRepository.save(user);

        String otp = otpService.generateAndStore(request.getEmail());
        emailService.sendVerifyEmail(request.getEmail(), otp);

        log.debug("User registered successfully: {}", request.getEmail());
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        log.debug("Verifying email: {}", request.getEmail());

        boolean verified = otpService.verify(request.getEmail(), request.getOtp());
        if (!verified) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.isEnabled()) {
            throw new AppException(ErrorCode.USER_ALREADY_VERIFIED);
        }

        user.setEnabled(true);
        userRepository.save(user);

        log.debug("Email verified successfully: {}", request.getEmail());
    }

    @Override
    public void resendOtp(ResendOtpRequest request) {
        log.debug("Resending OTP for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null || user.isEnabled()) {
            return;
        }

        String otp = otpService.generateAndStore(request.getEmail());
        emailService.sendVerifyEmail(request.getEmail(), otp);

        log.debug("OTP resent for email: {}", request.getEmail());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.debug("Forgot password request for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null || !user.isEnabled()) {
            return;
        }

        String otp = otpService.generateAndStore(request.getEmail());
        emailService.sendForgotPasswordOtp(request.getEmail(), otp);

        log.debug("Forgot password OTP sent for email: {}", request.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.debug("Resetting password for email: {}", request.getEmail());

        boolean verified = otpService.verify(request.getEmail(), request.getOtp());
        if (!verified) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenService.revokeAllUserTokens(user);

        log.debug("Password reset successfully for email: {}", request.getEmail());
    }
}
