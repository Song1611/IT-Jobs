package com.itjob.unit.specification;

import com.itjob.specification.GenericSpecificationBuilder;
import com.itjob.specification.SearchOperation;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static com.itjob.specification.SearchOperation.EQUALITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Unit - GenericSpecificationBuilder")
class GenericSpecificationBuilderTest {

    private final Root<Object> root = newRoot();
    private final CriteriaQuery<Object> query = newQuery();
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);

    @SuppressWarnings({"unchecked"})
    private static <T> Root<T> newRoot() {
        return (Root<T>) mock(Root.class);
    }

    @SuppressWarnings({"unchecked"})
    private static <T> CriteriaQuery<T> newQuery() {
        return (CriteriaQuery<T>) mock(CriteriaQuery.class);
    }

    @SuppressWarnings({"unchecked"})
    private static <Y> Path<Y> newPath() {
        return (Path<Y>) mock(Path.class);
    }

    @SuppressWarnings({"unchecked"})
    private static <Y> Expression<Y> newExpression() {
        return (Expression<Y>) mock(Expression.class);
    }

    private Predicate stubEquality(String key, String value) {
        Path<Object> path = newPath();
        when(root.get(key)).thenReturn(path);
        Predicate predicate = mock(Predicate.class);
        when(cb.equal(path, value)).thenReturn(predicate);
        return predicate;
    }

    @Nested
    @DisplayName("build")
    class Build {

        @Test
        @DisplayName("no criteria -> null specification")
        void emptyBuilderReturnsNull() {
            assertThat(new GenericSpecificationBuilder<>().build()).isNull();
        }

        @Test
        @DisplayName("single criterion -> non-null specification")
        void singleCriterionBuildsSpec() {
            GenericSpecificationBuilder<Object> builder = new GenericSpecificationBuilder<>();

            builder.with("status", EQUALITY, "active");

            assertThat(builder.build()).isNotNull();
        }
    }

    @Nested
    @DisplayName("with")
    class With {

        @Test
        @DisplayName("returns the same builder for fluent chaining")
        void fluentChainingReturnsSameInstance() {
            GenericSpecificationBuilder<Object> builder = new GenericSpecificationBuilder<>();

            GenericSpecificationBuilder<Object> result =
                    builder.with("status", EQUALITY, "active")
                            .with("level", SearchOperation.LIKE, "senior");

            assertThat(result).isSameAs(builder);
        }

        @Test
        @DisplayName("unresolvable operation is skipped -> build returns null")
        void unresolvableOperationIsSkipped() {
            GenericSpecificationBuilder<Object> builder = new GenericSpecificationBuilder<>();

            builder.with(null, "status", "??", "active", "", "");

            assertThat(builder.build()).isNull();
        }

        @Test
        @DisplayName("resolvable operation adds a criterion -> build non-null")
        void resolvableOperationIsAdded() {
            GenericSpecificationBuilder<Object> builder = new GenericSpecificationBuilder<>();

            builder.with(null, "status", ":", "active", "", "");

            assertThat(builder.build()).isNotNull();
        }

        @Test
        @DisplayName("wildcard prefix/suffix resolves to CONTAINS and adds criterion")
        void wildcardResolvesToContains() {
            GenericSpecificationBuilder<Object> builder = new GenericSpecificationBuilder<>();

            builder.with(null, "name", ":", "john", "*", "*");

            assertThat(builder.build()).isNotNull();
        }
    }

    @Nested
    @DisplayName("predicate composition")
    class Composition {

        @Test
        @DisplayName("two default criteria are combined with cb.and")
        void combinesWithAnd() {
            Predicate first = stubEquality("status", "active");
            Predicate second = stubEquality("level", "senior");
            Predicate combined = mock(Predicate.class);
            when(cb.and(first, second)).thenReturn(combined);

            GenericSpecificationBuilder<Object> builder = new GenericSpecificationBuilder<>();
            builder.with("status", EQUALITY, "active");
            builder.with("level", EQUALITY, "senior");

            Specification<Object> spec = builder.build();
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isSameAs(combined);
            verify(cb).and(first, second);
            verify(cb, never()).or(first, second);
        }

        @Test
        @DisplayName("criterion with orPredicate=true is combined with cb.or")
        void combinesWithOr() {
            Predicate first = stubEquality("status", "active");
            Predicate second = stubEquality("level", "senior");
            Predicate combined = mock(Predicate.class);
            when(cb.or(first, second)).thenReturn(combined);

            GenericSpecificationBuilder<Object> builder = new GenericSpecificationBuilder<>();
            builder.with("status", EQUALITY, "active");
            builder.with("level", EQUALITY, "senior", true);

            Specification<Object> spec = builder.build();
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isSameAs(combined);
            verify(cb).or(first, second);
            verify(cb, never()).and(first, second);
        }

        @Test
        @DisplayName("mixed criteria combine left-to-right: ((A AND B) OR C)")
        void combinesLeftToRight() {
            Predicate pa = stubEquality("a", "1");
            Predicate pb = stubEquality("b", "2");
            Predicate pc = stubEquality("c", "3");
            Predicate pab = mock(Predicate.class);
            Predicate pabc = mock(Predicate.class);
            when(cb.and(pa, pb)).thenReturn(pab);
            when(cb.or(pab, pc)).thenReturn(pabc);

            GenericSpecificationBuilder<Object> builder = new GenericSpecificationBuilder<>();
            builder.with("a", EQUALITY, "1");
            builder.with("b", EQUALITY, "2", false);
            builder.with("c", EQUALITY, "3", true);

            Specification<Object> spec = builder.build();
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isSameAs(pabc);
            verify(cb).and(pa, pb);
            verify(cb).or(pab, pc);
        }

        @Test
        @DisplayName("CONTAINS operation builds like predicate with %value% pattern")
        void containsBuildsLikePredicate() {
            Path<String> stringPath = newPath();
            doReturn(stringPath).when(root).get("name");
            Expression<String> lowerExpression = newExpression();
            Predicate likePredicate = mock(Predicate.class);
            when(cb.lower(stringPath)).thenReturn(lowerExpression);
            when(cb.like(lowerExpression, "%john%")).thenReturn(likePredicate);

            GenericSpecificationBuilder<Object> builder = new GenericSpecificationBuilder<>();
            builder.with(null, "name", ":", "JOHN", "*", "*");

            Predicate result = builder.build().toPredicate(root, query, cb);

            assertThat(result).isSameAs(likePredicate);
            verify(cb).like(lowerExpression, "%john%");
        }
    }
}