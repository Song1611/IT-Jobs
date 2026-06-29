package com.itjob.specification;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static com.itjob.specification.SearchOperation.*;

@Getter
@Setter
@NoArgsConstructor
public class SpecSearchCriteria {

    private String key;
    private SearchOperation operation;
    private Object value;
    private boolean orPredicate;

    public SpecSearchCriteria(final String key, final SearchOperation operation, final Object value) {
        this.key = key;
        this.operation = operation;
        this.value = value;
        this.orPredicate = false;
    }

    //Constructor with OR Predicate Flag
    public SpecSearchCriteria(final String orPredicate, final String key, final SearchOperation operation, final Object value) {
        this.orPredicate = orPredicate != null && orPredicate.equals(OR_PREDICATE_FLAG);
        this.key = key;
        this.operation = operation;
        this.value = value;
    }
}
