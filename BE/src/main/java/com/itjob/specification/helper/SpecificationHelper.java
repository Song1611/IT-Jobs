package com.itjob.specification.helper;

import com.itjob.specification.GenericSpecificationBuilder;
import com.itjob.specification.SearchOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpecificationHelper {

    private final FilterValidator filterValidator;
    private final TypeConverter typeConverter;

    // Groups: (field)(operator)(*?)(value)(*?)
    private static final Pattern FILTER_PATTERN =
            Pattern.compile("(\\w+?)(>=|<=|:|!|>|<|~|@|#)(\\*?)([^*]+)(\\*?)$");

    /**
     * Build Specification from filter[] param array.
     * Each filter string format: field{op}value
     * Examples:
     *   name~john        → LIKE %john%
     *   status:active    → EQUALITY
     *   age>18           → GREATER
     *   age>=18          → GREATER_EQUAL
     *   age<60           → LESS
     *   age<=60          → LESS_EQUAL
     *   age#18,60        → BETWEEN
     *   status@active,pending → IN
     *   name:*john       → STARTS_WITH
     *   name:john*       → ENDS_WITH
     *   name:*john*      → CONTAINS
     *   'status:active   → OR predicate
     */
    public <T> Specification<T> buildSpecification(String[] filters) {
        if (filters == null || filters.length == 0) return null;

        GenericSpecificationBuilder<T> builder = new GenericSpecificationBuilder<>();

        for (String filter : filters) {
            addFilter(builder, filter);
        }

        return builder.build();
    }

    private <T> void addFilter(GenericSpecificationBuilder<T> builder, String filter) {
        if (filter == null || filter.isBlank()) {
            return;
        }

        // Check OR predicate flag
        String orPredicate = null;
        String f = filter;
        if (f.startsWith(SearchOperation.OR_PREDICATE_FLAG)) {
            orPredicate = SearchOperation.OR_PREDICATE_FLAG;
            f = f.substring(1);
        }

        if (!filterValidator.isValidFilter(f)) {
            log.warn("Invalid filter skipped: {}", f);
            return;
        }

        Matcher matcher = FILTER_PATTERN.matcher(f);
        if (!matcher.find()) {
            log.warn("Cannot parse filter: {}", f);
            return;
        }

        String key       = matcher.group(1);
        String operation = matcher.group(2);
        String prefix    = matcher.group(3);
        String value     = matcher.group(4);
        String suffix    = matcher.group(5);

        log.info("Parsed filter - key: {}, operation: {}, prefix: {}, value: {}, suffix: {}",
                 key, operation, prefix, value, suffix);

        value = filterValidator.sanitizeValue(value);
        Object convertedValue = convertValue(operation, value);

        log.info("Converted value: {} (type: {})", convertedValue,
                 convertedValue != null ? convertedValue.getClass().getSimpleName() : "null");

        builder.with(orPredicate, key, operation, convertedValue, prefix, suffix);
    }

    private Object convertValue(String operator, String value) {
        if (value == null || value.isEmpty()) return value;
        return switch (operator) {
            case "@" -> typeConverter.convertListAuto(value);
            case "#" -> typeConverter.parseBetweenValueAuto(value);
            case ">=", "<=", ">", "<" -> typeConverter.convertAuto(value);
            case "!", ":" -> value; // Keep as string for EQUALITY/NEGATION to preserve leading zeros
            default  -> value; // ~ keeps as string
        };
    }
}
