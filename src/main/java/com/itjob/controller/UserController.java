package com.itjob.controller;

import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.dto.response.UserResponse;
import com.itjob.entity.User;
import com.itjob.service.UserService;
import com.itjob.util.FilterParamConverter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    UserService userService;

    @GetMapping()
    public ApiResponse<PageResponse<UserResponse>> getUsers(@RequestParam Map<String, String> allParams, Pageable pageable) {
        log.info("=== FILTER DEBUG START ===");
        log.info("Raw params received: {}", allParams);
        log.info("Param count: {}", allParams.size());
        
        allParams.forEach((key, value) -> {
            log.info("  Param - Key: '{}', Value: '{}'", key, value);
        });

        String[] filters = FilterParamConverter.convertToFilters(allParams);

        log.info("Converted filters array length: {}", filters.length);
        for (int i = 0; i < filters.length; i++) {
            log.info("  Filter[{}]: '{}'", i, filters[i]);
        }
        log.info("=== FILTER DEBUG END ===");

        log.info("Pageable: {}", pageable);

        return ApiResponse.<PageResponse<UserResponse>>builder()
                .result(userService.getUsers(filters, pageable))
                .build();
    }

}
