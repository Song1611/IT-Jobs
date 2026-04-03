package com.itjob.controller;

import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.dto.response.UserResponse;
import com.itjob.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> getUsers(
            @RequestParam(required = false) String[] filter,
            Pageable pageable) {

        return ApiResponse.<PageResponse<UserResponse>>builder()
                .result(userService.getUsers(filter, pageable))
                .build();
    }
}
