package com.itjob.service.impl;

import com.itjob.dto.request.JobRequest;
import com.itjob.dto.response.JobResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.entity.Company;
import com.itjob.entity.Job;
import com.itjob.entity.Skill;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.mapper.CompanyMapper;
import com.itjob.mapper.JobMapper;
import com.itjob.mapper.SkillMapper;
import com.itjob.repository.ApplicationRepository;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.JobRepository;
import com.itjob.repository.SkillRepository;
import com.itjob.repository.UserRepository;
import com.itjob.service.JobService;
import com.itjob.specification.helper.SpecificationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobServiceImpl implements JobService {
    
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final SkillRepository skillRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final JobMapper jobMapper;
    private final CompanyMapper companyMapper;
    private final SkillMapper skillMapper;
    private final SpecificationHelper specificationHelper;
    
    @Override
    public List<JobResponse> getFeaturedJobs(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Job> jobs = jobRepository.findFeaturedJobs(pageable);
        
        return jobs.stream()
                .map(this::buildJobResponseWithCompanyAndSkills)
                .collect(Collectors.toList());
    }
    
    @Override
    public PageResponse<JobResponse> searchJobs(String[] filters, Pageable pageable) {
        log.info("=== JOB SEARCH DEBUG ===");
        log.info("Filters received: {}", filters != null ? Arrays.toString(filters) : "null");
        
        // Build specification from filters
        Specification<Job> spec = specificationHelper.buildSpecification(filters);
        
        // If no filters provided, default to showing only open jobs
        if (spec == null) {
            log.info("No filters, using default specification (status=open)");
            spec = (root, query, cb) -> cb.equal(root.get("status"), "open");
        }
        
        log.info("Specification built: {}", "NOT NULL");
        log.info("=== JOB SEARCH DEBUG END ===");
        
        Page<Job> jobPage = jobRepository.findAll(spec, pageable);
        
        List<JobResponse> items = jobPage.getContent().stream()
                .map(this::buildJobResponseWithCompanyAndSkills)
                .collect(Collectors.toList());
        
        return buildJobPageResponse(jobPage, items);
    }
    
    @Override
    public JobResponse getJobById(UUID id, UUID currentUserId) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        
        JobResponse response = buildJobResponseWithCompanyAndSkills(job);
        
        // Check if user already applied
        if (currentUserId != null) {
            boolean isApplied = applicationRepository.existsByJobIdAndUserId(id, currentUserId);
            response.setIsApplied(isApplied);
        }
        
        // Increment view count
        job.setViewCount(job.getViewCount() + 1);
        jobRepository.save(job);
        
        return response;
    }
    
    @Override
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public PageResponse<JobResponse> getCompanyJobs(UUID companyId, String status, Pageable pageable) {
        Page<Job> jobPage = findJobsByCompanyAndStatus(companyId, status, pageable);
        
        List<JobResponse> items = jobPage.getContent().stream()
                .map(job -> {
                    JobResponse response = buildJobResponseWithCompany(job);
                    long applicationCount = applicationRepository.countByJobId(job.getId());
                    response.setApplicationCount((int) applicationCount);
                    return response;
                })
                .collect(Collectors.toList());
        
        return buildJobPageResponse(jobPage, items);
    }
    
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public JobResponse createJob(UUID companyId, JobRequest request, UUID userId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Job job = jobMapper.toJob(request);
        job.setCompany(company);
        job.setCreatedBy(user);
        job.setUpdatedBy(user);
        
        // Generate slug from title
        job.setSlug(generateSlug(request.getTitle()));
        
        // Set skills
        setJobSkills(job, request.getSkillIds());
        
        job = jobRepository.save(job);
        
        return buildJobResponseWithCompanyAndSkills(job);
    }
    
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public JobResponse updateJob(UUID id, UUID companyId, JobRequest request, UUID userId) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        
        verifyJobBelongsToCompany(job, companyId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        jobMapper.updateJob(job, request);
        job.setUpdatedBy(user);
        
        // Update slug if title changed
        if (request.getTitle() != null && !request.getTitle().equals(job.getTitle())) {
            job.setSlug(generateSlug(request.getTitle()));
        }
        
        // Update skills
        if (request.getSkillIds() != null) {
            setJobSkills(job, request.getSkillIds());
        }
        
        job = jobRepository.save(job);
        
        return buildJobResponseWithCompanyAndSkills(job);
    }
    
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public void deleteJob(UUID id, UUID companyId) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        
        verifyJobBelongsToCompany(job, companyId);
        
        // Soft delete by setting status to closed
        job.setStatus("closed");
        jobRepository.save(job);
    }
    
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<JobResponse> getAllJobs(String status, Pageable pageable) {
        Page<Job> jobPage = jobRepository.findAll(pageable);
        
        List<JobResponse> items = jobPage.getContent().stream()
                .map(this::buildJobResponseWithCompany)
                .collect(Collectors.toList());
        
        return buildJobPageResponse(jobPage, items);
    }
    
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void approveJob(UUID id, UUID adminId) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        
        job.setStatus("open");
        jobRepository.save(job);
    }
    
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void rejectJob(UUID id, UUID adminId, String reason) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        
        job.setStatus("rejected");
        jobRepository.save(job);
        
        log.info("Job {} rejected by admin {}: {}", id, adminId, reason);
    }
    
    /**
     * Build JobResponse with Company and Skills
     */
    private JobResponse buildJobResponseWithCompanyAndSkills(Job job) {
        JobResponse response = jobMapper.toJobResponse(job);
        response.setCompany(companyMapper.toCompanyBriefResponse(job.getCompany()));
        
        if (job.getSkills() != null) {
            response.setSkills(job.getSkills().stream()
                    .map(skillMapper::toSkillResponse)
                    .collect(Collectors.toSet()));
        }
        
        return response;
    }
    
    /**
     * Build JobResponse with Company only (no skills)
     */
    private JobResponse buildJobResponseWithCompany(Job job) {
        JobResponse response = jobMapper.toJobResponse(job);
        response.setCompany(companyMapper.toCompanyBriefResponse(job.getCompany()));
        return response;
    }
    
    /**
     * Build PageResponse for Job listings
     */
    private PageResponse<JobResponse> buildJobPageResponse(Page<Job> jobPage, List<JobResponse> items) {
        return PageResponse.<JobResponse>builder()
                .items(items)
                .page(jobPage.getNumber())
                .size(jobPage.getSize())
                .totalElements(jobPage.getTotalElements())
                .totalPages(jobPage.getTotalPages())
                .build();
    }
    
    /**
     * Find jobs by company and optional status filter
     */
    private Page<Job> findJobsByCompanyAndStatus(UUID companyId, String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return jobRepository.findByCompanyIdAndStatus(companyId, status, pageable);
        }
        return jobRepository.findByCompanyId(companyId, pageable);
    }
    
    /**
     * Set skills for a job from skill IDs
     */
    private void setJobSkills(Job job, Set<UUID> skillIds) {
        if (skillIds != null && !skillIds.isEmpty()) {
            Set<Skill> skills = new HashSet<>(skillRepository.findAllById(skillIds));
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
    
    /**
     * Generate URL-friendly slug from job title
     */
    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
