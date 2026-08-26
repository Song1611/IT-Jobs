package com.itjob.controller;

import com.itjob.dto.request.ChangePasswordRequest;
import com.itjob.dto.request.UserUpdateRequest;
import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.AttachmentResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.dto.response.UserResponse;
import com.itjob.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    /**
     * Get list of users (Admin only)
     */
    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> getUsers(
            @RequestParam(required = false) String[] filter,
            Pageable pageable) {

        return ApiResponse.<PageResponse<UserResponse>>builder()
                .result(userService.getUsers(filter, pageable))
                .build();
    }
    
    /**
     * Get current logged-in user info
     */
    @GetMapping("/my-info")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }
    
    /**
     * Update current logged-in user profile
     */
    @PutMapping("/my-profile")
    public ApiResponse<UserResponse> updateMyProfile(@Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateMyProfile(request))
                .build();
    }
    
    /**
     * Get user by ID (Admin only)
     */
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable String id) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUserById(id))
                .build();
    }
    
    /**
     * Update user by ID
     * - User can update own profile (PostAuthorize checks email matches)
     * - Admin can update any user
     */
    @PutMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(id, request))
                .build();
    }
    
    /**
     * Delete user by ID (Admin only)
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ApiResponse.<Void>builder()
                .message("User deleted successfully")
                .build();
    }

    /**
     * Update avatar (multipart file field: "avatar")
     */
    @PutMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> updateAvatar(
            @PathVariable String id,
            @RequestParam("avatar") MultipartFile file) {
        return ApiResponse.<UserResponse>builder()
                .message("Avatar updated successfully")
                .result(userService.updateAvatar(id, file))
                .build();
    }

    /**
     * Update cover image (multipart file field: "coverImage")
     */
    @PutMapping(value = "/{id}/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> updateCoverImage(
            @PathVariable String id,
            @RequestParam("coverImage") MultipartFile file) {
        return ApiResponse.<UserResponse>builder()
                .message("Cover image updated successfully")
                .result(userService.updateCoverImage(id, file))
                .build();
    }

    /**
     * Update CV (multipart file field: "cv")
     */
    @PutMapping(value = "/{id}/cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> updateCV(
            @PathVariable String id,
            @RequestParam("cv") MultipartFile file) {
        return ApiResponse.<UserResponse>builder()
                .message("CV updated successfully")
                .result(userService.updateCV(id, file))
                .build();
    }

    /**
     * Send OTP to verify before changing password
     */
    @PostMapping("/{id}/send-change-password-otp")
    public ApiResponse<Void> sendChangePasswordOtp(@PathVariable String id) {
        userService.sendChangePasswordOtp(id);
        return ApiResponse.<Void>builder()
                .message("Password change OTP sent to your email")
                .build();
    }

    /**
     * Change password
     */
    @PostMapping("/{id}/change-password")
    public ApiResponse<Void> changePassword(
            @PathVariable String id,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request);
        return ApiResponse.<Void>builder()
                .message("Password changed successfully")
                .build();
    }

    /**
     * Get user media (images/videos from their posts)
     */
    @GetMapping("/{id}/media")
    public ApiResponse<PageResponse<AttachmentResponse>> getMedia(
            @PathVariable UUID id,
            Pageable pageable) {
        return ApiResponse.<PageResponse<AttachmentResponse>>builder()
                .message("Media retrieved successfully")
                .result(userService.getMedia(id, pageable))
                .build();
    }
}
