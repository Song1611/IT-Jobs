package com.itjob.service.impl;

import com.itjob.redis.CacheName;
import com.itjob.dto.response.DashboardStatsResponse;
import com.itjob.enums.ApplicationStatus;
import com.itjob.enums.CompanyStatus;
import com.itjob.enums.JobStatus;
import com.itjob.repository.ApplicationRepository;
import com.itjob.repository.CompanyRepository;
import com.itjob.repository.JobRepository;
import com.itjob.repository.UserRepository;
import com.itjob.service.DashboardCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardCacheServiceImpl implements DashboardCacheService {
    
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    
    @Override
    @Cacheable(
        value = CacheName.DASHBOARD_HR,
        key = "T(com.itjob.util.CacheKeyGenerator).forHRDashboard(#companyId)"
    )
    public DashboardStatsResponse getCachedHRStats(UUID companyId) {
        log.info("Cache MISS - Fetching HR dashboard stats for company: {} from database", companyId);
        
        long totalActiveJobs = jobRepository.countByCompanyIdAndStatus(
            companyId, 
            JobStatus.OPEN.getValue()
        );
        
        // New applications in last 7 days
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long totalNewApplications = applicationRepository.countNewApplicationsByCompanyId(
            companyId,
            ApplicationStatus.PENDING.getValue(),
            sevenDaysAgo
        );
        
        // Total applications for all company jobs (optimized count query)
        long totalApplications = applicationRepository.countByCompanyId(companyId);
        
        // TODO: Implement Redis-based view aggregation
        long totalViews = 0L;
        
        return DashboardStatsResponse.builder()
                .totalActiveJobs(totalActiveJobs)
                .totalNewApplications(totalNewApplications)
                .totalApplications(totalApplications)
                .totalViews(totalViews)
                .build();
    }
    
    @Override
    @Cacheable(
        value = CacheName.DASHBOARD_ADMIN,
        key = "T(com.itjob.util.CacheKeyGenerator).STATS_SUFFIX"
    )
    public DashboardStatsResponse getCachedAdminStats() {
        log.info("Cache MISS - Fetching admin dashboard stats from database");
        
        long totalUsers = userRepository.count();
        long totalCompanies = companyRepository.count();
        long totalJobs = jobRepository.count();
        
        long pendingCompanies = companyRepository.countByStatus(CompanyStatus.PENDING.getValue());
        long activeCompanies = companyRepository.countByStatusAndIsDeleted(
            CompanyStatus.ACTIVE.getValue(), 
            false
        );
        
        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalCompanies(totalCompanies)
                .totalJobs(totalJobs)
                .pendingCompanies(pendingCompanies)
                .activeCompanies(activeCompanies)
                .build();
    }
}
