package com.itjob.service;

import com.itjob.dto.response.BlogResponse;

import java.util.UUID;

/**
 * Service to handle Blog caching operations.
 * Separated from BlogService to avoid Spring Cache proxy issues when calling @Cacheable methods from same class.
 */
public interface BlogCacheService {
    
    /**
     * Get blog by ID from cache or database
     * @param id Blog ID
     * @return Blog response with base data (no user-specific fields)
     */
    BlogResponse getCachedBlogById(UUID id);
}
