package com.itjob.service.impl;

import com.itjob.entity.RefreshToken;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.RefreshTokenRepository;
import com.itjob.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-duration}")
    private long refreshTokenDuration;

    @Override
    @Transactional
    public String createRefreshToken(User user) {

        log.debug("Creating refresh token for user: {}", user.getEmail());

        RefreshToken refreshToken = RefreshToken.builder()
                .username(user.getEmail())
                .expiryTime(
                        Instant.now()
                                .plus(refreshTokenDuration, ChronoUnit.SECONDS)
                )
                .revoked(false)
                .build();

        refreshToken = refreshTokenRepository.save(refreshToken);

        log.debug("Refresh token created successfully");

        return refreshToken.getToken().toString();
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken verifyRefreshToken(String token) {

        log.debug("Verifying refresh token");

        RefreshToken refreshToken = getRefreshToken(token);

        if (refreshToken.isRevoked()) {
            log.warn("Refresh token has been revoked");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (refreshToken.getExpiryTime().isBefore(Instant.now())) {
            log.warn("Refresh token has expired");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return refreshToken;
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String token) {

        log.debug("Revoking refresh token");

        RefreshToken refreshToken = getRefreshToken(token);

        refreshToken.setRevoked(true);

        log.debug("Refresh token revoked successfully");
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(User user) {

        log.debug("Revoking all refresh tokens for user: {}",
                user.getEmail());

        var tokens = refreshTokenRepository
                .findAllByUsernameAndRevokedFalse(user.getEmail());

        tokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(tokens);

        log.debug("All refresh tokens revoked successfully");
    }


    private RefreshToken getRefreshToken(String token) {

        UUID uuid = parseToken(token);

        return refreshTokenRepository.findByToken(uuid)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found");
                    return new AppException(ErrorCode.UNAUTHENTICATED);
                });
    }

    private UUID parseToken(String token) {

        try {
            return UUID.fromString(token);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid refresh token format");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }
}