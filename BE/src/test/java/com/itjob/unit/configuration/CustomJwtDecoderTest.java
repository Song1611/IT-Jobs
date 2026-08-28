package com.itjob.configuration;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Unit - CustomJwtDecoder")
class CustomJwtDecoderTest {

    private static final String SIGNER_KEY = "0".repeat(64);

    private CustomJwtDecoder decoder;

    @BeforeEach
    void setUp() {
        decoder = new CustomJwtDecoder(SIGNER_KEY);
    }

    @Test
    @DisplayName("decode -> returns the JWT claims for a valid token")
    void decodeValidToken() throws Exception {
        // Arrange
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS512),
                new JWTClaimsSet.Builder().subject("candidate@example.com").build());
        signedJWT.sign(new MACSigner(SIGNER_KEY.getBytes(StandardCharsets.UTF_8)));

        // Act
        Jwt decoded = decoder.decode(signedJWT.serialize());

        // Assert
        assertThat(decoded.getSubject()).isEqualTo("candidate@example.com");
        assertThat(decoded.getTokenValue()).isEqualTo(signedJWT.serialize());
    }

    @Test
    @DisplayName("decode -> throws JwtException for a token signed with a different key")
    void decodeWrongSignatureThrows() throws Exception {
        // Arrange
        String otherKey = "x".repeat(64);
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS512),
                new JWTClaimsSet.Builder().subject("candidate@example.com").build());
        signedJWT.sign(new MACSigner(otherKey.getBytes(StandardCharsets.UTF_8)));

        // Act & Assert
        String token = signedJWT.serialize();
        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("decode -> throws JwtException for a malformed token")
    void decodeMalformedTokenThrows() {
        assertThatThrownBy(() -> decoder.decode("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }
}