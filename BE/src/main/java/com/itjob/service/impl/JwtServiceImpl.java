package com.itjob.service.impl;

import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.service.JwtService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.signerkey}")
    private String signerKey;

    @Value("${jwt.access-token-duration}")
    private long accessTokenDuration;

    @Override
    public String generateAccessToken(User user) {
        log.debug("Generating access token for user: {}", user.getEmail());
        
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail())
                .issuer("itjob.com")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plus(accessTokenDuration, ChronoUnit.SECONDS)))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .claim("userId", user.getId().toString())
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(signerKey.getBytes(StandardCharsets.UTF_8)));
            String token = jwsObject.serialize();
            
            log.debug("Access token generated successfully for user: {}", user.getEmail());
            return token;
        } catch (JOSEException e) {
            log.error("Cannot generate JWT for user: {}", user.getEmail(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Override
    public UUID extractUserId(Authentication authentication) {
        log.debug("Extracting user ID from JWT authentication");
        
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication cannot be null");
        }
        
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalArgumentException("Authentication principal must be a JWT");
        }
        
        String userIdClaim = jwt.getClaimAsString("userId");
        if (userIdClaim == null || userIdClaim.trim().isEmpty()) {
            throw new IllegalStateException("JWT must contain a valid 'userId' claim");
        }
        
        try {
            UUID userId = UUID.fromString(userIdClaim);
            log.debug("User ID extracted successfully: {}", userId);
            return userId;
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format in 'userId' claim: {}", userIdClaim, e);
            throw new IllegalStateException("Invalid UUID format in 'userId' claim: " + userIdClaim, e);
        }
    }
    
    @Override
    public UUID extractUserIdSafely(Authentication authentication) {
        log.debug("Safely extracting user ID from JWT authentication");
        
        if (authentication == null) {
            log.debug("Authentication is null, returning null");
            return null;
        }
        
        return extractUserId(authentication);
    }

    private JWTClaimsSet extractAllClaims(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet();
        } catch (ParseException e) {
            log.error("Cannot extract claims from token", e);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private String buildScope(User user) {
        StringJoiner joiner = new StringJoiner(" ");

        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            user.getRoles().forEach(role -> {
                joiner.add("ROLE_" + role.getName());

                if (role.getPermissions() != null && !role.getPermissions().isEmpty()) {
                    role.getPermissions().forEach(permission -> 
                        joiner.add(permission.getName())
                    );
                }
            });
        }

        return joiner.toString();
    }
}