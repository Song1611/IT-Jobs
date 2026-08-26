package com.itjob.unit.service;

import com.itjob.entity.Permission;
import com.itjob.entity.Role;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.service.impl.JwtServiceImpl;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Unit - JwtServiceImpl")
class JwtServiceImplTest {

    private static final String SIGNER_KEY =
            "0123456789012345678901234567890123456789012345678901234567890123";

    private JwtServiceImpl jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl();
        ReflectionTestUtils.setField(jwtService, "signerKey", SIGNER_KEY);
        ReflectionTestUtils.setField(jwtService, "accessTokenDuration", 900L);
    }

    @Test
    @DisplayName("generateAccessToken -> returns a non-blank signed JWT with user claims")
    void generateAccessTokenReturnsNonEmptyToken() throws Exception {
        // Arrange
        User user = userWithRole();

        // Act
        String token = jwtService.generateAccessToken(user);

        // Assert
        assertThat(token).isNotBlank();
        SignedJWT parsed = SignedJWT.parse(token);
        assertThat(parsed.verify(new MACVerifier(SIGNER_KEY))).isTrue();
        JWTClaimsSet claims = parsed.getJWTClaimsSet();
        assertThat(claims.getSubject()).isEqualTo("test@example.com");
        assertThat(claims.getClaim("userId")).isEqualTo(user.getId().toString());
    }

    @Test
    @DisplayName("generateAccessToken -> scope contains ROLE_ prefixed roles and plain permissions")
    void scopeContainsRolesAndPermissions() throws Exception {
        // Act
        String token = jwtService.generateAccessToken(userWithRole());

        // Assert
        String scope = SignedJWT.parse(token).getJWTClaimsSet().getClaim("scope").toString();
        assertThat(scope).contains("ROLE_USER", "USER_READ");
    }

    @Test
    @DisplayName("generateAccessToken -> scope is empty when user has no roles")
    void scopeEmptyWithoutRoles() throws Exception {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");

        // Act
        String token = jwtService.generateAccessToken(user);

        // Assert
        assertThat(SignedJWT.parse(token).getJWTClaimsSet().getClaim("scope").toString())
                .isEmpty();
    }

    @Test
    @DisplayName("generateAccessToken -> signer key too short throws UNCATEGORIZED_EXCEPTION")
    void shortSignerKeyThrows() {
        // Arrange
        JwtServiceImpl shortKeyService = new JwtServiceImpl();
        ReflectionTestUtils.setField(shortKeyService, "signerKey", "short");
        ReflectionTestUtils.setField(shortKeyService, "accessTokenDuration", 900L);
        User user = userWithRole();

        // Act & Assert
        assertThatThrownBy(() -> shortKeyService.generateAccessToken(user))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    private User userWithRole() {
        Permission read = new Permission();
        read.setName("USER_READ");
        Role role = new Role();
        role.setName("USER");
        role.setPermissions(Set.of(read));

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setRoles(Set.of(role));
        return user;
    }
}
