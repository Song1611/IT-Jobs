package com.itjob.service;


import com.itjob.dto.request.ChangePasswordRequest;
import com.itjob.dto.request.UserUpdateRequest;
import com.itjob.dto.response.AttachmentResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface UserService {
    PageResponse<UserResponse> getUsers(String[] filters, Pageable pageable);
    UserResponse getUserById(String id);
    UserResponse getMyInfo();
    
    // User can update own profile, ADMIN can update any user
    UserResponse updateUser(String id, UserUpdateRequest request);
    
    // User can update own profile (no ID needed)
    UserResponse updateMyProfile(UserUpdateRequest request);
    
    // ADMIN only
    void deleteUser(String id);

    // Uploads & profile media
    UserResponse updateAvatar(String id, MultipartFile file);
    UserResponse updateCoverImage(String id, MultipartFile file);
    UserResponse updateCV(String id, MultipartFile file);
    void sendChangePasswordOtp(String id);
    void changePassword(String id, ChangePasswordRequest request);
    PageResponse<AttachmentResponse> getMedia(UUID userId, Pageable pageable);
}
