package com.itjob.repository.projection;

import java.util.UUID;

/**
 * Projection interface for batch querying application counts by job IDs
 * Used to avoid N+1 query problem when fetching application counts for multiple jobs
 */
public interface JobApplicationCountProjection {
    UUID getJobId();
    Long getApplicationCount();
}
