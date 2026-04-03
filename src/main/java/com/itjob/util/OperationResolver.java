package com.itjob.util;

import com.itjob.specification.SearchOperation;
import lombok.extern.slf4j.Slf4j;

import static com.itjob.specification.SearchOperation.*;

@Slf4j
public final class OperationResolver {

    private OperationResolver() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static SearchOperation resolveOperation(String operation, String prefix, String suffix) {
        log.info("Resolving operation - operation: {}, prefix: '{}', suffix: '{}'", operation, prefix, suffix);
        
        SearchOperation searchOperation = SearchOperation.getSimpleOperation(operation);
        if (searchOperation == null) {
            log.warn("Unknown operation: {}", operation);
            return null;
        }

        // Only EQUALITY supports wildcard variants
        if (searchOperation == EQUALITY) {
            SearchOperation resolved = resolveWildcardOperation(prefix, suffix);
            log.info("Resolved to: {}", resolved);
            return resolved;
        }
        
        log.info("Resolved to: {}", searchOperation);
        return searchOperation;
    }

    private static SearchOperation resolveWildcardOperation(String prefix, String suffix) {
        boolean startsWith = prefix != null && prefix.contains(ZERO_OR_MORE_REGEX);
        boolean endsWith   = suffix != null && suffix.contains(ZERO_OR_MORE_REGEX);

        if (startsWith && endsWith) return CONTAINS;
        if (startsWith)             return ENDS_WITH;    // *value → ENDS_WITH
        if (endsWith)               return STARTS_WITH;  // value* → STARTS_WITH
        return EQUALITY;
    }
}
