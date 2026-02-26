package com.itjob.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FilterParamConverter {

    private FilterParamConverter() {
        throw new UnsupportedOperationException("Utility class");   
    }

    private static final Set<String> DEFAULT_PAGINATION_PARAMS = new HashSet<>(
            Arrays.asList("page", "size","sort")
    );


    /**
     * Convert Map params to filter array (exclude default pagination params)
     *
     * @param allParams Map of all request params
     * @return String[] of filters
     */
    public static String[] convertToFilters(Map<String, String> allParams) {
        return convertToFilters(allParams, DEFAULT_PAGINATION_PARAMS);
    }

    /**
     * Convert Map params to filter array (exclude custom params)
     *
     * @param allParams Map of all request params
     * @param excludeParams Set of param names to exclude
     * @return String[] of filters
     */
   public static String[] convertToFilters(Map<String,String> allParams,  Set<String> excludeParams) {
       if(allParams == null || allParams.isEmpty()) {
           return new String[0];
       }

       return allParams.entrySet().stream()
               .filter(entry->!excludeParams.contains(entry.getKey()))
               .map(entry ->buildFilterString(entry.getKey(),entry.getValue()))
               .toArray(String[]::new);
   }

    private static String buildFilterString(String key, String value) {
       //if value null/empty -> key contains filter
       if(value == null || value.isEmpty()) {
           return key;
       }

       //otherwise value exists → combine key + value
       return key + ":" + value;
    }

    /**
     * Check if param is pagination param
     *
     * @param key param key
     * @return true if pagination param
     */
    public static boolean isPaginationParam(String key) {
        return DEFAULT_PAGINATION_PARAMS.contains(key);
    }
}
