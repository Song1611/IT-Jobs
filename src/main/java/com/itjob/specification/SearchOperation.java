package com.itjob.specification;

public enum SearchOperation {
    EQUALITY,          // : (colon)
    NEGATION,          // ! (exclamation)
    GREATER_THAN,      // > (greater than)
    LESS_THAN,         // < (less than)
    GREATER_EQUAL,     // >= (greater than or equal)
    LESS_EQUAL,        // <= (less than or equal)
    LIKE,              // ~ (tilde) - case-insensitive
    IN,                // @ (at) - value1,value2,value3
    BETWEEN,           // # (hash) - min,max
    STARTS_WITH,       // prefix with *
    ENDS_WITH,         // suffix with *
    CONTAINS;          // both prefix and suffix with *

    public static final String OR_PREDICATE_FLAG = "'";

    public static final String ZERO_OR_MORE_REGEX = "*";

    public static SearchOperation getSimpleOperation(final String operator) {
        if(operator == null || operator.isEmpty()){
            return null;
        }

        return switch (operator) {
            case ">=" -> GREATER_EQUAL;
            case "<=" -> LESS_EQUAL;
            case ":" -> EQUALITY;
            case "!" -> NEGATION;
            case ">" -> GREATER_THAN;
            case "<" -> LESS_THAN;
            case "~" -> LIKE;
            case "@" -> IN;
            case "#" -> BETWEEN;
            default -> null;
        };
    }
}
