    package com.itjob.service.impl;
    
    import com.itjob.annotation.DistributedLock;
    import com.itjob.redis.CacheName;
    import com.itjob.enums.CompanyStatus;
    import com.itjob.enums.JobStatus;
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
    import com.itjob.enums.ViewEntity;
    import com.itjob.service.CompanyCacheService;
    import com.itjob.service.CompanyService;
    import com.itjob.service.ViewCountService;
    import com.itjob.util.PageResponseUtil;
    import com.itjob.util.SlugUtil;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.cache.annotation.CacheEvict;
    import org.springframework.cache.annotation.CachePut;
    import org.springframework.cache.annotation.Cacheable;
    import org.springframework.cache.annotation.Caching;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Pageable;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;
    
    import java.time.LocalDateTime;
    import java.util.List;
    import java.util.Map;
    import java.util.Optional;
    import java.util.UUID;
    import java.util.stream.Collectors;
    
    @Service
    @RequiredArgsConstructor
    @Slf4j
    public class CompanyServiceImpl implements CompanyService {
        
        private static final int MAX_TOP_COMPANIES = 100;
    
        private final CompanyRepository companyRepository;
        private final JobRepository jobRepository;
        private final UserRepository userRepository;
        private final CompanyMapper companyMapper;
        private final ViewCountService viewCountService;
        private final CompanyCacheService companyCacheService;
        
        @Override
        @Cacheable(value = CacheName.COMPANY_FEATURED,
                   key = "T(com.itjob.util.CacheKeyGenerator).forLimit(#limit)")
        public List<CompanyResponse> getTopCompanies(int limit) {
            if (limit <= 0) {
                throw new AppException(ErrorCode.INVALID_LIMIT);
            }
            if (limit > MAX_TOP_COMPANIES) {
                throw new AppException(ErrorCode.LIMIT_EXCEEDED);
            }
            log.debug("Fetching {} top companies from database", limit);
            Pageable pageable = PageRequest.of(0, limit);
            List<Company> companies = companyRepository.findTopCompanies(CompanyStatus.ACTIVE.getValue(), pageable);
            
            // Optimize N+1 query using batch query
            return mapToCompanyResponsesWithJobCount(companies);
        }
        
        @Override
        @Cacheable(value = CacheName.COMPANY_SEARCH,
                   key = "T(com.itjob.util.CacheKeyGenerator).forPageable(#pageable)")
        public PageResponse<CompanyResponse> getActiveCompanies(Pageable pageable) {
            long start = System.currentTimeMillis();
            log.debug("Fetching active companies from database");
            Page<Company> companyPage = companyRepository.findActiveCompanies(CompanyStatus.ACTIVE.getValue(), pageable);
            
            List<CompanyResponse> items = mapToCompanyResponsesWithJobCount(companyPage.getContent());
            
            PageResponse<CompanyResponse> result = PageResponseUtil.build(companyPage, items);
            log.debug("getActiveCompanies completed in {} ms", System.currentTimeMillis() - start);
            return result;
        }
        
        @Override
        public CompanyResponse getCompanyById(UUID id) {
            CompanyResponse response = companyCacheService.getCachedCompanyById(id);
    
            long jobCount = jobRepository.countByCompanyIdAndStatus(id, JobStatus.OPEN.getValue());
            response.setJobCount((int) jobCount);
    
            viewCountService.incrementView(ViewEntity.COMPANY, id);
    
            long pendingViews = viewCountService.getPendingViewDelta(ViewEntity.COMPANY, id);
            if (pendingViews > 0) {
                Integer current = response.getViewCount();
                response.setViewCount((current == null ? 0 : current) + (int) pendingViews);
            }
            return response;
        }
        
        @Override
        public CompanyResponse getCompanyBySlug(String slug) {
            CompanyResponse response = companyCacheService.getCachedCompanyBySlug(slug);
    
            long jobCount = jobRepository.countByCompanyIdAndStatus(response.getId(), JobStatus.OPEN.getValue());
            response.setJobCount((int) jobCount);
    
            viewCountService.incrementView(ViewEntity.COMPANY, response.getId());
    
            long pendingViews = viewCountService.getPendingViewDelta(ViewEntity.COMPANY, response.getId());
            if (pendingViews > 0) {
                Integer current = response.getViewCount();
                response.setViewCount((current == null ? 0 : current) + (int) pendingViews);
            }
            return response;
        }
        
        @Override
        @Transactional
        @Caching(evict = {
                @CacheEvict(value = CacheName.COMPANY_FEATURED, allEntries = true),
                @CacheEvict(value = CacheName.COMPANY_SEARCH, allEntries = true),
                @CacheEvict(value = CacheName.DASHBOARD_ADMIN,
                            key = "T(com.itjob.util.CacheKeyGenerator).STATS_SUFFIX")
        })
        public CompanyResponse createCompany(CompanyRequest request, UUID userId) {
            log.info("Creating company for user {}", userId);
            User user = userRepository.getReferenceById(userId);
            
            // Check if user already has a company (1 HR = 1 Company rule)
            if (companyRepository.existsByCreatedByIdAndIsDeleted(userId, false)) {
                throw new AppException(ErrorCode.COMPANY_ALREADY_EXISTS);
            }
            
            Company company = companyMapper.toCompany(request);
            company.setCreatedBy(user);
            company.setSlug(generateUniqueSlug(request.getName()));
            company.setStatus(CompanyStatus.PENDING.getValue()); // Require admin approval
            
            company = companyRepository.save(company);
            
            // Optimization: New company has 0 jobs, no need to query
            CompanyResponse response = companyMapper.toCompanyResponse(company);
            response.setJobCount(0);
            return response;
        }
        
        @Override
        @Transactional
        @Caching(
                put = {
                        @CachePut(value = CacheName.COMPANY_BY_ID,
                                  key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)")
                },
                evict = {
                        @CacheEvict(value = CacheName.COMPANY_BY_SLUG, allEntries = true),
                        @CacheEvict(value = CacheName.COMPANY_FEATURED, allEntries = true),
                        @CacheEvict(value = CacheName.COMPANY_SEARCH, allEntries = true),
                        @CacheEvict(value = CacheName.JOB_BY_COMPANY, allEntries = true),
                        @CacheEvict(value = CacheName.COMPANY_MY, allEntries = true),
                        @CacheEvict(value = CacheName.DASHBOARD_ADMIN,
                                    key = "T(com.itjob.util.CacheKeyGenerator).STATS_SUFFIX"),
                        @CacheEvict(value = CacheName.DASHBOARD_HR,
                                    key = "T(com.itjob.util.CacheKeyGenerator).forHRDashboard(#id)")
                })
        public CompanyResponse updateCompany(UUID id, CompanyRequest request, UUID userId) {
            log.info("Updating company {}", id);
            Company company = companyRepository.findByIdAndIsDeleted(id, false)
                    .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
            
            // Check if user has permission to update this company
            if (!company.getCreatedBy().getId().equals(userId)) {
                throw new AppException(ErrorCode.FORBIDDEN);
            }
            
            // Save old name to check if slug needs update
            String oldName = company.getName();
            
            companyMapper.updateCompany(company, request);
            
            // Update slug if name changed
            if (request.getName() != null && !request.getName().equals(oldName)) {
                company.setSlug(generateUniqueSlug(request.getName(), id));
            }
            
            return mapToCompanyResponse(company);
        }
        
        @Override
        @Cacheable(value = CacheName.COMPANY_MY, key = "#userId")
        public CompanyResponse getMyCompany(UUID userId) {
            log.debug("Fetching my company for user {} from database", userId);
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
         * Generate a unique slug by appending a counter if the base slug already exists
         */
        private String generateUniqueSlug(String name) {
            return generateUniqueSlug(name, null);
        }
    
        private String generateUniqueSlug(String name, UUID excludeId) {
            String baseSlug = SlugUtil.generateSlug(name);
            String slug = baseSlug;
            int counter = 1;
            Optional<Company> existing = companyRepository.findBySlug(slug);
            while (existing.isPresent() && (excludeId == null || !existing.get().getId().equals(excludeId))) {
                slug = baseSlug + "-" + counter++;
                existing = companyRepository.findBySlug(slug);
            }
            return slug;
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
                    .toList();
            
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
                    .toList();
        }
        
        @Override
        public PageResponse<CompanyResponse> getAllCompanies(String status, Pageable pageable) {
            long start = System.currentTimeMillis();
            Page<Company> companyPage;
            
            if (status != null && !status.isEmpty()) {
                CompanyStatus companyStatus = CompanyStatus.fromValue(status);
                companyPage = companyRepository.findByStatusAndIsDeleted(companyStatus.getValue(), false, pageable);
            } else {
                companyPage = companyRepository.findByIsDeleted(false, pageable);
            }
            
            List<CompanyResponse> items = mapToCompanyResponsesWithJobCount(companyPage.getContent());
            
            PageResponse<CompanyResponse> result = PageResponseUtil.build(companyPage, items);
            log.debug("getAllCompanies completed in {} ms", System.currentTimeMillis() - start);
            return result;
        }
        
        @Override
        @DistributedLock(key = "'company:status:' + #id", leaseTime = 30)
        @Transactional
        @Caching(
                put = {
                        @CachePut(value = CacheName.COMPANY_BY_ID,
                                  key = "T(com.itjob.util.CacheKeyGenerator).forId(#result.id)")
                },
                evict = {
                        @CacheEvict(value = CacheName.COMPANY_BY_SLUG, allEntries = true),
                        @CacheEvict(value = CacheName.COMPANY_FEATURED, allEntries = true),
                        @CacheEvict(value = CacheName.COMPANY_SEARCH, allEntries = true),
                        @CacheEvict(value = CacheName.COMPANY_MY, allEntries = true),
                        @CacheEvict(value = CacheName.DASHBOARD_ADMIN,
                                    key = "T(com.itjob.util.CacheKeyGenerator).STATS_SUFFIX"),
                        @CacheEvict(value = CacheName.DASHBOARD_HR,
                                    key = "T(com.itjob.util.CacheKeyGenerator).forHRDashboard(#id)")
                })
        public CompanyResponse approveCompany(UUID id, UUID adminId) {
            Company company = companyRepository.findByIdAndIsDeleted(id, false)
                    .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
    
            if (!CompanyStatus.PENDING.getValue().equals(company.getStatus())) {
                throw new AppException(ErrorCode.COMPANY_ALREADY_PROCESSED);
            }
    
            company.setStatus(CompanyStatus.ACTIVE.getValue());
            company.setVerifiedAt(LocalDateTime.now());
    
            log.info("Company {} approved by admin {}", id, adminId);
            return mapToCompanyResponse(company);
        }
    
        @Override
        @DistributedLock(key = "'company:status:' + #id", leaseTime = 30)
        @Transactional
        @Caching(
                put = {
                        @CachePut(value = CacheName.COMPANY_BY_ID,
                                  key = "T(com.itjob.util.CacheKeyGenerator).forId(#result.id)")
                },
                evict = {
                        @CacheEvict(value = CacheName.COMPANY_BY_SLUG, allEntries = true),
                        @CacheEvict(value = CacheName.COMPANY_FEATURED, allEntries = true),
                        @CacheEvict(value = CacheName.COMPANY_SEARCH, allEntries = true),
                        @CacheEvict(value = CacheName.COMPANY_MY, allEntries = true),
                        @CacheEvict(value = CacheName.DASHBOARD_ADMIN,
                                    key = "T(com.itjob.util.CacheKeyGenerator).STATS_SUFFIX"),
                        @CacheEvict(value = CacheName.DASHBOARD_HR,
                                    key = "T(com.itjob.util.CacheKeyGenerator).forHRDashboard(#id)")
                })
        public CompanyResponse rejectCompany(UUID id, UUID adminId, String reason) {
            Company company = companyRepository.findByIdAndIsDeleted(id, false)
                    .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
    
            if (!CompanyStatus.PENDING.getValue().equals(company.getStatus())) {
                throw new AppException(ErrorCode.COMPANY_ALREADY_PROCESSED);
            }
    
            company.setStatus(CompanyStatus.REJECTED.getValue());
            company.setRejectReason(reason);
    
            log.info("Company {} rejected by admin {}: {}", id, adminId, reason);
            return mapToCompanyResponse(company);
        }
    
        @Override
        @DistributedLock(key = "'company:status:' + #id", leaseTime = 30)
        @Transactional
        @Caching(
                put = {
                        @CachePut(value = CacheName.COMPANY_BY_ID,
                                  key = "T(com.itjob.util.CacheKeyGenerator).forId(#result.id)")
                },
                evict = {
                        @CacheEvict(value = CacheName.COMPANY_BY_SLUG, allEntries = true),
                        @CacheEvict(value = CacheName.COMPANY_FEATURED, allEntries = true),
                        @CacheEvict(value = CacheName.COMPANY_SEARCH, allEntries = true),
                        @CacheEvict(value = CacheName.COMPANY_MY, allEntries = true),
                        @CacheEvict(value = CacheName.DASHBOARD_ADMIN,
                                    key = "T(com.itjob.util.CacheKeyGenerator).STATS_SUFFIX"),
                        @CacheEvict(value = CacheName.DASHBOARD_HR,
                                    key = "T(com.itjob.util.CacheKeyGenerator).forHRDashboard(#id)")
                })
        public CompanyResponse suspendCompany(UUID id, UUID adminId, String reason) {
            Company company = companyRepository.findByIdAndIsDeleted(id, false)
                    .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
    
            if (!CompanyStatus.ACTIVE.getValue().equals(company.getStatus())) {
                throw new AppException(ErrorCode.COMPANY_ALREADY_PROCESSED);
            }
    
            company.setStatus(CompanyStatus.SUSPENDED.getValue());
            company.setRejectReason(reason);
    
            log.info("Company {} suspended by admin {}: {}", id, adminId, reason);
            return mapToCompanyResponse(company);
        }
    }
