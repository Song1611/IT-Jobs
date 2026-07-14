package com.itjob.util;

import java.util.UUID;

/**
 * Utility class for generating URL-friendly slugs
 * Separates slug generation logic from service layer
 */
public final class SlugUtil {

    private SlugUtil() {
        // Prevent instantiation
    }

    /**
     * Generate a URL-friendly slug from a name with random suffix
     * 
     * @param name The original name to slugify
     * @return URL-friendly slug with random suffix
     */
    public static String generateSlug(String name) {
        if (name == null || name.isEmpty()) {
            return UUID.randomUUID().toString().substring(0, 8);
        }
        
        String slug = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")  // Remove special chars
                .replaceAll("\\s+", "-")           // Replace spaces with hyphens
                .replaceAll("-+", "-")             // Remove duplicate hyphens
                .trim();
        
        // Add random suffix for uniqueness
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        
        return slug.isEmpty() ? suffix : slug + "-" + suffix;
    }
}
