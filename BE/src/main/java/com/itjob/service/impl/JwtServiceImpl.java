package com.itjob.service.impl;

import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.service.JwtService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
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