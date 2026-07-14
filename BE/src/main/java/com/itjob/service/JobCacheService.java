package com.itjob.service;

import com.itjob.dto.response.JobResponse;

import java.util.UUID;

/**
 * Service to handle Job caching operations.
 * Separated from JobService to avoid Spring Cache proxy issues when calling @Cacheable methods from same class.
 */
public interface JobCacheService {
    
    /**
     * Get job by ID from cache or database
     * @param id Job ID
     * @return Job response with base data (no user-specific fields)
     */
    JobResponse getCachedJobById(UUID id);
}
