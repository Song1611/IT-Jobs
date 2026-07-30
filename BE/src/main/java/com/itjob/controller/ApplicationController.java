package com.itjob.controller;

import com.itjob.dto.request.ApplicationRequest;
import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.ApplicationResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.annotation.RateLimit;
import com.itjob.service.ApplicationService;
import com.itjob.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Slf4j
public class ApplicationController {
    
    private final ApplicationService applicationService;
    
    /**
     * Candidate APIs
     */
    
    @PostMapping
    @RateLimit(key = "apply-job", limit = 10, duration = 60)
    public ApiResponse<ApplicationResponse> applyForJob(
            @Valid @RequestBody ApplicationRequest request) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        
        log.info("User {} applying for job {}", userId, request.getJobId());
        
        return ApiResponse.<ApplicationResponse>builder()
                .message("Application submitted successfully")
                .result(applicationService.applyForJob(request, userId))
                .build();
    }
    
    @GetMapping("/me")
    public ApiResponse<PageResponse<ApplicationResponse>> getMyApplications(
            Pageable pageable) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        
        log.info("Getting applications for user {}", userId);
        
        return ApiResponse.<PageResponse<ApplicationResponse>>builder()
                .message("Applications retrieved successfully")
                .result(applicationService.getMyApplications(userId, pageable))
                .build();
    }
    
    @GetMapping("/{id}")
    public ApiResponse<ApplicationResponse> getApplicationById(
            @PathVariable UUID id) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        
        log.info("Getting application {} for user {}", id, userId);
        
        return ApiResponse.<ApplicationResponse>builder()
                .message("Application retrieved successfully")
                .result(applicationService.getApplicationById(id, userId))
                .build();
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> withdrawApplication(
            @PathVariable UUID id) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        
        log.info("User {} withdrawing application {}", userId, id);
        
        applicationService.withdrawApplication(id, userId);
        
        return ApiResponse.<Void>builder()
                .message("Application withdrawn successfully")
                .build();
    }
}
