package com.itjob.controller;

import com.itjob.dto.request.CompanyRequest;
import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.CompanyResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.service.CompanyService;
import com.itjob.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Slf4j
public class CompanyController {
    
    private final CompanyService companyService;
    
    /**
     * Guest & Candidate APIs
     */
    
    @GetMapping("/top")
    public ApiResponse<List<CompanyResponse>> getTopCompanies(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Getting top companies, limit: {}", limit);
        
        return ApiResponse.<List<CompanyResponse>>builder()
                .message("Top companies retrieved successfully")
                .result(companyService.getTopCompanies(limit))
                .build();
    }
    
    @GetMapping
    public ApiResponse<PageResponse<CompanyResponse>> getActiveCompanies(
            Pageable pageable) {
        
        log.info("Getting active companies");
        
        return ApiResponse.<PageResponse<CompanyResponse>>builder()
                .message("Companies retrieved successfully")
                .result(companyService.getActiveCompanies(pageable))
                .build();
    }
    
    @GetMapping("/{id}")
    public ApiResponse<CompanyResponse> getCompanyById(@PathVariable UUID id) {
        
        log.info("Getting company by id: {}", id);
        
        return ApiResponse.<CompanyResponse>builder()
                .message("Company retrieved successfully")
                .result(companyService.getCompanyById(id))
                .build();
    }
    
    @GetMapping("/slug/{slug}")
    public ApiResponse<CompanyResponse> getCompanyBySlug(@PathVariable String slug) {
        
        log.info("Getting company by slug: {}", slug);
        
        return ApiResponse.<CompanyResponse>builder()
                .message("Company retrieved successfully")
                .result(companyService.getCompanyBySlug(slug))
                .build();
    }

    /**
     * HR APIs
     */
    
    @PostMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ApiResponse<CompanyResponse> createCompany(
            @Valid @RequestBody CompanyRequest request) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        
        log.info("Creating company by user: {}", userId);
        
        return ApiResponse.<CompanyResponse>builder()
                .message("Company created successfully")
                .result(companyService.createCompany(request, userId))
                .build();
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ApiResponse<CompanyResponse> updateCompany(
            @PathVariable UUID id,
            @Valid @RequestBody CompanyRequest request) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        
        log.info("Updating company: {} by user: {}", id, userId);
        
        return ApiResponse.<CompanyResponse>builder()
                .message("Company updated successfully")
                .result(companyService.updateCompany(id, request, userId))
                .build();
    }
    
    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ApiResponse<CompanyResponse> getMyCompany() {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        
        log.info("Getting company for user: {}", userId);
        
        return ApiResponse.<CompanyResponse>builder()
                .message("Your company retrieved successfully")
                .result(companyService.getMyCompany(userId))
                .build();
    }

    /**
     * Admin APIs
     */
    
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<CompanyResponse>> getAllCompanies(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        
        log.info("Admin getting all companies with status: {}", status);
        
        return ApiResponse.<PageResponse<CompanyResponse>>builder()
                .message("All companies retrieved successfully")
                .result(companyService.getAllCompanies(status, pageable))
                .build();
    }
    
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> approveCompany(
            @PathVariable UUID id) {
        
        UUID adminId = SecurityUtil.getCurrentUserId();
        
        log.info("Admin {} approving company: {}", adminId, id);
        
        companyService.approveCompany(id, adminId);
        
        return ApiResponse.<Void>builder()
                .message("Company approved successfully")
                .build();
    }
    
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> rejectCompany(
            @PathVariable UUID id,
            @RequestParam String reason) {
        
        UUID adminId = SecurityUtil.getCurrentUserId();
        
        log.info("Admin {} rejecting company: {} with reason: {}", adminId, id, reason);
        
        companyService.rejectCompany(id, adminId, reason);
        
        return ApiResponse.<Void>builder()
                .message("Company rejected successfully")
                .build();
    }
    
    @PutMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> suspendCompany(
            @PathVariable UUID id,
            @RequestParam String reason) {
        
        UUID adminId = SecurityUtil.getCurrentUserId();
        
        log.info("Admin {} suspending company: {} with reason: {}", adminId, id, reason);
        
        companyService.suspendCompany(id, adminId, reason);
        
        return ApiResponse.<Void>builder()
                .message("Company suspended successfully")
                .build();
    }
}
