package com.itjob.specification;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.Serial;
import java.util.List;

/**
 * Generic JPA Specification for dynamic query building.
 * <p>
 * Supports various search operations including equality, comparison, pattern matching,
 * and collection operations (IN, BETWEEN).
 * </p>
 *
 * @param <T> the entity type
 */
public record GenericSpecification<T>(@NonNull SpecSearchCriteria criteria) implements Specification<T> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    @Nullable
    public Predicate toPredicate(@NonNull Root<T> root, CriteriaQuery<?> query, @NonNull CriteriaBuilder cb) {
        if (criteria.getValue() == null) {
            return cb.isNull(root.get(criteria.getKey()));
        }

        return switch (criteria.getOperation()) {
            case EQUALITY -> buildEqualityPredicate(root, cb);
            case NEGATION -> buildNegationPredicate(root, cb);
            case GREATER -> buildGreaterThanPredicate(root, cb);
            case GREATER_EQUAL -> buildGreaterOrEqualPredicate(root, cb);
            case LESS -> buildLessThanPredicate(root, cb);
            case LESS_EQUAL -> buildLessOrEqualPredicate(root, cb);
            case LIKE, CONTAINS -> buildLikePredicate(root, cb, "%", "%");
            case STARTS_WITH -> buildLikePredicate(root, cb, "", "%");
            case ENDS_WITH -> buildLikePredicate(root, cb, "%", "");
            case IN -> buildInPredicate(root, cb);
            case BETWEEN -> buildBetweenPredicate(root, cb);
        };
    }

    private Predicate buildEqualityPredicate(Root<T> root, CriteriaBuilder cb) {
        return cb.equal(root.get(criteria.getKey()), criteria.getValue());
    }

    private Predicate buildNegationPredicate(Root<T> root, CriteriaBuilder cb) {
        return cb.notEqual(root.get(criteria.getKey()), criteria.getValue());
    }

    private Predicate buildGreaterThanPredicate(Root<T> root, CriteriaBuilder cb) {
        Expression<Comparable<Object>> expression = getComparableExpression(root);
        Comparable<Object> value = castToComparable(criteria.getValue());
        return cb.greaterThan(expression, value);
    }

    private Predicate buildGreaterOrEqualPredicate(Root<T> root, CriteriaBuilder cb) {
        Expression<Comparable<Object>> expression = getComparableExpression(root);
        Comparable<Object> value = castToComparable(criteria.getValue());
        return cb.greaterThanOrEqualTo(expression, value);
    }

    private Predicate buildLessThanPredicate(Root<T> root, CriteriaBuilder cb) {
        Expression<Comparable<Object>> expression = getComparableExpression(root);
        Comparable<Object> value = castToComparable(criteria.getValue());
        return cb.lessThan(expression, value);
    }

    private Predicate buildLessOrEqualPredicate(Root<T> root, CriteriaBuilder cb) {
        Expression<Comparable<Object>> expression = getComparableExpression(root);
        Comparable<Object> value = castToComparable(criteria.getValue());
        return cb.lessThanOrEqualTo(expression, value);
    }

    private Predicate buildLikePredicate(Root<T> root, CriteriaBuilder cb, String prefix, String suffix) {
        Expression<String> lowerExpression = cb.lower(root.get(criteria.getKey()));
        String pattern = prefix + criteria.getValue().toString().toLowerCase() + suffix;
        return cb.like(lowerExpression, pattern);
    }

    private Predicate buildInPredicate(Root<T> root, CriteriaBuilder cb) {
        if (criteria.getValue() instanceof List<?> list) {
            return root.get(criteria.getKey()).in(list);
        }
        return buildEqualityPredicate(root, cb);
    }

    private Predicate buildBetweenPredicate(Root<T> root, CriteriaBuilder cb) {
        if (criteria.getValue() instanceof Object[] values && values.length == 2) {
            Expression<Comparable<Object>> expression = getComparableExpression(root);
            Comparable<Object> start = castToComparable(values[0]);
            Comparable<Object> end = castToComparable(values[1]);
            return cb.between(expression, start, end);
        }
        return buildEqualityPredicate(root, cb);
    }

    @SuppressWarnings("unchecked")
    private Expression<Comparable<Object>> getComparableExpression(Root<T> root) {
        return (Expression<Comparable<Object>>) (Expression<?>) root.get(criteria.getKey());
    }

    @SuppressWarnings("unchecked")
    private Comparable<Object> castToComparable(Object value) {
        return (Comparable<Object>) value;
    }
}
