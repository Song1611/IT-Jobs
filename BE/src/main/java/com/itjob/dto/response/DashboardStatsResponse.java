package com.itjob.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardStatsResponse {
    // HR Dashboard
    Long totalActiveJobs;
    Long totalNewApplications;
    Long totalViews;
    Long totalApplications;
    
    // Admin Dashboard
    Long totalUsers;
    Long totalCompanies;
    Long totalJobs;
    Long totalCandidates;
    Long totalEmployers;
    Long pendingCompanies;
    Long activeCompanies;
}
