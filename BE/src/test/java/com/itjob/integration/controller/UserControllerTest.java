package com.itjob.integration.controller;

import com.itjob.constant.OtpConstant;
import com.itjob.entity.User;
import com.itjob.redis.RedisKeys;
import com.itjob.service.storage.CloudinaryService;
import com.itjob.service.storage.CloudinaryUploadResult;
import com.itjob.util.HashUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("IT - UserController")
class UserControllerTest extends AbstractControllerTest {

    @MockitoBean
    private CloudinaryService cloudinaryService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("GET /api/users -> 401 without a token")
    void getUsersWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/users -> 403 for USER")
    void getUsersByUserReturns403() throws Exception {
        var user = newUser();
        mockMvc.perform(get("/api/users")
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/users -> 200 for ADMIN")
    void getUsersByAdminReturns200() throws Exception {
        var admin = newAdmin();
        mockMvc.perform(get("/api/users")
                        .header("Authorization", bearer(admin, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    @DisplayName("GET /api/users/my-info -> 401 without a token")
    void getMyInfoWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users/my-info")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/users/my-info -> 200 for an authenticated user")
    void getMyInfoReturns200() throws Exception {
        var user = newUser();
        mockMvc.perform(get("/api/users/my-info")
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.email").value(user.getEmail()));
    }

    @Test
    @DisplayName("PUT /api/users/my-profile -> 200 updates the profile")
    void updateMyProfileReturns200() throws Exception {
        var user = newUser();
        String body = "{\"fullName\":\"Updated Name\"}";
        mockMvc.perform(put("/api/users/my-profile")
                        .header("Authorization", bearer(user, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.fullName").value("Updated Name"));
    }

    @Test
    @DisplayName("GET /api/users/{id} -> 200 for ADMIN")
    void getUserByIdByAdminReturns200() throws Exception {
        var target = newUser();
        var admin = newAdmin();
        mockMvc.perform(get("/api/users/{id}", target.getId())
                        .header("Authorization", bearer(admin, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(target.getId().toString()));
    }

    @Test
    @DisplayName("GET /api/users/{id} -> 403 for USER")
    void getUserByIdByUserReturns403() throws Exception {
        var user = newUser();
        mockMvc.perform(get("/api/users/{id}", UUID.randomUUID())
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/users/{id} -> 200 for the owner")
    void updateUserByIdReturns200() throws Exception {
        var user = newUser();
        String body = "{\"fullName\":\"Self Updated\"}";
        mockMvc.perform(put("/api/users/{id}", user.getId())
                        .header("Authorization", bearer(user, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.fullName").value("Self Updated"));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} -> 403 for USER")
    void deleteUserByUserReturns403() throws Exception {
        var user = newUser();
        mockMvc.perform(delete("/api/users/{id}", UUID.randomUUID())
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/users/{id} -> 200 for ADMIN")
    void deleteUserByAdminReturns200() throws Exception {
        var target = newUser();
        var admin = newAdmin();
        mockMvc.perform(delete("/api/users/{id}", target.getId())
                        .header("Authorization", bearer(admin, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully"));
    }

    @Test
    @DisplayName("PUT /api/users/{id}/avatar -> 200 updates the avatar")
    void updateAvatarReturns200() throws Exception {
        var user = newUser();
        when(cloudinaryService.upload(any(), anyString()))
                .thenReturn(new CloudinaryUploadResult("https://cdn/avatar.png", "a-1", "image"));
        MockMultipartFile file = new MockMultipartFile("avatar", "a.png", "image/png", new byte[]{1});

        mockMvc.perform(multipart("/api/users/{id}/avatar", user.getId())
                        .file(file)
                        .header("Authorization", bearer(user, "USER"))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Avatar updated successfully"));
    }

    @Test
    @DisplayName("POST /api/users/{id}/send-change-password-otp -> 200")
    void sendChangePasswordOtpReturns200() throws Exception {
        var user = newUser();
        mockMvc.perform(post("/api/users/{id}/send-change-password-otp", user.getId())
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password change OTP sent to your email"));
    }

    @Test
    @DisplayName("POST /api/users/{id}/change-password -> 200 with a valid OTP")
    void changePasswordReturns200() throws Exception {
        var user = newUser();
        seedOtp(user.getEmail(), "123456");
        String body = "{\"currentPassword\":\"password123\",\"newPassword\":\"newpassword123\",\"otp\":\"123456\"}";
        mockMvc.perform(post("/api/users/{id}/change-password", user.getId())
                        .header("Authorization", bearer(user, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @DisplayName("POST /api/users/{id}/change-password -> 400 for invalid body")
    void changePasswordInvalidBodyReturns400() throws Exception {
        var user = newUser();
        mockMvc.perform(post("/api/users/{id}/change-password", user.getId())
                        .header("Authorization", bearer(user, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/users/{id}/media -> 200 for an existing user")
    void getMediaReturns200() throws Exception {
        var user = newUser();
        mockMvc.perform(get("/api/users/{id}/media", user.getId())
                        .header("Authorization", bearer(user, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items").isArray());
    }

    private void seedOtp(String email, String otp) {
        stringRedisTemplate.opsForValue().set(
                RedisKeys.otp(email), HashUtil.sha256(otp), OtpConstant.OTP_TTL);
    }
}