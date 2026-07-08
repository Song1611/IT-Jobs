package com.itjob.service;


import com.itjob.dto.request.UserUpdateRequest;
import com.itjob.dto.response.PageResponse;
import com.itjob.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

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
}
