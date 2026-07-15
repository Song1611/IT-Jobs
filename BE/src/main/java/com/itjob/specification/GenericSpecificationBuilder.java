package com.itjob.specification;

import com.itjob.util.OperationResolver;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GenericSpecificationBuilder<T> {

    List<SpecSearchCriteria> params = new ArrayList<>();

    /**
     * Add search criteria (AND by default)
     */
    public GenericSpecificationBuilder<T> with(String key,
                                               SearchOperation operation,
                                               Object value) {
        params.add(new SpecSearchCriteria(key, operation, value));
        return this;
    }

    /**
     * Add search criteria with AND / OR
     */
    public GenericSpecificationBuilder<T> with(String key,
                                               SearchOperation operation,
                                               Object value,
                                               boolean isOrPredicate) {
        SpecSearchCriteria criteria = new SpecSearchCriteria(key, operation, value);
        criteria.setOrPredicate(isOrPredicate);
        params.add(criteria);
        return this;
    }

    /**
     * Add search criteria from parsed query.
     */
    public GenericSpecificationBuilder<T> with(String orPredicate,
                                               String key,
                                               String operation,
                                               Object value,
                                               String prefix,
                                               String suffix) {

        SearchOperation searchOperation =
                OperationResolver.resolveOperation(operation, prefix, suffix);

        if (searchOperation != null) {
            params.add(new SpecSearchCriteria(orPredicate, key, searchOperation, value));
            log.debug("Added criteria: {} {} {}", key, searchOperation, value);
        } else {
            log.debug("Skipped criteria because operation could not be resolved: {}", key);
        }

        return this;
    }

    /**
     * Build Specification from all added criteria.
     *
     * @return Specification or null if no criteria exist.
     */
    public Specification<T> build() {

        if (params.isEmpty()) {
            return null;
        }

        Specification<T> result = new GenericSpecification<>(params.getFirst());

        for (int i = 1; i < params.size(); i++) {

            Specification<T> current = new GenericSpecification<>(params.get(i));

            if (params.get(i).isOrPredicate()) {
                result = result.or(current);
            } else {
                result = result.and(current);
            }
        }

        return result;
    }
}