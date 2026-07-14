package com.itjob.service;

import com.itjob.dto.response.DashboardStatsResponse;

import java.util.UUID;

/**
 * Cache service for dashboard statistics.
 * Separated to avoid Spring Cache proxy bypass issue.
 */
public interface DashboardCacheService {
    
    /**
     * Get cached HR dashboard statistics.
     *
     * @param companyId Company ID
     * @return Dashboard statistics
     */
    DashboardStatsResponse getCachedHRStats(UUID companyId);
    
    /**
     * Get cached admin dashboard statistics.
     *
     * @return Dashboard statistics
     */
    DashboardStatsResponse getCachedAdminStats();
}
