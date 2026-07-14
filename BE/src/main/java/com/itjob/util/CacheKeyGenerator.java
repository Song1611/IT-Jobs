package com.itjob.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

/**
 * Utility class for generating consistent cache keys
 * Provides standardized key generation for various caching scenarios
 * 
 * Key Design Principles:
 * - Human-readable: Easy to understand and debug in Redis
 * - Consistent: Same input always produces same key
 * - Unique: Different inputs produce different keys
 * - Sortable: Parameters sorted alphabetically for consistency
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CacheKeyGenerator {
    
    private static final String DELIMITER = ":";
    private static final String PARAM_DELIMITER = ",";
    
    /**
     * Generate cache key for simple entity by ID
     * Format: "uuid-here"
     * 
     * Usage: Single entity cache (job detail, company detail, user profile)
     * 
     * @param id Entity ID
     * @return Cache key string
     * 
     * @example
     * forId(UUID.fromString("123...")) → "123e4567-e89b-12d3-a456-426614174000"
     */
    public static String forId(Object id) {
        return String.valueOf(id);
    }
    
    /**
     * Generate cache key for entity by slug
     * Format: "slug-here"
     * 
     * Usage: When API uses slug instead of UUID
     * 
     * @param slug Entity slug
     * @return Cache key string
     * 
     * @example
     * forSlug("senior-java-developer-hanoi") → "senior-java-developer-hanoi"
     */
    public static String forSlug(String slug) {
        return slug;
    }
    
    /**
     * Generate cache key for list with limit
     * Format: "limit:10"
     * 
     * Usage: Featured jobs, recent blogs, top companies
     * 
     * @param limit Number of items
     * @return Cache key string
     * 
     * @example
     * forLimit(10) → "limit:10"
     * forLimit(20) → "limit:20"
     */
    public static String forLimit(int limit) {
        return "limit" + DELIMITER + limit;
    }
    
    /**
     * Generate cache key for paginated results
     * Format: "page:0:size:10:sort:createdAt,DESC"
     * 
     * Usage: ANY paginated API without filters
     * 
     * @param pageable Pageable object with page, size, and sort info
     * @return Cache key string
     * 
     * @example
     * forPageable(PageRequest.of(0, 10)) → "page:0:size:10"
     * forPageable(PageRequest.of(0, 10, Sort.by("title").ascending())) 
     *   → "page:0:size:10:sort:title,ASC"
     */
    public static String forPageable(Pageable pageable) {
        StringJoiner joiner = new StringJoiner(DELIMITER);
        
        joiner.add("page").add(String.valueOf(pageable.getPageNumber()));
        joiner.add("size").add(String.valueOf(pageable.getPageSize()));
        
        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            StringJoiner sortJoiner = new StringJoiner(PARAM_DELIMITER);
            sort.forEach(order -> 
                sortJoiner.add(order.getProperty() + PARAM_DELIMITER + order.getDirection())
            );
            joiner.add("sort").add(sortJoiner.toString());
        }
        
        return joiner.toString();
    }
    
    /**
     * Generate cache key for parent entity with paginated children
     * Format: "id:uuid:page:0:size:10:sort:createdAt,DESC"
     * 
     * Usage: Parent-child relationships with pagination
     * - Company → Jobs
     * - Blog → Comments
     * - User → Applications
     * - Category → Blogs
     * 
     * @param parentId Parent entity ID
     * @param pageable Pageable object
     * @return Cache key string
     * 
     * @example
     * forIdWithPageable(companyId, pageable) 
     *   → "id:123e4567-e89b-12d3-a456-426614174000:page:0:size:10"
     */
    public static String forIdWithPageable(Object parentId, Pageable pageable) {
        return "id" + DELIMITER + parentId + DELIMITER + forPageable(pageable);
    }
    
    /**
     * Generate cache key for search/filter operations
     * Format: "q:keyword:loc:hanoi:salary:1000-3000:page:0:size:10"
     * 
     * Usage: Search APIs with multiple filter criteria
     * - Job search (keyword, location, salary, type, level)
     * - Company search (name, industry, location)
     * - Blog search (keyword, category, tags)
     * 
     * @param searchParams Map of search parameters (will be sorted alphabetically)
     * @param pageable Pageable object
     * @return Cache key string
     * 
     * @example
     * Map<String, Object> params = new LinkedHashMap<>();
     * params.put("keyword", "java developer");
     * params.put("location", "hanoi");
     * params.put("salaryMin", 1000);
     * forSearch(params, pageable) 
     *   → "keyword:java developer:location:hanoi:salaryMin:1000:page:0:size:10"
     * 
     * @note Parameters are sorted alphabetically for consistency
     * @note Null/empty values are excluded from key
     */
    public static String forSearch(Map<String, Object> searchParams, Pageable pageable) {
        StringJoiner joiner = new StringJoiner(DELIMITER);
        
        // Sort parameters alphabetically for consistent keys
        Map<String, Object> sortedParams = new TreeMap<>(searchParams);
        
        // Add non-null search parameters
        sortedParams.forEach((key, value) -> {
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                joiner.add(key).add(String.valueOf(value));
            }
        });
        
        // Add pagination
        joiner.add(forPageable(pageable));
        
        return joiner.toString();
    }
    
    /**
     * Generate cache key with dynamic composite parameters
     * Format: "key1:value1:key2:value2:key3:value3"
     * 
     * Usage: Complex cache keys with multiple dynamic parameters
     * - Custom business logic caching
     * - Multi-criteria filtering
     * - Extensible for future requirements
     * 
     * @param params Map of key-value pairs (will be sorted alphabetically)
     * @return Cache key string
     * 
     * @example
     * Map<String, Object> params = new LinkedHashMap<>();
     * params.put("type", "full-time");
     * params.put("level", "senior");
     * params.put("remote", true);
     * forComposite(params) → "level:senior:remote:true:type:full-time"
     * 
     * @note Parameters are sorted alphabetically for consistency
     * @note Null/empty values are excluded from key
     */
    public static String forComposite(Map<String, Object> params) {
        StringJoiner joiner = new StringJoiner(DELIMITER);
        
        // Sort parameters alphabetically for consistent keys
        Map<String, Object> sortedParams = new TreeMap<>(params);
        
        // Add non-null parameters
        sortedParams.forEach((key, value) -> {
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                joiner.add(key).add(String.valueOf(value));
            }
        });
        
        return joiner.toString();
    }
    
    /**
     * Generate cache key for admin dashboard
     * Format: "stats" or "stats:period:monthly"
     * 
     * Usage: Admin dashboard statistics
     * 
     * @param period Optional time period (daily, weekly, monthly, yearly)
     * @return Cache key string
     * 
     * @example
     * forAdminDashboard(null) → "stats"
     * forAdminDashboard("monthly") → "stats:period:monthly"
     */
    public static String forAdminDashboard(String period) {
        if (period != null && !period.trim().isEmpty()) {
            return "stats" + DELIMITER + "period" + DELIMITER + period;
        }
        return "stats";
    }
    
    /**
     * Generate cache key for HR dashboard
     * Format: "hr:uuid:stats" or "hr:uuid:stats:period:monthly"
     * 
     * Usage: HR-specific dashboard statistics
     * 
     * @param hrId HR user ID
     * @param period Optional time period
     * @return Cache key string
     * 
     * @example
     * forHRDashboard(hrId, null) → "hr:123...:stats"
     * forHRDashboard(hrId, "monthly") → "hr:123...:stats:period:monthly"
     */
    public static String forHRDashboard(Object hrId, String period) {
        StringJoiner joiner = new StringJoiner(DELIMITER);
        joiner.add("hr").add(String.valueOf(hrId)).add("stats");
        
        if (period != null && !period.trim().isEmpty()) {
            joiner.add("period").add(period);
        }
        
        return joiner.toString();
    }
    
    /**
     * Generate cache key for user dashboard
     * Format: "user:uuid:stats" or "user:uuid:stats:period:monthly"
     * 
     * Usage: User-specific dashboard statistics
     * 
     * @param userId User ID
     * @param period Optional time period
     * @return Cache key string
     * 
     * @example
     * forUserDashboard(userId, null) → "user:123...:stats"
     * forUserDashboard(userId, "monthly") → "user:123...:stats:period:monthly"
     */
    public static String forUserDashboard(Object userId, String period) {
        StringJoiner joiner = new StringJoiner(DELIMITER);
        joiner.add("user").add(String.valueOf(userId)).add("stats");
        
        if (period != null && !period.trim().isEmpty()) {
            joiner.add("period").add(period);
        }
        
        return joiner.toString();
    }
}

