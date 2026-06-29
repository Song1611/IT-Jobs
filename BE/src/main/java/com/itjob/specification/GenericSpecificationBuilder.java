package com.itjob.specification;

import com.itjob.util.OperationResolver;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GenericSpecificationBuilder<T> {
    private final List<SpecSearchCriteria> params;

    public GenericSpecificationBuilder() {
        params = new ArrayList<>();
    }

    /**
     * Add criteria search (default is AND)
     *
     * @param key name field (firstName, age, email...)
     * @param operation operator (EQUALITY, GREATER_THAN...)
     * @param value value need to compare
     * @return builder instance (to chain methods)
     */
    public GenericSpecificationBuilder<T> with(final String key,
                                               final SearchOperation operation,
                                               final Object value) {
        params.add(new SpecSearchCriteria(key, operation, value));
        return this;
    }

    /**
     * Add criteria search with optional AND/OR
     *
     * @param key name field
     * @param operation operator
     * @param value value
     * @param isOrPredicate true = OR, false = AND
     * @return builder instance
     */
    public GenericSpecificationBuilder<T> with(final String key,
                                               final SearchOperation operation,
                                               final Object value,
                                               final boolean isOrPredicate) {
        SpecSearchCriteria criteria = new SpecSearchCriteria(key, operation, value);
        criteria.setOrPredicate(isOrPredicate);
        params.add(criteria);
        return this;
    }

    /**
     * Add criteria with full params (include OR predicate flag)
     *
     * @param orPredicate OR flag ("'" = OR, null = AND)
     * @param key name field
     * @param operation operator (can multi-char như >=, <=)
     * @param value value
     * @param prefix prefix of value
     * @param suffix suffix of value
     * @return builder instance
     */
    public GenericSpecificationBuilder<T> with(final String orPredicate,
                                               final String key,
                                               final String operation,
                                               final Object value,
                                               final String prefix,
                                               final String suffix) {

        SearchOperation searchOperation = OperationResolver.resolveOperation(operation, prefix, suffix);

        if (searchOperation != null) {
            SpecSearchCriteria criteria = new SpecSearchCriteria(orPredicate, key, searchOperation, value);
            params.add(criteria);
            System.out.println("Added criteria: " + key + " " + searchOperation + " " + value);
        } else {
            System.out.println("Skipped criteria (null operation): " + key);
        }
        return this;
    }

    /**
     * Build Specification by all criteria added
     * This Method combine all criteria with AND/OR logic
     * @return Specification<T> or null if don't have any criteria
     */
    public Specification<T> build() {
        System.out.println("Building specification with " + params.size() + " criteria");
        
        if (params.isEmpty()) {
            System.out.println("No criteria to build!");
            return null;
        }
        
        Specification<T> result = new GenericSpecification<>(params.getFirst());

        for (int i = 1; i < params.size(); i++) {
            result = params.get(i).isOrPredicate()
                    ? Specification.where(result).or(new GenericSpecification<>(params.get(i)))
                    : Specification.where(result).and(new GenericSpecification<>(params.get(i)));
        }

        System.out.println("Specification built successfully!");
        return result;
    }



}
