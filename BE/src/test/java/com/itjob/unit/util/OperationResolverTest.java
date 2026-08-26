package com.itjob.unit.util;

import com.itjob.util.OperationResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.itjob.specification.SearchOperation.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit - OperationResolver")
class OperationResolverTest {

    @Test
    @DisplayName("resolves simple operators directly")
    void resolvesSimpleOperations() {
        assertThat(OperationResolver.resolveOperation("~", null, null))
                .isEqualTo(LIKE);

        assertThat(OperationResolver.resolveOperation(">=", null, null))
                .isEqualTo(GREATER_EQUAL);

        assertThat(OperationResolver.resolveOperation("<", null, null))
                .isEqualTo(LESS);

        assertThat(OperationResolver.resolveOperation("!", null, null))
                .isEqualTo(NEGATION);
    }

    @Test
    @DisplayName("unknown or empty operation -> null")
    void unknownOperationReturnsNull() {
        assertThat(OperationResolver.resolveOperation("??", null, null))
                .isNull();

        assertThat(OperationResolver.resolveOperation(null, null, null))
                .isNull();

        assertThat(OperationResolver.resolveOperation("", null, null))
                .isNull();
    }

    @Test
    @DisplayName("without wildcard -> EQUALITY")
    void equalityWithoutWildcard() {
        assertThat(OperationResolver.resolveOperation(":", null, null))
                .isEqualTo(EQUALITY);
    }

    @Test
    @DisplayName("wildcard on both sides -> CONTAINS")
    void bothWildcardsContains() {
        assertThat(OperationResolver.resolveOperation(":", "*", "*"))
                .isEqualTo(CONTAINS);
    }

    @Test
    @DisplayName("leading wildcard -> ENDS_WITH")
    void leadingWildcardEndsWith() {
        assertThat(OperationResolver.resolveOperation(":", "*", "value"))
                .isEqualTo(ENDS_WITH);
    }

    @Test
    @DisplayName("trailing wildcard -> STARTS_WITH")
    void trailingWildcardStartsWith() {
        assertThat(OperationResolver.resolveOperation(":", "value", "*"))
                .isEqualTo(STARTS_WITH);
    }

    @Test
    @DisplayName("wildcard handling only applies to EQUALITY")
    void wildcardOnlyAppliesToEquality() {
        assertThat(OperationResolver.resolveOperation("~", "v*", "*"))
                .isEqualTo(LIKE);
    }
}