package com.itjob.service.impl;

import com.itjob.dto.request.CompanyRequest;
import com.itjob.dto.response.CompanyResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.entity.Company;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.mapper.CompanyMapper;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.JobRepository;
import com.itjob.repository.UserRepository;
import com.itjob.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyServiceImpl implements CompanyService {
    
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final CompanyMapper companyMapper;
    
    @Override
    public List<CompanyResponse> getTopCompanies(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Company> companies = companyRepository.findTopCompanies(pageable);
        
        return companies.stream()
                .map(company -> {
                    CompanyResponse response = companyMapper.toCompanyResponse(company);
                    long jobCount = jobRepository.countByCompanyIdAndStatus(company.getId(), "open");
                    response.setJobCount((int) jobCount);
                    return response;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public PageResponse<CompanyResponse> getActiveCompanies(Pageable pageable) {
        Page<Company> companyPage = companyRepository.findActiveCompanies(pageable);
        
        List<CompanyResponse> items = companyPage.getContent().stream()
                .map(company -> {
                    CompanyResponse response = companyMapper.toCompanyResponse(company);
                    long jobCount = jobRepository.countByCompanyIdAndStatus(company.getId(), "open");
                    response.setJobCount((int) jobCount);
                    return response;
                })
                .collect(Collectors.toList());
        
        return PageResponse.<CompanyResponse>builder()
                .items(items)
                .page(companyPage.getNumber())
                .size(companyPage.getSize())
                .totalElements(companyPage.getTotalElements())
                .totalPages(companyPage.getTotalPages())
                .build();
    }
    
    @Override
    public CompanyResponse getCompanyById(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        
        if (company.getIsDeleted()) {
            throw new AppException(ErrorCode.COMPANY_NOT_FOUND);
        }
        
        CompanyResponse response = companyMapper.toCompanyResponse(company);
        long jobCount = jobRepository.countByCompanyIdAndStatus(company.getId(), "open");
        response.setJobCount((int) jobCount);
        
        // Increment view count
        company.setViewCount(company.getViewCount() + 1);
        companyRepository.save(company);
        
        return response;
    }
    
    @Override
    public CompanyResponse getCompanyBySlug(String slug) {
        Company company = companyRepository.findBySlugAndIsDeleted(slug, false)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        
        CompanyResponse response = companyMapper.toCompanyResponse(company);
        long jobCount = jobRepository.countByCompanyIdAndStatus(company.getId(), "open");
        response.setJobCount((int) jobCount);
        
        // Increment view count
        company.setViewCount(company.getViewCount() + 1);
        companyRepository.save(company);
        
        return response;
    }
    
    @Override
    @Transactional
    public CompanyResponse createCompany(CompanyRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Company company = companyMapper.toCompany(request);
        company.setCreatedBy(user);
        company.setSlug(generateSlug(request.getName()));
        company.setStatus("pending"); // Require admin approval
        
        company = companyRepository.save(company);
        
        CompanyResponse response = companyMapper.toCompanyResponse(company);
        response.setJobCount(0);
        
        return response;
    }
    
    @Override
    @Transactional
    public CompanyResponse updateCompany(UUID id, CompanyRequest request, UUID userId) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        
        // Check if user has permission to update this company
        if (!company.getCreatedBy().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        companyMapper.updateCompany(company, request);
        
        // Update slug if name changed
        if (request.getName() != null && !request.getName().equals(company.getName())) {
            company.setSlug(generateSlug(request.getName()));
        }
        
        company = companyRepository.save(company);
        
        CompanyResponse response = companyMapper.toCompanyResponse(company);
        long jobCount = jobRepository.countByCompanyId(company.getId());
        response.setJobCount((int) jobCount);
        
        return response;
    }
    
    @Override
    public CompanyResponse getMyCompany(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Find company created by this user
        // Note: This assumes one user can only have one company
        // You might want to add a proper relationship or query method
        Company company = companyRepository.findAll().stream()
                .filter(c -> c.getCreatedBy() != null && c.getCreatedBy().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        
        CompanyResponse response = companyMapper.toCompanyResponse(company);
        long jobCount = jobRepository.countByCompanyId(company.getId());
        response.setJobCount((int) jobCount);
        
        return response;
    }
    
    @Override
    public PageResponse<CompanyResponse> getAllCompanies(String status, Pageable pageable) {
        Page<Company> companyPage;
        
        if (status != null && !status.isEmpty()) {
            companyPage = companyRepository.findByStatusAndIsDeleted(status, false, pageable);
        } else {
            companyPage = companyRepository.findAll(pageable);
        }
        
        List<CompanyResponse> items = companyPage.getContent().stream()
                .map(company -> {
                    CompanyResponse response = companyMapper.toCompanyResponse(company);
                    long jobCount = jobRepository.countByCompanyId(company.getId());
                    response.setJobCount((int) jobCount);
                    return response;
                })
                .collect(Collectors.toList());
        
        return PageResponse.<CompanyResponse>builder()
                .items(items)
                .page(companyPage.getNumber())
                .size(companyPage.getSize())
                .totalElements(companyPage.getTotalElements())
                .totalPages(companyPage.getTotalPages())
                .build();
    }
    
    @Override
    @Transactional
    public void approveCompany(UUID id, UUID adminId) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        
        company.setStatus("active");
        company.setVerifiedAt(LocalDateTime.now());
        companyRepository.save(company);
        
        log.info("Company {} approved by admin {}", id, adminId);
    }
    
    @Override
    @Transactional
    public void rejectCompany(UUID id, UUID adminId, String reason) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        
        company.setStatus("rejected");
        companyRepository.save(company);
        
        log.info("Company {} rejected by admin {}: {}", id, adminId, reason);
    }
    
    @Override
    @Transactional
    public void suspendCompany(UUID id, UUID adminId, String reason) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        
        company.setStatus("suspended");
        companyRepository.save(company);
        
        log.info("Company {} suspended by admin {}: {}", id, adminId, reason);
    }
    
    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
