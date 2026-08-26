package com.itjob.unit.specification;

import com.itjob.specification.helper.FilterValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit - FilterValidator")
class FilterValidatorTest {

    private final FilterValidator validator = new FilterValidator();

    @Nested
    @DisplayName("isValidFilter")
    class IsValidFilter {

        @Test
        @DisplayName("well-formed filter -> true")
        void acceptsWellFormedFilter() {
            assertThat(validator.isValidFilter("name:John")).isTrue();
        }

        @Test
        @DisplayName("all supported operators -> true")
        void acceptsAllSupportedOperators() {
            assertThat(validator.isValidFilter("age>=18")).isTrue();
            assertThat(validator.isValidFilter("age<=60")).isTrue();
            assertThat(validator.isValidFilter("age>18")).isTrue();
            assertThat(validator.isValidFilter("age<60")).isTrue();
            assertThat(validator.isValidFilter("name~john")).isTrue();
            assertThat(validator.isValidFilter("status@active,pending")).isTrue();
            assertThat(validator.isValidFilter("salary#1000,5000")).isTrue();
            assertThat(validator.isValidFilter("status!inactive")).isTrue();
        }

        @Test
        @DisplayName("null filter -> false")
        void nullFilterIsInvalid() {
            assertThat(validator.isValidFilter(null)).isFalse();
        }

        @Test
        @DisplayName("empty filter -> false")
        void emptyFilterIsInvalid() {
            assertThat(validator.isValidFilter("")).isFalse();
        }

        @Test
        @DisplayName("filter without operator -> false")
        void missingOperatorIsInvalid() {
            assertThat(validator.isValidFilter("name")).isFalse();
        }

        @Test
        @DisplayName("field name starting with digit -> false")
        void fieldNameStartingWithDigitIsInvalid() {
            assertThat(validator.isValidFilter("1name:x")).isFalse();
        }

        @Test
        @DisplayName("value containing SQL keyword -> false")
        void rejectsSqlKeywordsInValue() {
            assertThat(validator.isValidFilter("name:select * from users")).isFalse();
            assertThat(validator.isValidFilter("status:DROP TABLE users")).isFalse();
            assertThat(validator.isValidFilter("name:UNION SELECT")).isFalse();
        }

        @Test
        @DisplayName("value containing script tag -> false")
        void rejectsScriptTagInValue() {
            assertThat(validator.isValidFilter("comment:<script>alert</script>")).isFalse();
        }

        @Test
        @DisplayName("normal text value -> true")
        void acceptsNormalTextValue() {
            assertThat(validator.isValidFilter("title:senior java developer")).isTrue();
        }
    }

    @Nested
    @DisplayName("sanitizeValue")
    class SanitizeValue {

        @Test
        @DisplayName("null -> null")
        void nullStaysNull() {
            assertThat(validator.sanitizeValue(null)).isNull();
        }

        @Test
        @DisplayName("removes quotes, semicolons and backslashes")
        void stripsDangerousCharacters() {
            assertThat(validator.sanitizeValue("O'Brien")).isEqualTo("OBrien");
            assertThat(validator.sanitizeValue("a;b\"c\\d")).isEqualTo("abcd");
        }

        @Test
        @DisplayName("keeps normal text unchanged")
        void keepsNormalText() {
            assertThat(validator.sanitizeValue("senior java developer"))
                    .isEqualTo("senior java developer");
        }
    }
}