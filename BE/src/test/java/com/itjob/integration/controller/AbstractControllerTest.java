package com.itjob.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itjob.config.AbstractIntegrationTest;
import com.itjob.entity.User;
import com.itjob.repository.ApplicationRepository;
import com.itjob.repository.CommentRepository;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.JobRepository;
import com.itjob.repository.PostRepository;
import com.itjob.repository.SkillRepository;
import com.itjob.repository.UserRepository;
import com.itjob.service.EmailService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractControllerTest extends AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Value("${jwt.signerkey}")
    private String signerKey;

    @MockitoBean
    protected EmailService emailService;

    /**
     * Builds a valid signed access token (same key/claims shape the app issues)
     * with the given roles in the scope claim. Avoids JwtService.buildScope so the
     * token is independent of the persisted user's role graph.
     */
    protected String bearer(UUID userId, String email, String... roles) {
        String scope = Arrays.stream(roles)
                .map(role -> "ROLE_" + role)
                .collect(Collectors.joining(" "));
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(email)
                .claim("userId", userId.toString())
                .claim("scope", scope)
                .expirationTime(Date.from(Instant.now().plusSeconds(900)))
                .build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
        try {
            signedJWT.sign(new MACSigner(signerKey.getBytes(StandardCharsets.UTF_8)));
        } catch (com.nimbusds.jose.JOSEException e) {
            throw new IllegalStateException("Failed to sign test JWT", e);
        }
        return "Bearer " + signedJWT.serialize();
    }

    protected String bearer(User user, String... roles) {
        return bearer(user.getId(), user.getEmail(), roles);
    }

    protected User newUser(String email) {
        return userRepository.save(User.builder()
                .fullName("Test User")
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .enabled(true)
                .build());
    }

    protected User newUser() {
        return newUser("user-" + UUID.randomUUID() + "@example.com");
    }

    protected User newEmployer() {
        return newUser("emp-" + UUID.randomUUID() + "@example.com");
    }

    protected User newAdmin() {
        return newUser("admin-" + UUID.randomUUID() + "@example.com");
    }
}
