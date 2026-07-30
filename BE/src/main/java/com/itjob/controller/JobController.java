package com.itjob.controller;

import com.itjob.dto.request.JobRequest;
import com.itjob.dto.response.ApiResponse;
import com.itjob.dto.response.JobResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.service.JobService;
import com.itjob.service.SearchHistoryService;
import com.itjob.service.SearchSuggestionService;
import com.itjob.util.SecurityUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Slf4j
@Validated
public class JobController {
    
    private final JobService jobService;
    private final SearchSuggestionService searchSuggestionService;
    private final SearchHistoryService searchHistoryService;
    
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

    @GetMapping("/trending")
    public ApiResponse<List<JobResponse>> getTrendingJobs(
            @RequestParam(defaultValue = "20") int limit) {

        log.info("Getting trending jobs, limit: {}", limit);

        return ApiResponse.<List<JobResponse>>builder()
                .message("Trending jobs retrieved successfully")
                .result(jobService.getTrendingJobs(limit))
                .build();
    }

    @GetMapping("/suggestions")
    public ApiResponse<List<String>> getSuggestions(
            @RequestParam @Size(min = 1, max = 50) String q,
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int limit) {

        log.debug("Job search suggestions for: {}", q);
        List<String> suggestions = searchSuggestionService.getSuggestions(q, limit);
        return ApiResponse.<List<String>>builder()
                .message("Suggestions retrieved successfully")
                .result(suggestions)
                .build();
    }

    @GetMapping("/search/history")
    public ApiResponse<List<String>> getSearchHistory(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {

        UUID userId = SecurityUtil.getCurrentUserId();
        log.debug("Getting search history for user {}", userId);
        List<String> history = searchHistoryService.getSearchHistory(userId, limit);
        return ApiResponse.<List<String>>builder()
                .message("Search history retrieved successfully")
                .result(history)
                .build();
    }

    @GetMapping("/recently-viewed")
    public ApiResponse<List<JobResponse>> getRecentlyViewedJobs(
            @RequestParam(defaultValue = "10") int limit) {

        UUID userId = SecurityUtil.getCurrentUserId();

        log.info("Getting recently viewed jobs for user {}, limit: {}", userId, limit);

        return ApiResponse.<List<JobResponse>>builder()
                .message("Recently viewed jobs retrieved successfully")
                .result(jobService.getRecentlyViewedJobs(userId, limit))
                .build();
    }

    @GetMapping("/recommendations") 
    public ApiResponse<List<JobResponse>> getRecommendedJobs(
            @RequestParam(defaultValue = "10") int limit) {

        UUID userId = SecurityUtil.getCurrentUserId();

        log.info("Getting recommended jobs for user {}, limit: {}", userId, limit);

        return ApiResponse.<List<JobResponse>>builder()
                .message("Recommended jobs retrieved successfully")
                .result(jobService.getRecommendedJobs(userId, limit))
                .build();
    }
    
    /**
     * Search jobs using Specification pattern with filter array
     * Examples:
     * - GET /api/jobs?filter=title~developer
     * - GET /api/jobs?filter=title~developer&filter=workLocation~hanoi
     * - GET /api/jobs?filter=salaryMax>=1000&filter=type:full-time
     * - GET /api/jobs?filter=level:junior&filter=status:open
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
     * OR Logic: Use ' prefix
     * - GET /api/jobs?filter='title~java&filter='title~python
     */
    @GetMapping
    public ApiResponse<PageResponse<JobResponse>> searchJobs(
            @RequestParam(required = false) String[] filter,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            Pageable pageable) {
        
        log.info("Searching jobs with filters: {}", filter != null ? String.join(", ", filter) : "none");
        
        if (keyword != null && !keyword.isBlank()) {
            searchSuggestionService.recordKeyword(keyword);
            UUID userId = SecurityUtil.getCurrentUserId();
            searchHistoryService.recordSearch(userId, keyword);
        }
        
        PageResponse<JobResponse> result = jobService.searchJobs(filter, pageable);
        
        return ApiResponse.<PageResponse<JobResponse>>builder()
                .message("Jobs retrieved successfully")
                .result(result)
                .build();
    }
    
    @GetMapping("/{id}")
    public ApiResponse<JobResponse> getJobById(
            @PathVariable UUID id) {
        
        UUID userId = SecurityUtil.isAuthenticated() ? SecurityUtil.getCurrentUserId() : null;
        
        log.info("Getting job by id: {}, userId: {}", id, userId);
        
        return ApiResponse.<JobResponse>builder()
                .message("Job retrieved successfully")
                .result(jobService.getJobById(id, userId))
                .build();
    }

    /**
     * HR APIs - Company Management
     */
    
    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ApiResponse<PageResponse<JobResponse>> getCompanyJobs(
            @PathVariable UUID companyId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        
        log.info("Getting jobs for company: {}, status: {}, by user: {}", companyId, status, userId);
        
        return ApiResponse.<PageResponse<JobResponse>>builder()
                .message("Company jobs retrieved successfully")
                .result(jobService.getCompanyJobs(companyId, status, pageable))
                .build();
    }
    
    @PostMapping("/company/{companyId}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ApiResponse<JobResponse> createJob(
            @PathVariable UUID companyId,
            @Valid @RequestBody JobRequest request) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        
        log.info("Creating job for company: {} by user: {}", companyId, userId);
        
        return ApiResponse.<JobResponse>builder()
                .message("Job created successfully")
                .result(jobService.createJob(companyId, request, userId))
                .build();
    }
    
    @PutMapping("/{id}/company/{companyId}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ApiResponse<JobResponse> updateJob(
            @PathVariable UUID id,
            @PathVariable UUID companyId,
            @Valid @RequestBody JobRequest request) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        
        log.info("Updating job: {} for company: {} by user: {}", id, companyId, userId);
        
        return ApiResponse.<JobResponse>builder()
                .message("Job updated successfully")
                .result(jobService.updateJob(id, companyId, request, userId))
                .build();
    }
    
    @DeleteMapping("/{id}/company/{companyId}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ApiResponse<Void> deleteJob(
            @PathVariable UUID id,
            @PathVariable UUID companyId) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        
        log.info("Deleting job: {} from company: {} by user: {}", id, companyId, userId);
        
        jobService.deleteJob(id, companyId, userId);
        
        return ApiResponse.<Void>builder()
                .message("Job deleted successfully")
                .build();
    }

    /**
     * Admin APIs
     */
    
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<JobResponse>> getAllJobs(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        
        log.info("Admin getting all jobs with status: {}", status);
        
        return ApiResponse.<PageResponse<JobResponse>>builder()
                .message("All jobs retrieved successfully")
                .result(jobService.getAllJobs(status, pageable))
                .build();
    }
    
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> approveJob(
            @PathVariable UUID id) {
        
        UUID adminId = SecurityUtil.getCurrentUserId();
        
        log.info("Admin {} approving job: {}", adminId, id);
        
        jobService.approveJob(id, adminId);
        
        return ApiResponse.<Void>builder()
                .message("Job approved successfully")
                .build();
    }
    
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> rejectJob(
            @PathVariable UUID id,
            @RequestParam String reason) {
        
        UUID adminId = SecurityUtil.getCurrentUserId();
        
        log.info("Admin {} rejecting job: {} with reason: {}", adminId, id, reason);
        
        jobService.rejectJob(id, adminId, reason);
        
        return ApiResponse.<Void>builder()
                .message("Job rejected successfully")
                .build();
    }
}
