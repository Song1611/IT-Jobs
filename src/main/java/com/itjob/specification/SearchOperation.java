package com.itjob.specification;

public enum SearchOperation {
    EQUALITY,       // :   exact match          filter=name:John
    NEGATION,       // !   not equal            filter=status!inactive
    GREATER,        // >   greater than         filter=age>18
    GREATER_EQUAL,  // >=  greater or equal     filter=age>=18
    LESS,           // <   less than            filter=age<60
    LESS_EQUAL,     // <=  less or equal        filter=age<=60
    LIKE,           // ~   case-insensitive     filter=name~john
    IN,             // @   in list              filter=status@active,pending
    BETWEEN,        // #   between two values   filter=age#18,60
    STARTS_WITH,    // :*value   starts with
    ENDS_WITH,      // :value*   ends with
    CONTAINS;       // :*value*  contains

    public static final String OR_PREDICATE_FLAG = "'";
    public static final String ZERO_OR_MORE_REGEX = "*";

    public static SearchOperation getSimpleOperation(final String operator) {
        if (operator == null || operator.isEmpty()) return null;
        return switch (operator) {
            case ":"  -> EQUALITY;
            case "!"  -> NEGATION;
            case ">=" -> GREATER_EQUAL;
            case "<=" -> LESS_EQUAL;
            case ">"  -> GREATER;
            case "<"  -> LESS;
            case "~"  -> LIKE;
            case "@"  -> IN;
            case "#"  -> BETWEEN;
            default   -> null;
        };
    }
}
