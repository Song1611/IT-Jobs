package com.itjob.service.impl;

import com.itjob.dto.request.ApplicationRequest;
import com.itjob.dto.response.ApplicationResponse;
import com.itjob.dto.response.PageResponse;
import com.itjob.entity.Application;
import com.itjob.entity.Job;
import com.itjob.entity.User;
import com.itjob.exception.AppException;
import com.itjob.exception.ErrorCode;
import com.itjob.mapper.ApplicationMapper;
import com.itjob.mapper.CompanyMapper;
import com.itjob.mapper.JobMapper;
import com.itjob.mapper.UserMapper;
import com.itjob.repository.ApplicationRepository;
import com.itjob.repository.JobRepository;
import com.itjob.repository.UserRepository;
import com.itjob.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {
    
    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ApplicationMapper applicationMapper;
    private final JobMapper jobMapper;
    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApplicationResponse applyForJob(ApplicationRequest request, UUID userId) {
        // Check if job exists and is open
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        
        if (!"open".equals(job.getStatus())) {
            throw new AppException(ErrorCode.JOB_NOT_OPEN);
        }
        
        // Check if user already applied
        if (applicationRepository.existsByJobIdAndUserId(request.getJobId(), userId)) {
            throw new AppException(ErrorCode.ALREADY_APPLIED);
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Application application = applicationMapper.toApplication(request);
        application.setJob(job);
        application.setUser(user);
        application.setStatus("pending");
        
        application = applicationRepository.save(application);
        
        // Update job application count
        job.setApplicationCount(job.getApplicationCount() + 1);
        jobRepository.save(job);
        
        return buildFullApplicationResponse(application);
    }
    
    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public PageResponse<ApplicationResponse> getMyApplications(UUID userId, Pageable pageable) {
        Page<Application> applicationPage = applicationRepository.findByUserIdOrderByAppliedAtDesc(userId, pageable);
        
        List<ApplicationResponse> items = applicationPage.getContent().stream()
                .map(this::buildApplicationResponseWithJobAndCompany)
                .collect(Collectors.toList());
        
        return buildPageResponse(applicationPage, items);
    }
    
    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApplicationResponse getApplicationById(UUID id, UUID userId) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));
        
        // Check if user has permission to view this application
        if (!application.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        return buildFullApplicationResponse(application);
    }
    
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public void withdrawApplication(UUID id, UUID userId) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));
        
        if (!application.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        if (!"pending".equals(application.getStatus())) {
            throw new AppException(ErrorCode.CANNOT_WITHDRAW_APPLICATION);
        }
        
        application.setStatus("withdrawn");
        applicationRepository.save(application);
        
        // Update job application count
        Job job = application.getJob();
        job.setApplicationCount(Math.max(0, job.getApplicationCount() - 1));
        jobRepository.save(job);
    }
    
    @Override
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public PageResponse<ApplicationResponse> getJobApplications(UUID jobId, UUID companyId, String status, Pageable pageable) {
        // Verify job belongs to company
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
        
        if (!job.getCompany().getId().equals(companyId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        Page<Application> applicationPage;
        
        if (status != null && !status.isEmpty()) {
            applicationPage = applicationRepository.findByJobIdAndStatusOrderByAppliedAtDesc(jobId, status, pageable);
        } else {
            applicationPage = applicationRepository.findByJobIdOrderByAppliedAtDesc(jobId, pageable);
        }
        
        List<ApplicationResponse> items = applicationPage.getContent().stream()
                .map(this::buildApplicationResponseWithJobAndCandidate)
                .collect(Collectors.toList());
        
        return buildPageResponse(applicationPage, items);
    }
    
    @Override
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public PageResponse<ApplicationResponse> getCompanyApplications(UUID companyId, Pageable pageable) {
        Page<Application> applicationPage = applicationRepository.findByCompanyId(companyId, pageable);
        
        List<ApplicationResponse> items = applicationPage.getContent().stream()
                .map(this::buildApplicationResponseWithJobAndCandidate)
                .collect(Collectors.toList());
        
        return buildPageResponse(applicationPage, items);
    }
    
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public ApplicationResponse updateApplicationStatus(UUID id, UUID companyId, String status, String notes) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));
        
        // Verify application belongs to company
        verifyApplicationBelongsToCompany(application, companyId);
        
        application.setStatus(status);
        application.setHrNotes(notes);
        application.setRespondedAt(LocalDateTime.now());
        
        if ("reviewing".equals(status)) {
            application.setReviewedAt(LocalDateTime.now());
        } else if ("interview".equals(status)) {
            application.setInterviewAt(LocalDateTime.now());
        } else if ("rejected".equals(status)) {
            application.setRejectionReason(notes);
        }
        
        application = applicationRepository.save(application);
        
        return buildApplicationResponseWithJobAndCandidate(application);
    }
    
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('EMPLOYER', 'ADMIN')")
    public void markAsViewed(UUID id, UUID companyId) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));
        
        verifyApplicationBelongsToCompany(application, companyId);
        
        if (!application.getViewedByEmployer()) {
            application.setViewedByEmployer(true);
            application.setViewedAt(LocalDateTime.now());
            applicationRepository.save(application);
        }
    }
    
    /**
     * Build full ApplicationResponse with Job, Company, and Candidate info
     */
    private ApplicationResponse buildFullApplicationResponse(Application application) {
        ApplicationResponse response = applicationMapper.toApplicationResponse(application);
        response.setJob(jobMapper.toJobBriefResponse(application.getJob()));
        response.getJob().setCompany(companyMapper.toCompanyBriefResponse(application.getJob().getCompany()));
        response.setCandidate(userMapper.toUserBriefResponse(application.getUser()));
        return response;
    }
    
    /**
     * Build ApplicationResponse with Job and Company info (for candidate view)
     */
    private ApplicationResponse buildApplicationResponseWithJobAndCompany(Application application) {
        ApplicationResponse response = applicationMapper.toApplicationResponse(application);
        response.setJob(jobMapper.toJobBriefResponse(application.getJob()));
        response.getJob().setCompany(companyMapper.toCompanyBriefResponse(application.getJob().getCompany()));
        return response;
    }
    
    /**
     * Build ApplicationResponse with Job and Candidate info (for employer view)
     */
    private ApplicationResponse buildApplicationResponseWithJobAndCandidate(Application application) {
        ApplicationResponse response = applicationMapper.toApplicationResponse(application);
        response.setJob(jobMapper.toJobBriefResponse(application.getJob()));
        response.setCandidate(userMapper.toUserBriefResponse(application.getUser()));
        return response;
    }
    
    /**
     * Build PageResponse from Page and items list
     */
    private PageResponse<ApplicationResponse> buildPageResponse(Page<Application> applicationPage, List<ApplicationResponse> items) {
        return PageResponse.<ApplicationResponse>builder()
                .items(items)
                .page(applicationPage.getNumber())
                .size(applicationPage.getSize())
                .totalElements(applicationPage.getTotalElements())
                .totalPages(applicationPage.getTotalPages())
                .build();
    }
    
    /**
     * Verify that application belongs to the specified company
     */
    private void verifyApplicationBelongsToCompany(Application application, UUID companyId) {
        if (!application.getJob().getCompany().getId().equals(companyId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }
}
