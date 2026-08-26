package com.itjob.unit.util;

import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.util.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Unit - SecurityUtil")
class SecurityUtilTest {

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Jwt createJwt(String claimUserId) {
        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("userId", claimUserId);
        claims.put("sub", "user@example.com");
        return new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "HS512"),
                claims
        );
    }

    private void authenticateAs(Jwt jwt) {
        Authentication auth = new UsernamePasswordAuthenticationToken(jwt, null, List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("USER_READ")
        ));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("getCurrentUserId -> returns userId from JWT claim")
    void returnsCurrentUserId() {
        authenticateAs(createJwt(userId.toString()));

        assertThat(SecurityUtil.getCurrentUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("no authentication -> throws UNAUTHENTICATED")
    void unauthenticatedThrows() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(SecurityUtil::getCurrentUserId)
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("non-Jwt principal -> throws UNAUTHENTICATED")
    void nonJwtPrincipalThrows() {
        Authentication auth = new UsernamePasswordAuthenticationToken("plain", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(SecurityUtil::getCurrentUserId)
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("getCurrentEmail -> returns sub claim")
    void returnsCurrentEmail() {
        authenticateAs(createJwt(userId.toString()));

        assertThat(SecurityUtil.getCurrentEmail()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("isAuthenticated -> true with Jwt principal")
    void isAuthenticatedTrue() {
        authenticateAs(createJwt(userId.toString()));

        assertThat(SecurityUtil.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("isAuthenticated -> false without authentication")
    void isAuthenticatedFalse() {
        SecurityContextHolder.clearContext();

        assertThat(SecurityUtil.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("hasRole -> true only for authorities with ROLE_ prefix")
    void hasRoleTrue() {
        authenticateAs(createJwt(userId.toString()));

        assertThat(SecurityUtil.hasRole("ADMIN")).isTrue();
        assertThat(SecurityUtil.hasRole("USER")).isTrue();
        assertThat(SecurityUtil.hasRole("EMPLOYER")).isFalse();
        assertThat(SecurityUtil.hasRole("USER_READ")).isFalse();
    }
}