package com.itjob.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

import java.util.List;

@Getter
@AllArgsConstructor
public class GenericSpecification<T> implements Specification<T> {

    private SpecSearchCriteria criteria;

    @Override
    public @Nullable Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        if (criteria.getValue() == null) {
            return cb.isNull(root.get(criteria.getKey()));
        }

        return switch (criteria.getOperation()) {
            case EQUALITY -> cb.equal(root.get(criteria.getKey()), criteria.getValue());

            case NEGATION -> cb.notEqual(root.get(criteria.getKey()), criteria.getValue());

            case GREATER -> cb.greaterThan(root.get(criteria.getKey()), (Comparable) criteria.getValue());

            case GREATER_EQUAL -> cb.greaterThanOrEqualTo(root.get(criteria.getKey()), (Comparable) criteria.getValue());

            case LESS -> cb.lessThan(root.get(criteria.getKey()), (Comparable) criteria.getValue());

            case LESS_EQUAL -> cb.lessThanOrEqualTo(root.get(criteria.getKey()), (Comparable) criteria.getValue());

            case LIKE -> cb.like(
                    cb.lower(root.get(criteria.getKey())),
                    "%" + criteria.getValue().toString().toLowerCase() + "%");

            case STARTS_WITH -> cb.like(
                    cb.lower(root.get(criteria.getKey())),
                    criteria.getValue().toString().toLowerCase() + "%");

            case ENDS_WITH -> cb.like(
                    cb.lower(root.get(criteria.getKey())),
                    "%" + criteria.getValue().toString().toLowerCase());

            case CONTAINS -> cb.like(
                    cb.lower(root.get(criteria.getKey())),
                    "%" + criteria.getValue().toString().toLowerCase() + "%");

            case IN -> {
                if (criteria.getValue() instanceof List<?> list) {
                    yield root.get(criteria.getKey()).in(list);
                }
                yield cb.equal(root.get(criteria.getKey()), criteria.getValue());
            }

            case BETWEEN -> {
                if (criteria.getValue() instanceof Object[] values && values.length == 2) {
                    yield cb.between(root.get(criteria.getKey()),
                            (Comparable) values[0], (Comparable) values[1]);
                }
                yield cb.equal(root.get(criteria.getKey()), criteria.getValue());
            }
        };
    }
}
