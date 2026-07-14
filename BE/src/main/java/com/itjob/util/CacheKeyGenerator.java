package com.itjob.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

/**
 * Utility class for generating consistent cache keys.
 * Provides standardized key generation for various caching scenarios.
 * <p>
 * Key Design Principles:
 * <ul>
 * <li>Human-readable: Easy to understand and debug in Redis</li>
 * <li>Consistent: Same input always produces same key</li>
 * <li>Unique: Different inputs produce different keys</li>
 * <li>Sortable: Parameters sorted alphabetically for consistency</li>
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CacheKeyGenerator {
    
    private static final String DELIMITER = ":";
    private static final String PARAM_DELIMITER = ",";
    
    /**
     * Generate cache key for simple entity by ID.
     * <p>Format: "uuid-here"
     * <p>Usage: Single entity cache (job detail, company detail, user profile)
     *
     * @param id Entity ID
     * @return Cache key string
     */
    public static String forId(Object id) {
        return String.valueOf(id);
    }
    
    /**
     * Generate cache key for entity by slug.
     * <p>Format: "slug-here"
     * <p>Usage: When API uses slug instead of UUID
     *
     * @param slug Entity slug
     * @return Cache key string
     */
    public static String forSlug(String slug) {
        return slug;
    }
    
    /**
     * Generate cache key for list with limit.
     * <p>Format: "limit:10"
     * <p>Usage: Featured jobs, recent blogs, top companies
     *
     * @param limit Number of items
     * @return Cache key string
     */
    public static String forLimit(int limit) {
        return "limit" + DELIMITER + limit;
    }
    
    /**
     * Generate cache key for paginated results.
     * <p>Format: "page:0:size:10:sort:createdAt,DESC"
     * <p>Usage: ANY paginated API without filters
     *
     * @param pageable Pageable object with page, size, and sort info
     * @return Cache key string
     */
    public static String forPageable(Pageable pageable) {
        return buildPageableKey(pageable);
    }
    
    private static String buildPageableKey(Pageable pageable) {
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
     * Generate cache key for parent entity with paginated children.
     * <p>Format: "id:uuid:page:0:size:10:sort:createdAt,DESC"
     * <p>Usage: Parent-child relationships with pagination
     * <ul>
     * <li>Company → Jobs</li>
     * <li>Blog → Comments</li>
     * <li>User → Applications</li>
     * <li>Category → Blogs</li>
     * </ul>
     *
     * @param parentId Parent entity ID
     * @param pageable Pageable object
     * @return Cache key string
     */
    public static String forIdWithPageable(Object parentId, Pageable pageable) {
        return "id" + DELIMITER + parentId + DELIMITER + buildPageableKey(pageable);
    }
    
    /**
     * Generate cache key for search/filter operations.
     * <p>Format: "q:keyword:loc:location:salary:1000-3000:page:0:size:10"
     * <p>Usage: Search APIs with multiple filter criteria
     * <ul>
     * <li>Job search (keyword, location, salary, type, level)</li>
     * <li>Company search (name, industry, location)</li>
     * <li>Blog search (keyword, category, tags)</li>
     * </ul>
     * <p><b>Note:</b> Parameters are sorted alphabetically for consistency
     * <p><b>Note:</b> Null/empty values are excluded from key
     *
     * @param searchParams Map of search parameters (will be sorted alphabetically)
     * @param pageable Pageable object
     * @return Cache key string
     */
    public static String forSearch(Map<String, Object> searchParams, Pageable pageable) {
        StringJoiner joiner = new StringJoiner(DELIMITER);
        
        // Add non-null search parameters
        addSortedParams(joiner, searchParams);
        
        // Add pagination
        joiner.add(buildPageableKey(pageable));
        
        return joiner.toString();
    }
    
    /**
     * Generate cache key with dynamic composite parameters.
     * <p>Format: "key1:value1:key2:value2:key3:value3"
     * <p>Usage: Complex cache keys with multiple dynamic parameters
     * <ul>
     * <li>Custom business logic caching</li>
     * <li>Multi-criteria filtering</li>
     * <li>Extensible for future requirements</li>
     * </ul>
     * <p><b>Note:</b> Parameters are sorted alphabetically for consistency
     * <p><b>Note:</b> Null/empty values are excluded from key
     *
     * @param params Map of key-value pairs (will be sorted alphabetically)
     * @return Cache key string
     */
    public static String forComposite(Map<String, Object> params) {
        StringJoiner joiner = new StringJoiner(DELIMITER);
        addSortedParams(joiner, params);
        return joiner.toString();
    }
    
    private static void addSortedParams(StringJoiner joiner, Map<String, Object> params) {
        // Sort parameters alphabetically for consistent keys
        Map<String, Object> sortedParams = new TreeMap<>(params);
        
        // Add non-null parameters
        sortedParams.forEach((key, value) -> {
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                joiner.add(key).add(String.valueOf(value));
            }
        });
    }
    
    /**
     * Generate cache key for admin dashboard.
     * <p>Format: "stats"
     * <p>Usage: Admin dashboard statistics
     *
     * @return Cache key string
     */
    public static String forAdminDashboard() {
        return "stats";
    }
    
    /**
     * Generate cache key for HR dashboard.
     * <p>Format: "hr:uuid:stats"
     * <p>Usage: HR-specific dashboard statistics
     *
     * @param hrId HR user ID
     * @return Cache key string
     */
    public static String forHRDashboard(Object hrId) {
        return "hr" + DELIMITER + hrId + DELIMITER + "stats";
    }
    
    /**
     * Generate cache key for user dashboard.
     * <p>Format: "user:uuid:stats"
     * <p>Usage: User-specific dashboard statistics
     *
     * @param userId User ID
     * @return Cache key string
     */
    public static String forUserDashboard(Object userId) {
        return "user" + DELIMITER + userId + DELIMITER + "stats";
    }
}

