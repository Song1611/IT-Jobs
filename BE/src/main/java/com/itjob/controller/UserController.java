package com.itjob.controller;

import com.itjob.dto.request.UserUpdateRequest;
import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.dto.response.UserResponse;
import com.itjob.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

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
}
