package com.itjob.specification.helper;

import com.itjob.util.FilterParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

@Component
@Slf4j
public class FilterValidator {

    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            ".*(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|<script).*",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> VALID_OPERATORS = Set.of(":", "!", ">=", "<=", ">", "<", "~", "@", "#");

    public boolean isValidFilter(String filter) {
        if (filter == null || filter.isEmpty()) return false;

        FilterParser.FilterComponents components = FilterParser.parse(filter);
        if (components == null || !components.isValid()) {
            log.warn("Cannot parse filter: {}", filter);
            return false;
        }

        return isValidFieldName(components.getFieldName())
                && isValidOperator(components.getOperator())
                && isValidValue(components.getValue());
    }

    public String sanitizeValue(String value) {
        if (value == null) return null;
        return value.replaceAll("[';\"\\\\]", "");
    }

    private boolean isValidFieldName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) return false;
        if (!FIELD_NAME_PATTERN.matcher(fieldName).matches()) {
            log.warn("Invalid field name: {}", fieldName);
            return false;
        }
        return true;
    }

    private boolean isValidOperator(String operator) {
        if (!VALID_OPERATORS.contains(operator)) {
            log.warn("Invalid operator: {}", operator);
            return false;
        }
        return true;
    }

    private boolean isValidValue(String value) {
        if (value == null) return true;
        if (SQL_INJECTION_PATTERN.matcher(value).matches()) {
            log.warn("Potential SQL injection in value: {}", value);
            return false;
        }
        return true;
    }
}
