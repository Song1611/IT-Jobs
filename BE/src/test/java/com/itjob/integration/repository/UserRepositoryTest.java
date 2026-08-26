package com.itjob.integration.repository;

import com.itjob.config.AbstractPostgresIntegrationTest;
import com.itjob.entity.Role;
import com.itjob.entity.User;
import com.itjob.repository.RoleRepository;
import com.itjob.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("IT - UserRepository")
class UserRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("save -> assigns UUID, timestamps and default enabled")
    void saveAssignsDefaultValues() {
        // Act
        User saved = userRepository.save(user("candidate@example.com"));

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("findByEmail -> returns user when email exists")
    void findByEmailReturnsUser() {
        // Arrange
        userRepository.save(user("candidate@example.com"));

        // Act
        Optional<User> result = userRepository.findByEmail("candidate@example.com");

        // Assert
        assertThat(result)
                .isPresent()
                .hasValueSatisfying(found ->
                        assertThat(found.getEmail()).isEqualTo("candidate@example.com"));
    }

    @Test
    @DisplayName("findByEmail -> empty when email does not exist")
    void findByEmailReturnsEmptyWhenMissing() {
        // Act
        Optional<User> result = userRepository.findByEmail("missing@example.com");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByEmail -> loads roles for the user")
    void findByEmailLoadsRoles() {
        // Arrange
        Role role = roleRepository.save(Role.builder().name("USER").build());
        User user = user("candidate@example.com");
        user.setRoles(Set.of(role));
        userRepository.save(user);

        // Act
        User found = userRepository.findByEmail("candidate@example.com").orElseThrow();

        // Assert
        assertThat(found.getRoles()).extracting(Role::getName).containsExactly("USER");
    }

    @Test
    @DisplayName("save duplicate email -> throws DataIntegrityViolationException")
    void saveDuplicateEmailThrows() {
        // Arrange
        userRepository.save(user("duplicate@example.com"));
        User duplicate = user("duplicate@example.com");

        // Act & Assert
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("findAll -> applies specification filter")
    void findAllAppliesSpecification() {
        // Arrange
        userRepository.save(user("john@example.com", "John Doe"));
        userRepository.save(user("jane@example.com", "Jane Doe"));

        // Act
        List<User> result = userRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("fullName"), "John Doe"));

        // Assert
        assertThat(result).extracting(User::getEmail).containsExactly("john@example.com");
    }

    private static User user(String email) {
        return user(email, "John Doe");
    }

    private static User user(String email, String fullName) {
        return User.builder()
                .fullName(fullName)
                .email(email)
                .password("hashed-password")
                .build();
    }
}
