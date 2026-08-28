package com.itjob.integration.service;

import com.itjob.constant.OtpConstant;
import com.itjob.dto.request.ChangePasswordRequest;
import com.itjob.dto.request.UserUpdateRequest;
import com.itjob.entity.Skill;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.redis.RedisKeys;
import com.itjob.repository.SkillRepository;
import com.itjob.repository.UserRepository;
import com.itjob.service.UserService;
import com.itjob.util.HashUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DisplayName("IT - UserService")
class UserServiceImplTest extends AbstractServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("getMyInfo -> returns the authenticated user")
    @Transactional
    void getMyInfoReturnsCurrentUser() {
        User user = createVerifiedUser("user-" + UUID.randomUUID() + "@example.com");
        authenticateAs(user.getId(), user.getEmail(), "USER");

        var response = userService.getMyInfo();

        assertThat(response.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("getMyInfo -> throws USER_NOT_FOUND for an unknown principal")
    void getMyInfoNotFoundThrows() {
        authenticateAs(UUID.randomUUID(), "missing@example.com", "USER");

        assertThatThrownBy(() -> userService.getMyInfo())
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("getUserById -> returns the user for an admin")
    @Transactional
    void getUserByIdReturnsUser() {
        User admin = createAdmin();
        User target = createVerifiedUser("user-" + UUID.randomUUID() + "@example.com");
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");

        var response = userService.getUserById(target.getId().toString());

        assertThat(response.getEmail()).isEqualTo(target.getEmail());
    }

    @Test
    @DisplayName("getUserById -> throws USER_NOT_FOUND for a missing user")
    void getUserByIdNotFoundThrows() {
        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");

        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> userService.getUserById(randomId.toString()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("updateMyProfile -> updates the user's full name")
    void updateMyProfileUpdatesFullName() {
        User user = createVerifiedUser("user-" + UUID.randomUUID() + "@example.com");
        authenticateAs(user.getId(), user.getEmail(), "USER");
        UserUpdateRequest request = new UserUpdateRequest();
        request.setFullName("Updated Name");

        var response = userService.updateMyProfile(request);

        assertThat(response.getFullName()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("updateUser -> admin updates skills and roles")
    @Transactional
    void updateUserSetsSkillsAndRoles() {
        Skill skill = skillRepository.save(Skill.builder().name("Java-" + UUID.randomUUID()).build());
        User target = createVerifiedUser("user-" + UUID.randomUUID() + "@example.com");
        User admin = createAdmin();
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");

        UserUpdateRequest request = new UserUpdateRequest();
        request.setSkillIds(Set.of(skill.getId()));
        request.setRoles(Set.of("USER"));
        var response = userService.updateUser(target.getId().toString(), request);

        assertThat(response.getSkills()).extracting("id").contains(skill.getId().toString());
    }

    @Test
    @DisplayName("deleteUser -> deletes the user")
    void deleteUserDeletes() {
        User admin = createAdmin();
        User target = createVerifiedUser("user-" + UUID.randomUUID() + "@example.com");
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");

        userService.deleteUser(target.getId().toString());

        assertThat(userRepository.findById(target.getId())).isEmpty();
    }

    @Test
    @DisplayName("sendChangePasswordOtp -> sends a change-password OTP")
    void sendChangePasswordOtpSends() {
        User user = createEnabledUser("user-" + UUID.randomUUID() + "@example.com");

        userService.sendChangePasswordOtp(user.getId().toString());

        verify(emailService).sendChangePasswordOtp(eq(user.getEmail()), anyString());
    }

    @Test
    @DisplayName("changePassword -> updates the password with a valid OTP")
    void changePasswordUpdatesPassword() {
        User user = createEnabledUser("user-" + UUID.randomUUID() + "@example.com");
        seedOtp(user.getEmail(), "123456");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("password123");
        request.setNewPassword("newpassword123");
        request.setOtp("123456");
        userService.changePassword(user.getId().toString(), request);

        User persisted = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertThat(passwordEncoder.matches("newpassword123", persisted.getPassword())).isTrue();
    }

    @Test
    @DisplayName("changePassword -> throws OTP_INVALID for a wrong OTP")
    void changePasswordWrongOtpThrows() {
        User user = createEnabledUser("user-" + UUID.randomUUID() + "@example.com");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("password123");
        request.setNewPassword("newpassword123");
        request.setOtp("000000");
        assertThatThrownBy(() -> userService.changePassword(user.getId().toString(), request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.OTP_INVALID);
    }

    @Test
    @DisplayName("changePassword -> throws CURRENT_PASSWORD_INCORRECT for a wrong current password")
    void changePasswordWrongCurrentThrows() {
        User user = createEnabledUser("user-" + UUID.randomUUID() + "@example.com");
        seedOtp(user.getEmail(), "123456");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong-password");
        request.setNewPassword("newpassword123");
        request.setOtp("123456");
        assertThatThrownBy(() -> userService.changePassword(user.getId().toString(), request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CURRENT_PASSWORD_INCORRECT);
    }

    @Test
    @DisplayName("getMedia -> throws USER_NOT_FOUND for a missing user")
    void getMediaUserNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> userService.getMedia(randomId, PageRequest.of(0, 10)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("getUsers -> returns users for an admin")
    @Transactional
    void getUsersReturnsUsers() {
        User admin = createAdmin();
        createVerifiedUser("user-" + UUID.randomUUID() + "@example.com");
        authenticateAs(admin.getId(), admin.getEmail(), "ADMIN");

        var page = userService.getUsers(null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isPositive();
    }

    private void seedOtp(String email, String otp) {
        stringRedisTemplate.opsForValue().set(
                RedisKeys.otp(email), HashUtil.sha256(otp), OtpConstant.OTP_TTL);
    }
}