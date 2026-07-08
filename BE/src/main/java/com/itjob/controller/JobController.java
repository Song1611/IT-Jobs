package com.itjob.controller;

import com.itjob.dto.request.JobRequest;
import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.JobResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {
    
    private final JobService jobService;
    
    /**
     * Guest & Candidate APIs
     */
    
    @GetMapping("/featured")
    public ApiResponse<List<JobResponse>> getFeaturedJobs(
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Getting featured jobs, limit: {}", limit);
        
        return ApiResponse.<List<JobResponse>>builder()
                .message("Featured jobs retrieved successfully")
                .result(jobService.getFeaturedJobs(limit))
                .build();
    }
    
    /**
     * Search jobs using Specification pattern with filter array
     * 
     * Examples:
     * - GET /api/jobs?filter=title~developer
     * - GET /api/jobs?filter=title~developer&filter=workLocation~hanoi
     * - GET /api/jobs?filter=salaryMax>=1000&filter=type:full-time
     * - GET /api/jobs?filter=level:junior&filter=status:open
     * 
     * Supported operators:
     * - : (EQUALITY)       → filter=type:full-time
     * - ~ (LIKE)           → filter=title~developer
     * - ! (NOT_EQUAL)      → filter=status!closed
     * - > (GREATER)        → filter=salaryMax>2000
     * - >= (GREATER_EQUAL) → filter=salaryMax>=1000
     * - < (LESS)           → filter=quantity<5
     * - <= (LESS_EQUAL)    → filter=quantity<=10
     * - @ (IN)             → filter=type@full-time,part-time
     * - # (BETWEEN)        → filter=salaryMax#1000,3000
     * 
     * OR Logic: Use ' prefix
     * - GET /api/jobs?filter='title~java&filter='title~python
     */
    @GetMapping
    public ApiResponse<PageResponse<JobResponse>> searchJobs(
            @RequestParam(required = false) String[] filter,
            Pageable pageable) {
        
        log.info("Searching jobs with filters: {}", filter != null ? String.join(", ", filter) : "none");
        
        PageResponse<JobResponse> result = jobService.searchJobs(filter, pageable);
        
        return ApiResponse.<PageResponse<JobResponse>>builder()
                .message("Jobs retrieved successfully")
                .result(result)
                .build();
    }
    
    @GetMapping("/{id}")
    public ApiResponse<JobResponse> getJobById(
            @PathVariable UUID id,
            Authentication authentication) {
        
        UUID userId = authentication != null ? 
                UUID.fromString(authentication.getName()) : null;
        
        log.info("Getting job by id: {}, userId: {}", id, userId);
        
        return ApiResponse.<JobResponse>builder()
                .message("Job retrieved successfully")
                .result(jobService.getJobById(id, userId))
                .build();
    }
}
