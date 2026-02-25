package com.itjob.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@Getter
@AllArgsConstructor
public class GenericSpecification<T> implements Specification<T>{

    private SpecSearchCriteria criteria;

    @Override
    public @Nullable Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {

        if(criteria.getValue()==null){
            return criteriaBuilder.isNull(root.get(criteria.getKey()));
        }

        return switch (criteria.getOperation()){

            case EQUALITY -> criteriaBuilder.equal(
                    root.get(criteria.getKey()),
                    criteria.getValue());

            case NEGATION -> criteriaBuilder.notEqual(
                    root.get(criteria.getKey()),
                    criteria.getValue());

            case GREATER_THAN -> criteriaBuilder.greaterThan(
                    root.get(criteria.getKey()),
                    (Comparable)criteria.getValue());

            case GREATER_EQUAL -> criteriaBuilder.greaterThanOrEqualTo(
                    root.get(criteria.getKey()),
                    (Comparable)criteria.getValue());

            case LESS_THAN -> criteriaBuilder.lessThan(
                    root.get(criteria.getKey()),
                    (Comparable)criteria.getValue());

            case LESS_EQUAL -> criteriaBuilder.lessThanOrEqualTo(
                    root.get(criteria.getKey()),
                    (Comparable)criteria.getValue());

            case LIKE -> criteriaBuilder.like(criteriaBuilder.lower(
                    root.get(criteria.getKey())),
                    "%"+criteria.getValue().toString().toLowerCase()+"%");

            case STARTS_WITH -> criteriaBuilder.like(
                    criteriaBuilder.lower(root.get(criteria.getKey())),
                    criteria.getValue().toString().toLowerCase()+ "%");

            case ENDS_WITH -> criteriaBuilder.like(
                    criteriaBuilder.lower(root.get(criteria.getKey())),
                    "%" + criteria.getValue().toString().toLowerCase());

            case CONTAINS -> criteriaBuilder.like(
                    criteriaBuilder.lower(root.get(criteria.getKey())),
                    "%" + criteria.getValue().toString().toLowerCase() + "%");

            case IN -> {
                if(criteria.getValue() instanceof List){
                    yield root.get(criteria.getKey()).in((List<?>) criteria.getValue());
                }else{
                    yield criteriaBuilder.equal(root.get(criteria.getKey()), criteria.getValue());
                }
            }

            case BETWEEN -> {
                if(criteria.getValue() instanceof Object[]){
                    Object[] values = (Object[]) criteria.getValue();
                    if(values.length == 2){
                        yield criteriaBuilder.between(
                                root.get(criteria.getKey()),
                                (Comparable) values[0],
                                (Comparable) values[1]
                        );

                    }

                }
                yield criteriaBuilder.equal(
                        root.get(criteria.getKey()),
                        criteria.getValue()
                );
            }
        };

    }
}
