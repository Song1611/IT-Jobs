package com.itjob.specification.helper;


import com.itjob.util.FilterParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
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

    private static final Set<String> VALID_OPERATORS = Set.of(":", "!", ">", "<", "~", "@", "#", ">=", "<=");

    /**
     * Validate all filter string
     *
     * @param filter filter string (e.g., "firstName:John")
     * @return true if valid
     */
    public boolean isValidFilter(String filter) {
        if (filter == null || filter.isEmpty()) {
            return false;
        }

        // use FilterParser to avoid code duplication
        FilterParser.FilterComponents components = FilterParser.parse(filter);

        if (components == null || !components.isValid()) {
            log.warn("Cannot parse filter: {}", filter);
            return false;
        }

        // Validate partial
        return isValidFieldName(components.getFieldName())
                && isValidOperator(components.getOperator())
                && isValidValue(components.getValue());
    }

    /**
     * Sanitize value (remove dangerous characters)
     *
     * @param value value need to sanitize
     * @return value is sanitized
     */
    public String sanitizeValue(String value) {
        if (value == null) {
            return null;
        }

        // Remove potential SQL injection characters
        return value.replaceAll("[';\"\\\\]", "");
    }

    /**
     * Validate field name (internal use only)
     *
     * @param fieldName name of field
     * @return true if valid
     */
    private boolean isValidFieldName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            log.warn("Field name is null or empty");
            return false;
        }

        // Check format
        if (!FIELD_NAME_PATTERN.matcher(fieldName).matches()) {
            log.warn("Invalid field name format: {}", fieldName);
            return false;
        }

        return true;
    }

    /**
     * Validate operator (internal use only)
     *
     * @param operator operator string
     * @return true if valid
     */
    private boolean isValidOperator(String operator) {
        if (operator == null || operator.isEmpty()) {
            log.warn("Operator is null or empty");
            return false;
        }

        if (!VALID_OPERATORS.contains(operator)) {
            log.warn("Invalid operator: {}", operator);
            return false;
        }

        return true;
    }

    /**
     * Validate value - check SQL injection, XSS (internal use only)
     *
     * @param value value need to validate
     * @return true if valid
     */
    private boolean isValidValue(String value) {
        if (value == null) {
            return true; // NULL is valid
        }

        // Check SQL injection
        if (SQL_INJECTION_PATTERN.matcher(value).matches()) {
            log.warn("Potential SQL injection detected in value: {}", value);
            return false;
        }

        return true;
    }
}
