package com.itjob.service.impl;

import com.itjob.constant.CacheName;
import com.itjob.dto.response.DashboardStatsResponse;
import com.itjob.repository.ApplicationRepository;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.JobRepository;
import com.itjob.repository.UserRepository;
import com.itjob.service.DashboardService;
import com.itjob.util.CacheKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {
    
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    
    @Override
    @Cacheable(value = CacheName.DASHBOARD_HR, key = "@cacheKeyGenerator.forHRDashboard(#companyId)")
    public DashboardStatsResponse getHRDashboardStats(UUID companyId) {
        log.info("Fetching HR dashboard stats for company: {} from database", companyId);
        
        long totalActiveJobs = jobRepository.countByCompanyIdAndStatus(companyId, "open");
        
        // New applications in last 7 days
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long totalNewApplications = applicationRepository.countNewApplicationsByCompanyId(companyId, sevenDaysAgo);
        
        // Total applications for all company jobs
        long totalApplications = applicationRepository.findByCompanyId(companyId, org.springframework.data.domain.Pageable.unpaged())
                .getTotalElements();
        
        // Total views would need to be calculated from job views
        // For now, we'll skip it or calculate from jobs
        long totalViews = 0L;
        
        return DashboardStatsResponse.builder()
                .totalActiveJobs(totalActiveJobs)
                .totalNewApplications(totalNewApplications)
                .totalApplications(totalApplications)
                .totalViews(totalViews)
                .build();
    }
    
    @Override
    @Cacheable(value = CacheName.DASHBOARD_ADMIN, key = "@cacheKeyGenerator.forAdminDashboard()")
    public DashboardStatsResponse getAdminDashboardStats() {
        log.info("Fetching admin dashboard stats from database");
        
        long totalUsers = userRepository.count();
        long totalCompanies = companyRepository.count();
        long totalJobs = jobRepository.count();
        
        // Count by role if needed
        // long totalCandidates = userRepository.countByRole("CANDIDATE");
        // long totalEmployers = userRepository.countByRole("HR");
        
        long pendingCompanies = companyRepository.countByStatus("pending");
        long activeCompanies = companyRepository.countByStatusAndIsDeleted("active", false);
        
        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalCompanies(totalCompanies)
                .totalJobs(totalJobs)
                .pendingCompanies(pendingCompanies)
                .activeCompanies(activeCompanies)
                .build();
    }
}
