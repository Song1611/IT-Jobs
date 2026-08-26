package com.itjob.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared PostgreSQL container for all repository/integration tests.
 * <p>
 * Uses the Testcontainers "singleton container" pattern (started once per JVM in a
 * static block) so that multiple test classes running in the same forked JVM reuse
 * one container. The {@code @Testcontainers} + static {@code @Container} lifecycle is
 * per test class and stops the shared container after the first class, which breaks
 * subsequent classes ("Connection refused") when all classes run in one JVM.
 */
public abstract class AbstractPostgresIntegrationTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("itjob_test")
            .withUsername("test")
            .withPassword("test");

    static {
        POSTGRES.start();
        Runtime.getRuntime().addShutdownHook(new Thread(POSTGRES::stop));
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }
}
