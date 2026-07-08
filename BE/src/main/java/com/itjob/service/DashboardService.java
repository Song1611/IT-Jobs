package com.itjob.service;

import com.itjob.dto.response.DashboardStatsResponse;

import java.util.UUID;

public interface DashboardService {
    
    // HR Dashboard
    DashboardStatsResponse getHRDashboardStats(UUID companyId);
    
    // Admin Dashboard
    DashboardStatsResponse getAdminDashboardStats();
}
