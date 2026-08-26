package com.itjob.unit.specification;

import com.itjob.specification.helper.FilterValidator;
import com.itjob.specification.helper.SpecificationHelper;
import com.itjob.specification.helper.TypeConverter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Unit - SpecificationHelper")
class SpecificationHelperTest {

    private final SpecificationHelper helper =
            new SpecificationHelper(new FilterValidator(), new TypeConverter());

    private final Root<Object> root = newRoot();
    private final CriteriaQuery<Object> query = newQuery();
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Root<T> newRoot() {
        return (Root<T>) mock(Root.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> CriteriaQuery<T> newQuery() {
        return (CriteriaQuery<T>) mock(CriteriaQuery.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <Y> Path<Y> newPath() {
        return (Path<Y>) mock(Path.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <Y> Expression<Y> newExpression() {
        return (Expression<Y>) mock(Expression.class);
    }

    private Predicate stubEquality(String key, String value) {
        Path<Object> path = newPath();
        doReturn(path).when(root).get(key);
        Predicate predicate = mock(Predicate.class);
        when(cb.equal(path, value)).thenReturn(predicate);
        return predicate;
    }

    private Predicate runToPredicate(String[] filters) {
        Specification<Object> spec = helper.buildSpecification(filters);
        assertThat(spec).isNotNull();
        return spec.toPredicate(root, query, cb);
    }

    @Nested
    @DisplayName("structure")
    class Structure {

        @Test
        @DisplayName("null filters -> null specification")
        void nullFiltersReturnNull() {
            assertThat(helper.buildSpecification(null)).isNull();
        }

        @Test
        @DisplayName("empty filters -> null specification")
        void emptyFiltersReturnNull() {
            assertThat(helper.buildSpecification(new String[0])).isNull();
        }

        @Test
        @DisplayName("only blank filters -> null specification")
        void blankFiltersAreSkipped() {
            assertThat(helper.buildSpecification(new String[]{" ", ""})).isNull();
        }

        @Test
        @DisplayName("all filters invalid -> null specification")
        void allInvalidFiltersReturnNull() {
            assertThat(helper.buildSpecification(new String[]{"badfilter", "1x:a"})).isNull();
        }
    }

    @Nested
    @DisplayName("equality and negation predicates")
    class EqualityPredicates {

        @Test
        @DisplayName("status:active -> predicate status = 'active'")
        void equalityUsesFieldAndValue() {
            Predicate statusPredicate = stubEquality("status", "active");

            Predicate result = runToPredicate(new String[]{"status:active"});

            assertThat(result).isSameAs(statusPredicate);
            verify(root).get("status");
        }

        @Test
        @DisplayName("code:007 keeps leading zeros as String, not converted to number")
        void equalityPreservesLeadingZeros() {
            Predicate codePredicate = stubEquality("code", "007");

            Predicate result = runToPredicate(new String[]{"code:007"});

            assertThat(result).isSameAs(codePredicate);
        }

        @Test
        @DisplayName("status!inactive -> predicate status != 'inactive'")
        void negationUsesNotEqual() {
            Path<Object> path = newPath();
            doReturn(path).when(root).get("status");
            Predicate notEqualPredicate = mock(Predicate.class);
            when(cb.notEqual(path, "inactive")).thenReturn(notEqualPredicate);

            Predicate result = runToPredicate(new String[]{"status!inactive"});

            assertThat(result).isSameAs(notEqualPredicate);
        }

        @Test
        @DisplayName("quotes are sanitized before building the predicate")
        void sanitizesQuotesFromValue() {
            Predicate sanitizedPredicate = stubEquality("name", "OBrien");

            Predicate result = runToPredicate(new String[]{"name:O'Brien"});

            assertThat(result).isSameAs(sanitizedPredicate);
        }
    }

    @Nested
    @DisplayName("or predicate flag")
    class OrPredicateFlag {

        @Test
        @DisplayName("'flag on second filter combines with OR")
        void orFlagCombinesWithOr() {
            Predicate first = stubEquality("a", "1");
            Predicate second = stubEquality("b", "2");
            Predicate combined = mock(Predicate.class);
            when(cb.or(first, second)).thenReturn(combined);

            Predicate result = runToPredicate(new String[]{"a:1", "'b:2"});

            assertThat(result).isSameAs(combined);
            verify(cb).or(first, second);
            verify(cb, never()).and(first, second);
        }

        @Test
        @DisplayName("no flag combines with AND")
        void noFlagCombinesWithAnd() {
            Predicate first = stubEquality("a", "1");
            Predicate second = stubEquality("b", "2");
            Predicate combined = mock(Predicate.class);
            when(cb.and(first, second)).thenReturn(combined);

            Predicate result = runToPredicate(new String[]{"a:1", "b:2"});

            assertThat(result).isSameAs(combined);
            verify(cb).and(first, second);
            verify(cb, never()).or(first, second);
        }
    }

    @Nested
    @DisplayName("wildcard like predicates")
    class WildcardLikePredicates {

        private Predicate stubLike(String key, String expectedPattern) {
            Path<String> path = newPath();
            doReturn(path).when(root).get(key);
            Expression<String> lowerExpression = newExpression();
            Predicate likePredicate = mock(Predicate.class);
            when(cb.lower(path)).thenReturn(lowerExpression);
            when(cb.like(lowerExpression, expectedPattern)).thenReturn(likePredicate);
            return likePredicate;
        }

        @ParameterizedTest(name = "{0} -> LIKE {1}")
        @CsvSource({
                "name:*JOHN*, %john%",
                "name:john*, john%",
                "name:*john, %john"
        })
        @DisplayName("wildcard filters produce lowercased LIKE patterns")
        void wildcardsProduceLikePatterns(String filter, String expectedPattern) {
            Predicate likePredicate = stubLike("name", expectedPattern);

            Predicate result = runToPredicate(new String[]{filter});

            assertThat(result).isSameAs(likePredicate);
        }
    }

    @Nested
    @DisplayName("in predicate")
    class InPredicate {

        @Test
        @DisplayName("status@active,pending -> path in ('active','pending')")
        void inConvertsCsvToList() {
            Path<Object> path = newPath();
            doReturn(path).when(root).get("status");
            Predicate inPredicate = mock(Predicate.class);
            when(path.in(List.of("active", "pending"))).thenReturn(inPredicate);

            Predicate result = runToPredicate(new String[]{"status@active,pending"});

            assertThat(result).isSameAs(inPredicate);
        }
    }

    @Nested
    @DisplayName("sql injection filters are excluded")
    class SqlInjectionExclusion {

        @Test
        @DisplayName("SQL keyword filter never reaches the predicate, valid filter still applies")
        void sqlKeywordFilterNeverReachesPredicate() {
            Predicate statusPredicate = stubEquality("status", "active");

            Predicate result = runToPredicate(
                    new String[]{"name:select * from users", "status:active"});

            assertThat(result).isSameAs(statusPredicate);
            verify(root, never()).get("name");
        }

        @Test
        @DisplayName("script tag filter never reaches the predicate")
        void scriptTagFilterNeverReachesPredicate() {
            Predicate statusPredicate = stubEquality("status", "active");

            Predicate result = runToPredicate(
                    new String[]{"comment:<script>alert</script>", "status:active"});

            assertThat(result).isSameAs(statusPredicate);
            verify(root, never()).get("comment");
        }
    }
}