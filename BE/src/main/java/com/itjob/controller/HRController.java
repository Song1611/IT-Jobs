package com.itjob.controller;

import com.itjob.dto.request.CompanyRequest;
import com.itjob.dto.request.JobRequest;
import com.itjob.dto.request.UpdateStatusRequest;
import com.itjob.dto.response.*;
import com.itjob.service.ApplicationService;
import com.itjob.service.CompanyService;
import com.itjob.service.DashboardService;
import com.itjob.service.JobService;
import com.itjob.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('EMPLOYER')")
public class HRController {
    
    private final JobService jobService;
    private final CompanyService companyService;
    private final ApplicationService applicationService;
    private final DashboardService dashboardService;
    
    /**
     * Dashboard
     */
    
    @GetMapping("/dashboard/summary")
    public ApiResponse<DashboardStatsResponse> getDashboardSummary(
            @RequestParam UUID companyId) {
        
        log.info("Getting HR dashboard summary for company {}", companyId);
        
        return ApiResponse.<DashboardStatsResponse>builder()
                .message("Dashboard stats retrieved successfully")
                .result(dashboardService.getHRDashboardStats(companyId))
                .build();
    }
    
    /**
     * Company Management
     */
    
    @GetMapping("/company")
    public ApiResponse<CompanyResponse> getMyCompany() {
        UUID userId = SecurityUtil.getCurrentUserId();
        
        log.info("Getting company for user {}", userId);
        
        return ApiResponse.<CompanyResponse>builder()
                .message("Company retrieved successfully")
                .result(companyService.getMyCompany(userId))
                .build();
    }
    
    @PostMapping("/company")
    public ApiResponse<CompanyResponse> createCompany(
            @Valid @RequestBody CompanyRequest request) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        
        log.info("Creating company for user {}", userId);
        
        return ApiResponse.<CompanyResponse>builder()
                .message("Company created successfully")
                .result(companyService.createCompany(request, userId))
                .build();
    }
    
    @PutMapping("/company/{id}")
    public ApiResponse<CompanyResponse> updateCompany(
            @PathVariable UUID id,
            @Valid @RequestBody CompanyRequest request) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        
        log.info("Updating company {} by user {}", id, userId);
        
        return ApiResponse.<CompanyResponse>builder()
                .message("Company updated successfully")
                .result(companyService.updateCompany(id, request, userId))
                .build();
    }
    
    /**
     * Job Management
     */
    
    @GetMapping("/jobs")
    public ApiResponse<PageResponse<JobResponse>> getCompanyJobs(
            @RequestParam UUID companyId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        
        log.info("Getting jobs for company {}, status: {}", companyId, status);
        
        return ApiResponse.<PageResponse<JobResponse>>builder()
                .message("Jobs retrieved successfully")
                .result(jobService.getCompanyJobs(companyId, status, pageable))
                .build();
    }
    
    @PostMapping("/jobs")
    public ApiResponse<JobResponse> createJob(
            @RequestParam UUID companyId,
            @Valid @RequestBody JobRequest request) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        
        log.info("Creating job for company {} by user {}", companyId, userId);
        
        return ApiResponse.<JobResponse>builder()
                .message("Job created successfully")
                .result(jobService.createJob(companyId, request, userId))
                .build();
    }
    
    @PutMapping("/jobs/{id}")
    public ApiResponse<JobResponse> updateJob(
            @PathVariable UUID id,
            @RequestParam UUID companyId,
            @Valid @RequestBody JobRequest request) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        
        log.info("Updating job {} by user {}", id, userId);
        
        return ApiResponse.<JobResponse>builder()
                .message("Job updated successfully")
                .result(jobService.updateJob(id, companyId, request, userId))
                .build();
    }
    
    @DeleteMapping("/jobs/{id}")
    public ApiResponse<Void> deleteJob(
            @PathVariable UUID id,
            @RequestParam UUID companyId) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        
        log.info("Deleting job {} for company {} by user {}", id, companyId, userId);
        
        jobService.deleteJob(id, companyId, userId);
        
        return ApiResponse.<Void>builder()
                .message("Job deleted successfully")
                .build();
    }
    
    /**
     * Application Management
     */
    
    @GetMapping("/applications")
    public ApiResponse<PageResponse<ApplicationResponse>> getJobApplications(
            @RequestParam UUID jobId,
            @RequestParam UUID companyId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        
        log.info("Getting applications for job {}, status: {}", jobId, status);
        
        return ApiResponse.<PageResponse<ApplicationResponse>>builder()
                .message("Applications retrieved successfully")
                .result(applicationService.getJobApplications(jobId, companyId, status, pageable))
                .build();
    }
    
    @GetMapping("/applications/all")
    public ApiResponse<PageResponse<ApplicationResponse>> getAllCompanyApplications(
            @RequestParam UUID companyId,
            Pageable pageable) {
        
        log.info("Getting all applications for company {}", companyId);
        
        return ApiResponse.<PageResponse<ApplicationResponse>>builder()
                .message("Applications retrieved successfully")
                .result(applicationService.getCompanyApplications(companyId, pageable))
                .build();
    }
    
    @PatchMapping("/applications/{id}/status")
    public ApiResponse<ApplicationResponse> updateApplicationStatus(
            @PathVariable UUID id,
            @RequestParam UUID companyId,
            @Valid @RequestBody UpdateStatusRequest request) {
        
        log.info("Updating application {} status to {}", id, request.getStatus());
        
        return ApiResponse.<ApplicationResponse>builder()
                .message("Application status updated successfully")
                .result(applicationService.updateApplicationStatus(
                        id, companyId, request.getStatus(), request.getNotes()))
                .build();
    }
    
    @PatchMapping("/applications/{id}/viewed")
    public ApiResponse<Void> markApplicationAsViewed(
            @PathVariable UUID id,
            @RequestParam UUID companyId) {
        
        log.info("Marking application {} as viewed", id);
        
        applicationService.markAsViewed(id, companyId);
        
        return ApiResponse.<Void>builder()
                .message("Application marked as viewed")
                .build();
    }
}
