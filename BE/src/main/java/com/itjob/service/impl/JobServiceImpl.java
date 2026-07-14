package com.itjob.service.impl;

import com.itjob.constant.CacheName;
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
import com.itjob.service.JobService;
import com.itjob.specification.helper.SpecificationHelper;
import com.itjob.util.PageResponseUtil;
import com.itjob.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    
    @Override
    @Cacheable(value = CacheName.JOB_FEATURED,
               key = "T(com.itjob.util.CacheKeyGenerator).forLimit(#limit)")
    public List<JobResponse> getFeaturedJobs(int limit) {
        if (limit <= 0) {
            throw new AppException(ErrorCode.INVALID_LIMIT);
        }
        
        // Reject if limit exceeds maximum instead of silently capping
        if (limit > MAX_FEATURED_LIMIT) {
            throw new AppException(ErrorCode.LIMIT_EXCEEDED);
        }
        
        log.debug("Fetching {} featured jobs from database", limit);
        Pageable pageable = PageRequest.of(0, limit);
        List<Job> jobs = jobRepository.findFeaturedJobs(pageable);
        
        // Use mapper - @EntityGraph already fetches company and skills
        return jobs.stream()
                .map(jobMapper::toJobResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Cacheable(value = CacheName.JOB_SEARCH,
               key = "T(com.itjob.util.CacheKeyGenerator).forComposite(" +
                     "T(java.util.Map).of(" +
                     "'filters', #filters != null ? T(java.util.Arrays).toString(#filters) : 'none', " +
                     "'page', #pageable.pageNumber, " +
                     "'size', #pageable.pageSize))")
    public PageResponse<JobResponse> searchJobs(String[] filters, Pageable pageable) {
        log.debug("Fetching jobs with filters from database");
        
        // Build specification from filters
        Specification<Job> spec = specificationHelper.buildSpecification(filters);
        
        // If no filters provided, default to showing only open jobs
        if (spec == null) {
            spec = (root, query, cb) -> cb.equal(root.get("status"), JobStatus.OPEN.getValue());
        }
        
        Page<Job> jobPage = jobRepository.findAll(spec, pageable);
        
        // Use mapper
        List<JobResponse> items = jobPage.getContent().stream()
                .map(jobMapper::toJobResponse)
                .collect(Collectors.toList());
        
        return buildJobPageResponse(jobPage, items);
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
        
        // TODO: Implement view count tracking with RedisTemplate + async + scheduled DB sync
        
        return response;
    }
    
    @Override
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    @Cacheable(value = CacheName.JOB_BY_COMPANY,
               key = "T(com.itjob.util.CacheKeyGenerator).forComposite(" +
                     "T(java.util.Map).of(" +
                     "'companyId', #companyId, " +
                     "'status', #status != null ? #status : 'all', " +
                     "'page', #pageable.pageNumber, " +
                     "'size', #pageable.pageSize))")
    public PageResponse<JobResponse> getCompanyJobs(UUID companyId, String status, Pageable pageable) {
        log.debug("Fetching jobs for company {} from database", companyId);
        Page<Job> jobPage = findJobsByCompanyAndStatus(companyId, status, pageable);
        
        List<Job> jobs = jobPage.getContent();
        
        if (jobs.isEmpty()) {
            return buildJobPageResponse(jobPage, List.of());
        }
        
        // Batch query application counts to avoid N+1
        List<UUID> jobIds = jobs.stream()
                .map(Job::getId)
                .collect(Collectors.toList());
        
        Map<UUID, Long> applicationCountMap = applicationRepository.countApplicationsByJobIds(jobIds)
                .stream()
                .collect(Collectors.toMap(
                        JobApplicationCountProjection::getJobId,
                        JobApplicationCountProjection::getApplicationCount
                ));
        
        // Map jobs to responses with application counts
        List<JobResponse> items = jobs.stream()
                .map(job -> {
                    JobResponse response = jobMapper.toJobResponseWithCompanyOnly(job);
                    Long count = applicationCountMap.getOrDefault(job.getId(), 0L);
                    response.setApplicationCount(count.intValue());
                    return response;
                })
                .collect(Collectors.toList());
        
        return buildJobPageResponse(jobPage, items);
    }
    
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    @Caching(evict = {
            @CacheEvict(value = CacheName.JOB_FEATURED, allEntries = true),
            @CacheEvict(value = CacheName.JOB_SEARCH, allEntries = true),
            @CacheEvict(value = CacheName.JOB_BY_COMPANY, allEntries = true),
            @CacheEvict(value = CacheName.COMPANY_BY_ID, key = "T(com.itjob.util.CacheKeyGenerator).forId(#companyId)"),
            @CacheEvict(value = CacheName.DASHBOARD_ADMIN, allEntries = true),
            @CacheEvict(value = CacheName.DASHBOARD_HR, allEntries = true)
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
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Job job = jobMapper.toJob(request);
        job.setCompany(company);
        job.setCreatedBy(user);
        job.setUpdatedBy(user);
        
        // Generate slug from title
        job.setSlug(SlugUtil.generateSlug(request.getTitle()));
        
        // Set skills
        setJobSkills(job, request.getSkillIds());
        
        job = jobRepository.save(job);
        
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
            @CacheEvict(value = CacheName.COMPANY_BY_ID, key = "T(com.itjob.util.CacheKeyGenerator).forId(#companyId)"),
            @CacheEvict(value = CacheName.DASHBOARD_ADMIN, allEntries = true),
            @CacheEvict(value = CacheName.DASHBOARD_HR, allEntries = true)
    })
    public JobResponse updateJob(UUID id, UUID companyId, JobRequest request, UUID userId) {
        log.info("Updating job {}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        
        verifyJobBelongsToCompany(job, companyId);
        
        // Get company from job (already loaded via @EntityGraph) instead of querying again
        Company company = job.getCompany();
        
        // Verify company is not deleted
        if (company.getIsDeleted()) {
            throw new AppException(ErrorCode.COMPANY_NOT_FOUND);
        }
        
        // Verify company belongs to user (authorization)
        if (!company.getCreatedBy().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Save old title to check if slug needs update (use Objects.equals to handle null)
        String oldTitle = job.getTitle();
        
        jobMapper.updateJob(job, request);
        job.setUpdatedBy(user);
        
        // Update slug if title changed
        if (request.getTitle() != null && !request.getTitle().equals(oldTitle)) {
            job.setSlug(SlugUtil.generateSlug(request.getTitle()));
        }
        
        // Update skills
        if (request.getSkillIds() != null) {
            setJobSkills(job, request.getSkillIds());
        }
        
        job = jobRepository.save(job);
        
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
            @CacheEvict(value = CacheName.DASHBOARD_ADMIN, allEntries = true),
            @CacheEvict(value = CacheName.DASHBOARD_HR, allEntries = true)
    })
    public void deleteJob(UUID id, UUID companyId, UUID userId) {
        log.info("Deleting job {}", id);
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        
        verifyJobBelongsToCompany(job, companyId);
        
        // Get company from job (already loaded) and verify ownership
        Company company = job.getCompany();
        if (!company.getCreatedBy().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
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
                .collect(Collectors.toList());
        
        return buildJobPageResponse(jobPage, items);
    }
    
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void approveJob(UUID id, UUID adminId) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        
        job.setStatus(JobStatus.OPEN.getValue());
    }
    
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void rejectJob(UUID id, UUID adminId, String reason) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        
        job.setStatus(JobStatus.REJECTED.getValue());
        
        log.info("Job {} rejected by admin {}: {}", id, adminId, reason);
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
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
}

