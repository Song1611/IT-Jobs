package com.itjob.service;

import com.itjob.dto.request.ApplicationRequest;
import com.itjob.dto.response.ApplicationResponse;
import com.itjob.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ApplicationService {
    
    // Candidate APIs
    ApplicationResponse applyForJob(ApplicationRequest request, UUID userId);
    
    PageResponse<ApplicationResponse> getMyApplications(UUID userId, Pageable pageable);
    
    ApplicationResponse getApplicationById(UUID id, UUID userId);
    
    void withdrawApplication(UUID id, UUID userId);
    
    // HR/Employer APIs
    PageResponse<ApplicationResponse> getJobApplications(UUID jobId, UUID companyId, String status, Pageable pageable);
    
    PageResponse<ApplicationResponse> getCompanyApplications(UUID companyId, Pageable pageable);
    
    ApplicationResponse updateApplicationStatus(UUID id, UUID companyId, String status, String notes);
    
    void markAsViewed(UUID id, UUID companyId);
}
