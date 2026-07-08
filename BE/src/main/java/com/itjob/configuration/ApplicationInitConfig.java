package com.itjob.configuration;

import com.itjob.constant.PredefinedRole;
import com.itjob.entity.Role;
import com.itjob.entity.User;
import com.itjob.repository.RoleRepository;
import com.itjob.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;

    @NonFinal
    static final String ADMIN_USER_NAME = "admin123";

    @NonFinal
    static final String ADMIN_PASSWORD = "admin123";

    @Bean
    @ConditionalOnProperty(
            prefix = "spring.datasource",
            name = "driver-class-name",
            havingValue = "org.postgresql.Driver"
    )
    ApplicationRunner applicationRunner(UserRepository userRepository, RoleRepository roleRepository) {
        log.info("Initializing application.....");
        return args -> {
            // Initialize roles if they don't exist
            Role userRole = roleRepository.findById(PredefinedRole.USER_ROLE)
                    .orElseGet(() -> {
                        log.info("Creating USER role");
                        return roleRepository.save(Role.builder()
                                .name(PredefinedRole.USER_ROLE)
                                .description("User role")
                                .build());
                    });

            Role adminRole = roleRepository.findById(PredefinedRole.ADMIN_ROLE)
                    .orElseGet(() -> {
                        log.info("Creating ADMIN role");
                        return roleRepository.save(Role.builder()
                                .name(PredefinedRole.ADMIN_ROLE)
                                .description("Admin role")
                                .build());
                    });

            // Initialize admin user if it doesn't exist
            if (userRepository.findByEmail(ADMIN_USER_NAME).isEmpty()) {
                var roles = new HashSet<Role>();
                roles.add(adminRole);

                User user = User.builder()
                        .email(ADMIN_USER_NAME)
                        .fullName("Administrator")
                        .password(passwordEncoder.encode(ADMIN_PASSWORD))
                        .roles(roles)
                        .build();

                userRepository.save(user);
                log.warn("admin user has been created with default password: admin, please change it");
            }
            log.info("Application initialization completed .....");
        };
    }

}
