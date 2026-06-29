package com.itjob.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FilterParser {

    // Supported operators: : ! >= <= > < ~ @ #
    private static final Pattern FILTER_PATTERN = Pattern.compile("^(\\w+)(>=|<=|:|!|>|<|~|@|#)(.*)$");

    private FilterParser() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static FilterComponents parse(String filter) {
        if (filter == null || filter.isEmpty()) return null;

        Matcher matcher = FILTER_PATTERN.matcher(filter);
        if (matcher.matches()) {
            return new FilterComponents(matcher.group(1), matcher.group(2), matcher.group(3));
        }
        return null;
    }

    @Getter
    @AllArgsConstructor
    public static class FilterComponents {
        private final String fieldName;
        private final String operator;
        private final String value;

        public boolean isValid() {
            return fieldName != null && operator != null && value != null;
        }
    }
}
