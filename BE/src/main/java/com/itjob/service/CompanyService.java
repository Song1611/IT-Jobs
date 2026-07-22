package com.itjob.service;

import com.itjob.dto.request.CompanyRequest;
import com.itjob.dto.response.CompanyResponse;
import com.itjob.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CompanyService {
    
    // Guest & Candidate APIs
    List<CompanyResponse> getTopCompanies(int limit);
    
    PageResponse<CompanyResponse> getActiveCompanies(Pageable pageable);
    
    CompanyResponse getCompanyById(UUID id);
    
    CompanyResponse getCompanyBySlug(String slug);
    
    // HR APIs
    CompanyResponse createCompany(CompanyRequest request, UUID userId);
    
    CompanyResponse updateCompany(UUID id, CompanyRequest request, UUID userId);
    
    CompanyResponse getMyCompany(UUID userId);
    
    // Admin APIs
    PageResponse<CompanyResponse> getAllCompanies(String status, Pageable pageable);
    
    CompanyResponse approveCompany(UUID id, UUID adminId);

    CompanyResponse rejectCompany(UUID id, UUID adminId, String reason);

    CompanyResponse suspendCompany(UUID id, UUID adminId, String reason);
}
