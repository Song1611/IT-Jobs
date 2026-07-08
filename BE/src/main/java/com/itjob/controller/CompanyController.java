package com.itjob.controller;

import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.CompanyResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
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
}
