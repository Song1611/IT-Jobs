package com.itjob.integration.service;

import com.itjob.entity.RefreshToken;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.repository.RefreshTokenRepository;
import com.itjob.service.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("IT - RefreshTokenService")
class RefreshTokenServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("createRefreshToken -> returns a token that can be verified")
    void createAndVerifyToken() {
        User user = createVerifiedUser("user-" + UUID.randomUUID() + "@example.com");

        String token = refreshTokenService.createRefreshToken(user);
        var verified = refreshTokenService.verifyRefreshToken(token);

        assertThat(token).isNotBlank();
        assertThat(verified.getUsername()).isEqualTo(user.getEmail());
        assertThat(verified.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("revokeRefreshToken -> token can no longer be used")
    void revokeTokenPreventsReuse() {
        User user = createVerifiedUser("user-" + UUID.randomUUID() + "@example.com");
        String token = refreshTokenService.createRefreshToken(user);

        refreshTokenService.revokeRefreshToken(token);

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken(token))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.REFRESH_TOKEN_REVOKED);
    }

    @Test
    @DisplayName("verifyRefreshToken -> throws UNAUTHENTICATED for an unknown token")
    void verifyUnknownTokenThrows() {
        String randomToken = UUID.randomUUID().toString();
        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken(randomToken))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("verifyRefreshToken -> throws UNAUTHENTICATED for a malformed token")
    void verifyMalformedTokenThrows() {
        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken("not-a-uuid"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("verifyRefreshToken -> throws UNAUTHENTICATED for an expired token")
    void verifyExpiredTokenThrows() {
        // Arrange
        User user = createVerifiedUser("user-" + UUID.randomUUID() + "@example.com");
        String token = refreshTokenService.createRefreshToken(user);
        RefreshToken refreshToken = refreshTokenRepository.findByToken(UUID.fromString(token)).orElseThrow();
        refreshToken.setExpiryTime(Instant.now().minusSeconds(1));
        refreshTokenRepository.save(refreshToken);

        // Act & Assert
        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken(token))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("revokeRefreshToken -> a second revoke is a no-op")
    void revokeTwiceIsNoOp() {
        // Arrange
        User user = createVerifiedUser("user-" + UUID.randomUUID() + "@example.com");
        String token = refreshTokenService.createRefreshToken(user);

        // Act & Assert
        refreshTokenService.revokeRefreshToken(token);
        refreshTokenService.revokeRefreshToken(token);
    }

    @Test
    @DisplayName("verifyRefreshToken -> revoked token triggers revocation of all user tokens")
    void verifyRevokedTokenRevokesAllUserTokens() {
        // Arrange
        User user = createVerifiedUser("user-" + UUID.randomUUID() + "@example.com");
        String token1 = refreshTokenService.createRefreshToken(user);
        refreshTokenService.createRefreshToken(user);
        refreshTokenService.revokeRefreshToken(token1);

        // Act & Assert
        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken(token1))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.REFRESH_TOKEN_REVOKED);
    }
}