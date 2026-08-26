package com.itjob.service.impl;

import com.itjob.annotation.DistributedLock;
import com.itjob.redis.CacheName;
import com.itjob.enums.CompanyStatus;
import com.itjob.enums.JobStatus;
import com.itjob.dto.request.JobRequest;
import com.itjob.dto.response.JobResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.entity.Company;
import com.itjob.entity.Job;
import com.itjob.entity.Skill;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.mapper.JobMapper;
import com.itjob.repository.ApplicationRepository;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.JobRepository;
import com.itjob.repository.SkillRepository;
import com.itjob.repository.UserRepository;
import com.itjob.repository.projection.JobApplicationCountProjection;
import com.itjob.service.JobCacheService;
import com.itjob.enums.ViewEntity;
import com.itjob.service.JobService;
import com.itjob.service.RecentViewService;
import com.itjob.service.RecommendationService;
import com.itjob.service.SearchSuggestionService;
import com.itjob.service.TrendingJobService;
import com.itjob.service.ViewCountService;
import com.itjob.specification.helper.SpecificationHelper;
import com.itjob.util.PageResponseUtil;
import com.itjob.util.SlugUtil;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobServiceImpl implements JobService {

    private static final int MAX_FEATURED_LIMIT = 100;
    
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final SkillRepository skillRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final JobMapper jobMapper;
    private final SpecificationHelper specificationHelper;
    private final JobCacheService jobCacheService;
    private final ViewCountService viewCountService;
    private final TrendingJobService trendingJobService;
    private final RecentViewService recentViewService;
    private final RecommendationService recommendationService;
    private final SearchSuggestionService searchSuggestionService;
    
    @Override
    @Cacheable(value = CacheName.JOB_FEATURED,
               key = "T(com.itjob.util.CacheKeyGenerator).forLimit(#limit)")
    public List<JobResponse> getFeaturedJobs(int limit) {
        long start = System.currentTimeMillis();
        if (limit <= 0) {
            throw new AppException(ErrorCode.INVALID_LIMIT);
        }
        
        if (limit > MAX_FEATURED_LIMIT) {
            throw new AppException(ErrorCode.LIMIT_EXCEEDED);
        }
        
        log.debug("Fetching {} featured jobs from database", limit);
        List<JobResponse> result = fetchFeaturedJobs(limit);
        log.debug("getFeaturedJobs({}) completed in {} ms", limit, System.currentTimeMillis() - start);
        return result;
    }

    @Override
    public List<JobResponse> getTrendingJobs(int limit) {
        log.debug("Fetching {} trending jobs from Redis", limit);
        List<UUID> topIds = trendingJobService.getTopJobIds(limit);

        if (topIds.isEmpty()) {
            log.debug("No trending jobs in Redis, falling back to featured jobs");
            return fetchFeaturedJobs(limit);
        }

        List<JobResponse> result = fetchJobResponsesByIdOrder(topIds);

        log.debug("getTrendingJobs({}) returned {} jobs", limit, result.size());
        return result;
    }
    
    @Override
    @Cacheable(value = CacheName.JOB_SEARCH,
               key = "T(com.itjob.util.CacheKeyGenerator).forComposite(" +
                     "T(java.util.Map).of(" +
                     "'filters', T(com.itjob.util.CacheKeyGenerator).forFilters(#filters), " +
                     "'page', #pageable.pageNumber, " +
                     "'size', #pageable.pageSize, " +
                     "'sort', #pageable.sort.isSorted() ? #pageable.sort.toString() : ''))")
    public PageResponse<JobResponse> searchJobs(String[] filters, Pageable pageable) {
        long start = System.currentTimeMillis();
        log.debug("Fetching jobs with filters from database");
        
        Specification<Job> spec = specificationHelper.buildSpecification(filters);
        
        if (spec == null) {
            spec = (root, query, cb) -> cb.equal(root.get("status"), JobStatus.OPEN.getValue());
        }
        
        Page<Job> jobPage = jobRepository.findAll(spec, pageable);
        
        List<JobResponse> items = jobPage.getContent().stream()
                .map(jobMapper::toJobResponse)
                .toList();
        
        PageResponse<JobResponse> result = buildJobPageResponse(jobPage, items);
        log.debug("searchJobs completed in {} ms", System.currentTimeMillis() - start);
        return result;
    }
    
    @Override
    public JobResponse getJobById(UUID id, UUID currentUserId) {
        // Get base job data from cache (using separate service to avoid proxy bypass)
        JobResponse response = jobCacheService.getCachedJobById(id);
        
        // Add user-specific fields dynamically (NOT cached)
        if (currentUserId != null) {
            boolean isApplied = applicationRepository.existsByJobIdAndUserId(id, currentUserId);
            response.setIsApplied(isApplied);
        }

        viewCountService.incrementView(ViewEntity.JOB, id, currentUserId != null ? currentUserId.toString() : null);

        if (currentUserId != null) {
            recentViewService.recordView(currentUserId, id);
        }

        long pendingViews = viewCountService.getPendingViewDelta(ViewEntity.JOB, id);
        if (pendingViews > 0) {
            Integer current = response.getViewCount();
            response.setViewCount((current == null ? 0 : current) + (int) pendingViews);
        }

        return response;
    }

    @Override
    public List<JobResponse> getRecentlyViewedJobs(UUID userId, int limit) {
        log.debug("Fetching {} recently viewed jobs for user {}", limit, userId);
        List<UUID> ids = recentViewService.getRecentViewIds(userId, limit);
        List<JobResponse> result = fetchJobResponsesByIdOrder(ids);
        log.debug("getRecentlyViewedJobs({}) returned {} jobs", limit, result.size());
        return result;
    }

    @Override
    public List<JobResponse> getRecommendedJobs(UUID userId, int limit) {
        log.debug("Fetching {} recommended jobs for user {}", limit, userId);
        List<UUID> ids = recommendationService.getRecommendedJobs(userId, limit);
        List<JobResponse> result = fetchJobResponsesByIdOrder(ids);
        log.debug("getRecommendedJobs({}) returned {} jobs", limit, result.size());
        return result;
    }
    
    @Override
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    @Cacheable(value = CacheName.JOB_BY_COMPANY,
               key = "T(com.itjob.util.CacheKeyGenerator).forComposite(" +
                     "T(java.util.Map).of(" +
                     "'companyId', #companyId, " +
                     "'status', #status != null ? #status : 'all', " +
                     "'page', #pageable.pageNumber, " +
                     "'size', #pageable.pageSize, " +
                     "'sort', #pageable.sort.isSorted() ? #pageable.sort.toString() : ''))")
    public PageResponse<JobResponse> getCompanyJobs(UUID companyId, String status, Pageable pageable) {
        long start = System.currentTimeMillis();
        log.debug("Fetching jobs for company {} from database", companyId);
        Page<Job> jobPage = findJobsByCompanyAndStatus(companyId, status, pageable);
        
        List<Job> jobs = jobPage.getContent();
        
        if (jobs.isEmpty()) {
            PageResponse<JobResponse> result = buildJobPageResponse(jobPage, List.of());
            log.debug("getCompanyJobs({}) completed in {} ms", companyId, System.currentTimeMillis() - start);
            return result;
        }
        
        List<UUID> jobIds = jobs.stream()
                .map(Job::getId)
                .toList();
        
        Map<UUID, Long> applicationCountMap = applicationRepository.countApplicationsByJobIds(jobIds)
                .stream()
                .collect(Collectors.toMap(
                        JobApplicationCountProjection::getJobId,
                        JobApplicationCountProjection::getApplicationCount
                ));
        
        List<JobResponse> items = jobs.stream()
                .map(job -> {
                    JobResponse response = jobMapper.toJobResponseWithCompanyOnly(job);
                    Long count = applicationCountMap.getOrDefault(job.getId(), 0L);
                    response.setApplicationCount(count.intValue());
                    return response;
                })
                .toList();
        
        PageResponse<JobResponse> result = buildJobPageResponse(jobPage, items);
        log.debug("getCompanyJobs({}) completed in {} ms", companyId, System.currentTimeMillis() - start);
        return result;
    }
    
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    @Caching(evict = {
            @CacheEvict(value = CacheName.JOB_FEATURED, allEntries = true),
            @CacheEvict(value = CacheName.JOB_SEARCH, allEntries = true),
            @CacheEvict(value = CacheName.JOB_BY_COMPANY, allEntries = true),
            @CacheEvict(value = CacheName.COMPANY_BY_ID, key = "T(com.itjob.util.CacheKeyGenerator).forId(#companyId)"),
            @CacheEvict(value = CacheName.DASHBOARD_ADMIN,
                        key = "T(com.itjob.util.CacheKeyGenerator).STATS_SUFFIX"),
            @CacheEvict(value = CacheName.DASHBOARD_HR,
                        key = "T(com.itjob.util.CacheKeyGenerator).forHRDashboard(#companyId)")
    })
    public JobResponse createJob(UUID companyId, JobRequest request, UUID userId) {
        log.info("Creating job for company {}", companyId);
        Company company = companyRepository.findByIdAndIsDeleted(companyId, false)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        
        // Check if company is active
        if (!CompanyStatus.ACTIVE.getValue().equals(company.getStatus())) {
            throw new AppException(ErrorCode.COMPANY_NOT_ACTIVE);
        }
        
        // Check if company belongs to user (authorization)
        if (!company.getCreatedBy().getId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        
        User user = userRepository.getReferenceById(userId);
        
        Job job = jobMapper.toJob(request);
        job.setCompany(company);
        job.setCreatedBy(user);
        job.setUpdatedBy(user);
        
        job.setSlug(generateUniqueJobSlug(request.getTitle()));
        
        // Set skills
        setJobSkills(job, request.getSkillIds());
        
        job = jobRepository.save(job);
        
        searchSuggestionService.recordKeyword(job.getTitle());
        
        // Use mapper
        return jobMapper.toJobResponse(job);
    }
    
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    @Caching(
            put = {
                    @CachePut(value = CacheName.JOB_DETAIL,
                              key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)")
            },
            evict = {
                    @CacheEvict(value = CacheName.JOB_FEATURED, allEntries = true),
                    @CacheEvict(value = CacheName.JOB_SEARCH, allEntries = true),
                    @CacheEvict(value = CacheName.JOB_BY_COMPANY, allEntries = true),
                    @CacheEvict(value = CacheName.COMPANY_BY_ID, key = "T(com.itjob.util.CacheKeyGenerator).forId(#companyId)"),
                    @CacheEvict(value = CacheName.DASHBOARD_ADMIN,
                                key = "T(com.itjob.util.CacheKeyGenerator).STATS_SUFFIX"),
                    @CacheEvict(value = CacheName.DASHBOARD_HR,
                                key = "T(com.itjob.util.CacheKeyGenerator).forHRDashboard(#companyId)")
            })
    public JobResponse updateJob(UUID id, UUID companyId, JobRequest request, UUID userId) {
        log.info("Updating job {}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        
        verifyJobBelongsToCompany(job, companyId);
        
        // Get company from job (already loaded via @EntityGraph) instead of querying again
        Company company = job.getCompany();
        
        // Verify company is not deleted
        if (Boolean.TRUE.equals(company.getIsDeleted())) {
            throw new AppException(ErrorCode.COMPANY_NOT_FOUND);
        }
        
        // Verify company belongs to user (authorization)
        if (!company.getCreatedBy().getId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        
        User user = userRepository.getReferenceById(userId);
        
        String oldTitle = job.getTitle();
        
        jobMapper.updateJob(job, request);
        job.setUpdatedBy(user);
        
        if (request.getTitle() != null && !request.getTitle().equals(oldTitle)) {
            job.setSlug(generateUniqueJobSlug(request.getTitle(), id));
        }
        
        // Update skills
        if (request.getSkillIds() != null) {
            setJobSkills(job, request.getSkillIds());
        }
        
        job = jobRepository.save(job);
        
        searchSuggestionService.recordKeyword(job.getTitle());
        
        // Use mapper
        return jobMapper.toJobResponse(job);
    }
    
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    @Caching(evict = {
            @CacheEvict(value = CacheName.JOB_DETAIL, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)"),
            @CacheEvict(value = CacheName.JOB_FEATURED, allEntries = true),
            @CacheEvict(value = CacheName.JOB_SEARCH, allEntries = true),
            @CacheEvict(value = CacheName.JOB_BY_COMPANY, allEntries = true),
            @CacheEvict(value = CacheName.DASHBOARD_ADMIN,
                        key = "T(com.itjob.util.CacheKeyGenerator).STATS_SUFFIX"),
            @CacheEvict(value = CacheName.DASHBOARD_HR,
                        key = "T(com.itjob.util.CacheKeyGenerator).forHRDashboard(#companyId)")
    })
    public void deleteJob(UUID id, UUID companyId, UUID userId) {
        log.info("Deleting job {}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        
        verifyJobBelongsToCompany(job, companyId);
        
        // Get company from job (already loaded) and verify ownership
        Company company = job.getCompany();
        if (!company.getCreatedBy().getId().equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        
        // Soft delete by setting status to closed (Hibernate dirty checking will auto-save)
        job.setStatus(JobStatus.CLOSED.getValue());
    }
    
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<JobResponse> getAllJobs(String status, Pageable pageable) {
        Page<Job> jobPage;
        
        if (status != null && !status.isEmpty()) {
            // Validate and convert status string to enum
            JobStatus jobStatus = JobStatus.fromValue(status);
            jobPage = jobRepository.findByStatus(jobStatus.getValue(), pageable);
        } else {
            // TODO: Consider filtering out soft-deleted jobs if needed
            jobPage = jobRepository.findAll(pageable);
        }
        
        // Use mapper without skills for admin listings
        List<JobResponse> items = jobPage.getContent().stream()
                .map(jobMapper::toJobResponseWithCompanyOnly)
                .toList();
        
        return buildJobPageResponse(jobPage, items);
    }
    
    @Override
    @DistributedLock(key = "'job:state:' + #id", leaseTime = 30)
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @Caching(evict = {
            @CacheEvict(value = CacheName.JOB_DETAIL, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)"),
            @CacheEvict(value = CacheName.JOB_FEATURED, allEntries = true),
            @CacheEvict(value = CacheName.JOB_SEARCH, allEntries = true),
            @CacheEvict(value = CacheName.JOB_BY_COMPANY, allEntries = true),
            @CacheEvict(value = CacheName.DASHBOARD_ADMIN,
                        key = "T(com.itjob.util.CacheKeyGenerator).STATS_SUFFIX")
    })
    public void approveJob(UUID id, UUID adminId) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));

        if (!JobStatus.DRAFT.getValue().equals(job.getStatus())) {
            throw new AppException(ErrorCode.JOB_ALREADY_PROCESSED);
        }

        job.setStatus(JobStatus.OPEN.getValue());

        log.info("Job {} approved by admin {}", id, adminId);
    }

    @Override
    @DistributedLock(key = "'job:state:' + #id", leaseTime = 30)
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @Caching(evict = {
            @CacheEvict(value = CacheName.JOB_DETAIL, key = "T(com.itjob.util.CacheKeyGenerator).forId(#id)"),
            @CacheEvict(value = CacheName.JOB_FEATURED, allEntries = true),
            @CacheEvict(value = CacheName.JOB_SEARCH, allEntries = true),
            @CacheEvict(value = CacheName.JOB_BY_COMPANY, allEntries = true),
            @CacheEvict(value = CacheName.DASHBOARD_ADMIN,
                        key = "T(com.itjob.util.CacheKeyGenerator).STATS_SUFFIX")
    })
    public void rejectJob(UUID id, UUID adminId, String reason) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));

        if (!JobStatus.DRAFT.getValue().equals(job.getStatus())) {
            throw new AppException(ErrorCode.JOB_ALREADY_PROCESSED);
        }

        job.setStatus(JobStatus.REJECTED.getValue());

        log.info("Job {} rejected by admin {}: {}", id, adminId, reason);
    }
    
    private List<JobResponse> fetchFeaturedJobs(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return jobRepository.findFeaturedJobs(JobStatus.OPEN.getValue(), pageable).stream()
                .map(jobMapper::toJobResponse)
                .toList();
    }

    private List<JobResponse> fetchJobResponsesByIdOrder(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Job> jobs = jobRepository.findAllById(ids);
        Map<UUID, Job> jobMap = new HashMap<>();
        for (Job job : jobs) {
            jobMap.put(job.getId(), job);
        }
        return ids.stream()
                .map(jobMap::get)
                .filter(java.util.Objects::nonNull)
                .map(jobMapper::toJobResponse)
                .toList();
    }

    /**
     * Build PageResponse for Job listings
     */
    private PageResponse<JobResponse> buildJobPageResponse(Page<Job> jobPage, List<JobResponse> items) {
        return PageResponseUtil.build(jobPage, items);
    }
    
    /**
     * Find jobs by company and optional status filter
     */
    private Page<Job> findJobsByCompanyAndStatus(UUID companyId, String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            // Validate and convert status to enum
            JobStatus jobStatus = JobStatus.fromValue(status);
            return jobRepository.findByCompanyIdAndStatus(companyId, jobStatus.getValue(), pageable);
        }
        return jobRepository.findByCompanyId(companyId, pageable);
    }
    
    /**
     * Set skills for a job from skill IDs
     * Validates that all skill IDs exist
     */
    private void setJobSkills(Job job, Set<UUID> skillIds) {
        if (skillIds != null && !skillIds.isEmpty()) {
            Set<Skill> skills = new HashSet<>(skillRepository.findAllById(skillIds));
            
            // Validate all skill IDs exist
            if (skills.size() != skillIds.size()) {
                throw new AppException(ErrorCode.SKILL_NOT_FOUND);
            }
            
            job.setSkills(skills);
        }
    }
    
    /**
     * Verify that job belongs to the specified company
     */
    private void verifyJobBelongsToCompany(Job job, UUID companyId) {
        if (!job.getCompany().getId().equals(companyId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private String generateUniqueJobSlug(String title) {

        return generateUniqueJobSlug(title, null);
    }

    private String generateUniqueJobSlug(String title, UUID excludeId) {
        String baseSlug = SlugUtil.generateSlug(title);
        String slug = baseSlug;
        int counter = 1;
        Optional<Job> existing = jobRepository.findBySlug(slug);
        while (existing.isPresent()) {
            if (excludeId != null && existing.get().getId().equals(excludeId)) {
                break;
            }
            slug = baseSlug + "-" + counter++;
            existing = jobRepository.findBySlug(slug);
        }
        return slug;
    }
}

