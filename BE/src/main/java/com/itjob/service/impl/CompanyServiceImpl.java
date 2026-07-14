package com.itjob.service.impl;

import com.itjob.constant.CacheName;
import com.itjob.constant.CompanyStatus;
import com.itjob.constant.JobStatus;
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
import com.itjob.repository.projection.CompanyJobCountProjection;
import com.itjob.service.CompanyService;
import com.itjob.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    @Cacheable(value = CacheName.COMPANY_FEATURED,
               key = "T(com.itjob.util.CacheKeyGenerator).forLimit(#limit)")
    public List<CompanyResponse> getTopCompanies(int limit) {
        log.debug("Fetching {} top companies from database", limit);
        Pageable pageable = PageRequest.of(0, limit);
        List<Company> companies = companyRepository.findTopCompanies(pageable);
        
        // Optimize N+1 query using batch query
        return mapToCompanyResponsesWithJobCount(companies);
    }
    
    @Override
    @Cacheable(value = CacheName.COMPANY_SEARCH,
               key = "T(com.itjob.util.CacheKeyGenerator).forPageable(#pageable)")
    public PageResponse<CompanyResponse> getActiveCompanies(Pageable pageable) {
        log.debug("Fetching active companies from database");
        Page<Company> companyPage = companyRepository.findActiveCompanies(pageable);
        
        // Optimize N+1 query using batch query
        List<CompanyResponse> items = mapToCompanyResponsesWithJobCount(companyPage.getContent());
        
        return PageResponse.<CompanyResponse>builder()
                .items(items)
                .page(companyPage.getNumber())
                .size(companyPage.getSize())
                .totalElements(companyPage.getTotalElements())
                .totalPages(companyPage.getTotalPages())
                .build();
    }
    
    @Override
    @Cacheable(value = CacheName.COMPANY_BY_ID, 
               key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)")
    public CompanyResponse getCompanyById(UUID id) {
        log.debug("Fetching company {} from database", id);
        Company company = companyRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        
        // TODO: Implement view count tracking with RedisTemplate + async + scheduled DB sync
        // Current: View count increment is disabled when cache is hit (cache bypass issue)
        
        return mapToCompanyResponse(company);
    }
    
    @Override
    @Cacheable(value = CacheName.COMPANY_BY_SLUG,
               key = "T(com.itjob.util.CacheKeyGenerator).forSlug(#slug)")
    public CompanyResponse getCompanyBySlug(String slug) {
        log.debug("Fetching company by slug {} from database", slug);
        Company company = companyRepository.findBySlugAndIsDeleted(slug, false)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        
        // TODO: Implement view count tracking with RedisTemplate + async + scheduled DB sync
        // Current: View count increment is disabled when cache is hit (cache bypass issue)
        
        return mapToCompanyResponse(company);
    }
    
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheName.COMPANY_FEATURED, allEntries = true),
            @CacheEvict(value = CacheName.COMPANY_SEARCH, allEntries = true),
            @CacheEvict(value = CacheName.DASHBOARD_ADMIN, allEntries = true)
    })
    public CompanyResponse createCompany(CompanyRequest request, UUID userId) {
        log.info("Creating company for user {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Check if user already has a company (1 HR = 1 Company rule)
        if (companyRepository.existsByCreatedByIdAndIsDeleted(userId, false)) {
            throw new AppException(ErrorCode.COMPANY_ALREADY_EXISTS);
        }
        
        Company company = companyMapper.toCompany(request);
        company.setCreatedBy(user);
        company.setSlug(SlugUtil.generateSlug(request.getName()));
        company.setStatus(CompanyStatus.PENDING.getValue()); // Require admin approval
        
        company = companyRepository.save(company);
        
        // Optimization: New company has 0 jobs, no need to query
        CompanyResponse response = companyMapper.toCompanyResponse(company);
        response.setJobCount(0);
        return response;
    }
    
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheName.COMPANY_BY_ID, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)"),
            @CacheEvict(value = CacheName.COMPANY_BY_SLUG, allEntries = true),
            @CacheEvict(value = CacheName.COMPANY_FEATURED, allEntries = true),
            @CacheEvict(value = CacheName.COMPANY_SEARCH, allEntries = true),
            @CacheEvict(value = CacheName.JOB_BY_COMPANY, allEntries = true),
            @CacheEvict(value = CacheName.DASHBOARD_ADMIN, allEntries = true)
    })
    public CompanyResponse updateCompany(UUID id, CompanyRequest request, UUID userId) {
        log.info("Updating company {}", id);
        Company company = companyRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        
        // Check if user has permission to update this company
        if (!company.getCreatedBy().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        // Save old name to check if slug needs update
        String oldName = company.getName();
        
        companyMapper.updateCompany(company, request);
        
        // Update slug if name changed
        if (request.getName() != null && !request.getName().equals(oldName)) {
            company.setSlug(SlugUtil.generateSlug(request.getName()));
        }
        
        company = companyRepository.save(company);
        
        return mapToCompanyResponse(company);
    }
    
    @Override
    public CompanyResponse getMyCompany(UUID userId) {
        // Use dedicated repository method instead of findAll().stream()
        // Exclude deleted companies
        Company company = companyRepository.findByCreatedByIdAndIsDeleted(userId, false)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        
        return mapToCompanyResponse(company);
    }
    
    /**
     * Helper method to map Company entity to CompanyResponse with jobCount
     * For single company (no batch optimization needed)
     */
    private CompanyResponse mapToCompanyResponse(Company company) {
        CompanyResponse response = companyMapper.toCompanyResponse(company);
        long jobCount = jobRepository.countByCompanyIdAndStatus(company.getId(), JobStatus.OPEN.getValue());
        response.setJobCount((int) jobCount);
        return response;
    }
    
    /**
     * Helper method to map multiple companies with optimized batch query for jobCount
     * Solves N+1 query problem using type-safe projection
     */
    private List<CompanyResponse> mapToCompanyResponsesWithJobCount(List<Company> companies) {
        if (companies.isEmpty()) {
            return List.of();
        }
        
        // Extract company IDs
        List<UUID> companyIds = companies.stream()
                .map(Company::getId)
                .collect(Collectors.toList());
        
        // Batch query: Get all job counts in one query using type-safe projection
        List<CompanyJobCountProjection> jobCounts = jobRepository.countJobsByCompanyIdsAndStatus(
                companyIds, 
                JobStatus.OPEN.getValue()
        );
        
        // Build map: companyId -> jobCount using modern Java stream API with merge function
        Map<UUID, Long> jobCountMap = jobCounts.stream()
                .collect(Collectors.toMap(
                        CompanyJobCountProjection::getCompanyId,
                        CompanyJobCountProjection::getJobCount,
                        Long::sum  // Merge function for duplicate keys (best practice)
                ));
        
        // Map companies to responses with job counts from map
        return companies.stream()
                .map(company -> {
                    CompanyResponse response = companyMapper.toCompanyResponse(company);
                    long jobCount = jobCountMap.getOrDefault(company.getId(), 0L);
                    response.setJobCount((int) jobCount);
                    return response;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public PageResponse<CompanyResponse> getAllCompanies(String status, Pageable pageable) {
        Page<Company> companyPage;
        
        if (status != null && !status.isEmpty()) {
            // Validate and convert status string to enum
            CompanyStatus companyStatus = CompanyStatus.fromValue(status);
            companyPage = companyRepository.findByStatusAndIsDeleted(companyStatus.getValue(), false, pageable);
        } else {
            // Get all non-deleted companies
            companyPage = companyRepository.findByIsDeleted(false, pageable);
        }
        
        // Optimize N+1 query using batch query
        List<CompanyResponse> items = mapToCompanyResponsesWithJobCount(companyPage.getContent());
        
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
    @Caching(evict = {
            @CacheEvict(value = CacheName.COMPANY_BY_ID, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)"),
            @CacheEvict(value = CacheName.COMPANY_BY_SLUG, allEntries = true),
            @CacheEvict(value = CacheName.COMPANY_FEATURED, allEntries = true),
            @CacheEvict(value = CacheName.COMPANY_SEARCH, allEntries = true),
            @CacheEvict(value = CacheName.DASHBOARD_ADMIN, allEntries = true)
    })
    public void approveCompany(UUID id, UUID adminId) {
        Company company = companyRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        
        company.setStatus(CompanyStatus.ACTIVE.getValue());
        company.setVerifiedAt(LocalDateTime.now());
        companyRepository.save(company);
        
        log.info("Company {} approved by admin {}", id, adminId);
    }
    
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheName.COMPANY_BY_ID, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)"),
            @CacheEvict(value = CacheName.COMPANY_BY_SLUG, allEntries = true),
            @CacheEvict(value = CacheName.COMPANY_FEATURED, allEntries = true),
            @CacheEvict(value = CacheName.COMPANY_SEARCH, allEntries = true),
            @CacheEvict(value = CacheName.DASHBOARD_ADMIN, allEntries = true)
    })
    public void rejectCompany(UUID id, UUID adminId, String reason) {
        Company company = companyRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        
        company.setStatus(CompanyStatus.REJECTED.getValue());
        companyRepository.save(company);
        
        log.info("Company {} rejected by admin {}: {}", id, adminId, reason);
    }
    
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheName.COMPANY_BY_ID, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)"),
            @CacheEvict(value = CacheName.COMPANY_BY_SLUG, allEntries = true),
            @CacheEvict(value = CacheName.COMPANY_FEATURED, allEntries = true),
            @CacheEvict(value = CacheName.COMPANY_SEARCH, allEntries = true),
            @CacheEvict(value = CacheName.DASHBOARD_ADMIN, allEntries = true)
    })
    public void suspendCompany(UUID id, UUID adminId, String reason) {
        Company company = companyRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        
        company.setStatus(CompanyStatus.SUSPENDED.getValue());
        companyRepository.save(company);
        
        log.info("Company {} suspended by admin {}: {}", id, adminId, reason);
    }
}
