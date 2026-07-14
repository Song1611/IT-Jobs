package com.itjob.service;

import com.itjob.dto.request.JobRequest;
import com.itjob.dto.response.JobResponse;
import com.itjob.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface JobService {
    
    // Guest & Candidate APIs
    List<JobResponse> getFeaturedJobs(int limit);
    
    // Search jobs with filter array (Specification-based)
    PageResponse<JobResponse> searchJobs(String[] filters, Pageable pageable);
    
    JobResponse getJobById(UUID id, UUID currentUserId);
    
    // HR APIs
    PageResponse<JobResponse> getCompanyJobs(UUID companyId, String status, Pageable pageable);
    
    JobResponse createJob(UUID companyId, JobRequest request, UUID userId);
    
    JobResponse updateJob(UUID id, UUID companyId, JobRequest request, UUID userId);
    
    void deleteJob(UUID id, UUID companyId, UUID userId);
    
    // Admin APIs
    PageResponse<JobResponse> getAllJobs(String status, Pageable pageable);
    
    void approveJob(UUID id, UUID adminId);
    
    void rejectJob(UUID id, UUID adminId, String reason);
}
