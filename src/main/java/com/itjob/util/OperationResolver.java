package com.itjob.util;

import com.itjob.specification.SearchOperation;

import static com.itjob.specification.SearchOperation.*;

public final class OperationResolver {
    private OperationResolver() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static SearchOperation resolveOperation(String operation, String prefix, String suffix ) {
        SearchOperation searchOperation = SearchOperation.getSimpleOperation(operation);

        if(searchOperation == null) {
            return null;
        }

        if(searchOperation == EQUALITY){
            return resolveWildcardOperation(prefix, suffix);
        }
        return searchOperation;
    }


    public static SearchOperation resolveWildcardOperation(String prefix, String suffix) {
        final boolean startWithAsterisk = prefix != null && prefix.contains(ZERO_OR_MORE_REGEX);
        final boolean endWithAsterisk = suffix != null && suffix.contains(ZERO_OR_MORE_REGEX);

        if(startWithAsterisk && endWithAsterisk) {
            return CONTAINS;
        } else if(startWithAsterisk) {
            return STARTS_WITH;
        }
        else if(endWithAsterisk) {
            return ENDS_WITH;
        }
        return EQUALITY;
    }
}
