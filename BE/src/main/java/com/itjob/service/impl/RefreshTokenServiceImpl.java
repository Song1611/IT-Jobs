package com.itjob.service.impl;

import com.itjob.entity.RefreshToken;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.RefreshTokenRepository;
import com.itjob.service.RefreshTokenBlacklistService;
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
    private final RefreshTokenBlacklistService blacklistService;

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
    @Transactional
    public RefreshToken verifyRefreshToken(String token) {

        log.debug("Verifying refresh token");

        RefreshToken refreshToken = getRefreshToken(token);

        if (refreshToken.isRevoked()) {
            log.warn("Refresh token reuse detected: {}", token);
            revokeAllTokensForUser(refreshToken.getUsername());
            throw new AppException(ErrorCode.REFRESH_TOKEN_REVOKED);
        }

        if (refreshToken.getExpiryTime().isBefore(Instant.now())) {
            log.warn("Refresh token has expired");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return refreshToken;
    }

    private void revokeAllTokensForUser(String username) {
        var tokens = refreshTokenRepository.findAllByUsernameAndRevokedFalse(username);
        for (RefreshToken t : tokens) {
            t.setRevoked(true);
            blacklistService.blacklist(t.getToken().toString(), t.getExpiryTime());
        }
        refreshTokenRepository.saveAll(tokens);
        log.warn("All refresh tokens revoked and blacklisted for user: {} due to reuse attack", username);
    }

    @Override
    @Transactional
    public RefreshToken revokeRefreshToken(String token) {

        log.debug("Revoking refresh token");

        RefreshToken refreshToken = getRefreshToken(token);

        if (refreshToken.isRevoked()) {
            log.debug("Refresh token already revoked");
            return refreshToken;
        }

        refreshToken.setRevoked(true);

        log.debug("Refresh token revoked successfully");

        return refreshToken;
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