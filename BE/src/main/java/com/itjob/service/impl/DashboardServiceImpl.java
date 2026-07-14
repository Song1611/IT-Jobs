package com.itjob.service.impl;

import com.itjob.dto.response.DashboardStatsResponse;
import com.itjob.service.DashboardCacheService;
import com.itjob.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Dashboard service implementation.
 * Delegates to DashboardCacheService to avoid Spring Cache proxy bypass.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {
    
    private final DashboardCacheService dashboardCacheService;
    
    @Override
    public DashboardStatsResponse getHRDashboardStats(UUID companyId) {
        log.debug("Getting HR dashboard stats for company: {}", companyId);
        return dashboardCacheService.getCachedHRStats(companyId);
    }
    
    @Override
    public DashboardStatsResponse getAdminDashboardStats() {
        log.debug("Getting admin dashboard stats");
        return dashboardCacheService.getCachedAdminStats();
    }
}
