package com.itjob.controller;

import com.itjob.dto.request.UpdateStatusRequest;
import com.itjob.dto.response.*;
import com.itjob.service.CompanyService;
import com.itjob.service.DashboardService;
import com.itjob.service.JobService;
import com.itjob.service.JwtService;
import com.itjob.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final UserService userService;
    private final CompanyService companyService;
    private final JobService jobService;
    private final DashboardService dashboardService;
    private final JwtService jwtService;
    
    /**
     * Dashboard
     */
    
    @GetMapping("/dashboard/kpi")
    public ApiResponse<DashboardStatsResponse> getDashboardKPI() {
        
        log.info("Getting admin dashboard KPI");
        
        return ApiResponse.<DashboardStatsResponse>builder()
                .message("Dashboard KPI retrieved successfully")
                .result(dashboardService.getAdminDashboardStats())
                .build();
    }
    
    /**
     * User Management
     */
    
    @GetMapping("/users")
    public ApiResponse<PageResponse<UserResponse>> getAllUsers(
            @RequestParam(required = false) String[] filter,
            Pageable pageable) {
        
        log.info("Getting all users with filters");
        
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .message("Users retrieved successfully")
                .result(userService.getUsers(filter, pageable))
                .build();
    }
    
    @PatchMapping("/users/{id}/status")
    public ApiResponse<Void> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        
        log.info("Updating user {} status to {}", id, request.getStatus());
        
        // TODO: Implement user status update in UserService
        
        return ApiResponse.<Void>builder()
                .message("User status updated successfully")
                .build();
    }
    
    /**
     * Company Management
     */
    
    @GetMapping("/companies")
    public ApiResponse<PageResponse<CompanyResponse>> getAllCompanies(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        
        log.info("Getting all companies, status: {}", status);
        
        return ApiResponse.<PageResponse<CompanyResponse>>builder()
                .message("Companies retrieved successfully")
                .result(companyService.getAllCompanies(status, pageable))
                .build();
    }
    
    @PatchMapping("/companies/{id}/approval")
    public ApiResponse<Void> updateCompanyApproval(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request,
            Authentication authentication) {
        
        UUID adminId = jwtService.extractUserId(authentication);
        
        log.info("Admin {} updating company {} approval to {}", adminId, id, request.getStatus());
        
        if ("approved".equalsIgnoreCase(request.getStatus()) || 
            "active".equalsIgnoreCase(request.getStatus())) {
            companyService.approveCompany(id, adminId);
        } else if ("rejected".equalsIgnoreCase(request.getStatus())) {
            companyService.rejectCompany(id, adminId, request.getReason());
        } else if ("suspended".equalsIgnoreCase(request.getStatus())) {
            companyService.suspendCompany(id, adminId, request.getReason());
        }
        
        return ApiResponse.<Void>builder()
                .message("Company approval updated successfully")
                .build();
    }
    
    /**
     * Job Management
     */
    
    @GetMapping("/jobs")
    public ApiResponse<PageResponse<JobResponse>> getAllJobs(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        
        log.info("Getting all jobs, status: {}", status);
        
        return ApiResponse.<PageResponse<JobResponse>>builder()
                .message("Jobs retrieved successfully")
                .result(jobService.getAllJobs(status, pageable))
                .build();
    }
    
    @PatchMapping("/jobs/{id}/approval")
    public ApiResponse<Void> updateJobApproval(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request,
            Authentication authentication) {
        
        UUID adminId = jwtService.extractUserId(authentication);
        
        log.info("Admin {} updating job {} approval to {}", adminId, id, request.getStatus());
        
        if ("approved".equalsIgnoreCase(request.getStatus()) || 
            "open".equalsIgnoreCase(request.getStatus())) {
            jobService.approveJob(id, adminId);
        } else if ("rejected".equalsIgnoreCase(request.getStatus())) {
            jobService.rejectJob(id, adminId, request.getReason());
        }
        
        return ApiResponse.<Void>builder()
                .message("Job approval updated successfully")
                .build();
    }
}
