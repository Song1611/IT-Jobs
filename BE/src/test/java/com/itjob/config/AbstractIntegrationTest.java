package com.itjob.config;

import org.junit.jupiter.api.AfterEach;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

/**
 * Base class for {@code @SpringBootTest} service integration tests: one shared
 * PostgreSQL + Redis pair started once per JVM (singleton container pattern, no
 * {@code withReuse}) and shared across all subclasses via the Spring context cache.
 * <p>
 * Repository tests that only need PostgreSQL should extend
 * {@link AbstractPostgresIntegrationTest} instead so Redis is not started for them.
 */
public abstract class AbstractIntegrationTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("itjob_test")
            .withUsername("test")
            .withPassword("test");

    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS =
        new GenericContainer<>(DockerImageName.parse("redis:7.4.2"))
            .withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            POSTGRES.stop();
            REDIS.stop();
        }));
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(REDIS.getMappedPort(6379)));
    }

    /**
     * Seeds a Jwt-backed SecurityContext with the given role so services relying on
     * {@code SecurityUtil.getCurrentUserId()}/{@code hasRole(...)} work against the
     * real chain (matching how the app's JwtDecoder builds the Authentication).
     * Authorities are carried by the {@link JwtAuthenticationToken}, so no
     * {@code scope} claim is needed.
     */
    protected void authenticateAs(UUID userId, String email, String role) {
        Jwt jwt = Jwt.withTokenValue(UUID.randomUUID().toString())
            .header("alg", "HS256")
            .claim("sub", email)
            .claim("userId", userId.toString())
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
