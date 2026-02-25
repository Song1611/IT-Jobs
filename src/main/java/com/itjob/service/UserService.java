package com.itjob.service;


import com.itjob.dto.response.PageResponse;
import com.itjob.dto.response.UserResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public interface UserService {
    PageResponse<UserResponse> getUsers(Pageable pageable);
    UserResponse getUserById(String id);

}
