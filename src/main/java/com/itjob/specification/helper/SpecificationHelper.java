package com.itjob.specification.helper;

import com.itjob.specification.GenericSpecificationBuilder;
import com.itjob.specification.SearchOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


//SpecificationHelper to convert request param into specification
@Component
@RequiredArgsConstructor
@Slf4j
public class SpecificationHelper {

    private final FilterValidator filterValidator;
    private final TypeConverter typeConverter;

    private static final String SEARCH_SPEC_OPERATOR =
            "(\\w+?)(>=|<=|:|!|>|<|~|@|#)(.*?)(\\*?)(\\*?)";

    /**
     * Build Specification from array of filter strings
     *
     * @param filters array of filter strings (e.g., ["firstName:John", "age>=25"])
     * @param <T> Entity type
     * @return Specification<T> or null if filters are empty
     */

    public <T> Specification<T> buildSpecification(String[] filters) {
        if(filters == null || filters.length == 0) {
            return null;
        }

        GenericSpecificationBuilder<T> builder = new GenericSpecificationBuilder<>();
        Pattern pattern = Pattern.compile(SEARCH_SPEC_OPERATOR);

        for(String filter : filters) {
            // Check for OR predicate flag (')
            String orPredicate = null;
            if (filter.startsWith(SearchOperation.OR_PREDICATE_FLAG)) {
                orPredicate = SearchOperation.OR_PREDICATE_FLAG;
                filter = filter.substring(1); // Remove ' prefix
            }
            // Validate filter format and security
            if(!filterValidator.isValidFilter(filter)) {
                log.warn("Invalid filter: {}", filter);
                continue;
            }
            Matcher matcher = pattern.matcher(filter);

            if(matcher.find()){
                String key = matcher.group(1);        //field name
                String operation = matcher.group(2);  // operator
                String value = matcher.group(3);      // value
                String prefix = matcher.group(4);     // prefix (*)
                String suffix = matcher.group(5);     // suffix (*)

                //Sanitize value
                value = filterValidator.sanitizeValue(value);

                //Convert value to correct type based on operator
                Object convertedValue = convertValue(operation, value);

                // Add to builder
                builder.with(orPredicate,key, operation, convertedValue, prefix, suffix);
            }
            else {
                log.warn("Cannot parse filter: {}", filter);
            }

        }
        return builder.build();
    }
    /**
     * Convert value to correct type based on operator
     *
     * @param operator operator string
     * @param value string value
     * @return converted value
     */
    private Object convertValue(String operator, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return switch (operator){
            case "@" ->
                // IN operator: convert to List
                    typeConverter.convertListAuto(value);
            case "#" ->
                // BETWEEN operator: convert to array [min, max]
                    typeConverter.parseBetweenValueAuto(value);

            case ">", "<", ">=", "<=" ->
                // Comparison operators: auto-detect numeric type
                    typeConverter.convertAuto(value);
            default ->
                // Other operators: keep as string or auto-convert
                    typeConverter.convertAuto(value);
        };
    }
}
