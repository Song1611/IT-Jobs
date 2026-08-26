package com.itjob.unit.specification;

import com.itjob.specification.helper.TypeConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit - TypeConverter")
class TypeConverterTest {

    private final TypeConverter converter = new TypeConverter();

    @Nested
    @DisplayName("convertAuto")
    class ConvertAuto {

        @Test
        @DisplayName("null -> null")
        void nullReturnsNull() {
            assertThat(converter.convertAuto(null)).isNull();
        }

        @Test
        @DisplayName("numeric string within int range -> Integer")
        void convertsToInteger() {
            Object result = converter.convertAuto("123");

            assertThat(result).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.INTEGER)
                    .isEqualTo(123);
        }

        @Test
        @DisplayName("numeric string beyond int range -> Long")
        void convertsToLong() {
            Object result = converter.convertAuto("2147483648");

            assertThat(result).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LONG)
                    .isEqualTo(2147483648L);
        }

        @Test
        @DisplayName("decimal string -> Double")
        void convertsToDouble() {
            Object result = converter.convertAuto("3.14");

            assertThat(result).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.DOUBLE)
                    .isEqualTo(3.14);
        }

        @Test
        @DisplayName("boolean string -> Boolean regardless of case")
        void convertsToBoolean() {
            assertThat(converter.convertAuto("true")).isEqualTo(Boolean.TRUE);
            assertThat(converter.convertAuto("FALSE")).isEqualTo(Boolean.FALSE);
            assertThat(converter.convertAuto("True")).isEqualTo(Boolean.TRUE);
        }

        @Test
        @DisplayName("ISO date string -> LocalDate")
        void convertsToLocalDate() {
            Object result = converter.convertAuto("2024-01-15");

            assertThat(result).isEqualTo(LocalDate.of(2024, 1, 15));
        }

        @Test
        @DisplayName("non-parseable text -> original String")
        void fallsBackToString() {
            assertThat(converter.convertAuto("hello")).isEqualTo("hello");
            assertThat(converter.convertAuto("12abc")).isEqualTo("12abc");
        }

        @Test
        @DisplayName("empty string -> empty String")
        void emptyStringStaysString() {
            assertThat(converter.convertAuto("")).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("convertListAuto")
    class ConvertListAuto {

        @Test
        @DisplayName("null -> empty list")
        void nullReturnsEmptyList() {
            assertThat(converter.convertListAuto(null)).isEmpty();
        }

        @Test
        @DisplayName("comma-separated strings -> list of Strings")
        void splitsIntoStrings() {
            List<Object> result = converter.convertListAuto("active,pending");

            assertThat(result).containsExactly("active", "pending");
        }

        @Test
        @DisplayName("comma-separated numbers -> list of Integers")
        void convertsNumericItems() {
            List<Object> result = converter.convertListAuto("18,60");

            assertThat(result).containsExactly(18, 60);
        }

        @Test
        @DisplayName("trims whitespace around items")
        void trimsItems() {
            List<Object> result = converter.convertListAuto(" a , b ");

            assertThat(result).containsExactly("a", "b");
        }
    }

    @Nested
    @DisplayName("parseBetweenValueAuto")
    class ParseBetweenValueAuto {

        @Test
        @DisplayName("null -> empty array")
        void nullReturnsEmptyArray() {
            assertThat(converter.parseBetweenValueAuto(null)).isEmpty();
        }

        @Test
        @DisplayName("min,max -> array of two converted values")
        void parsesMinMax() {
            Object[] result = converter.parseBetweenValueAuto("18,60");

            assertThat(result).hasSize(2);
            assertThat(result[0]).isEqualTo(18);
            assertThat(result[1]).isEqualTo(60);
        }

        @Test
        @DisplayName("value without comma -> empty array")
        void missingCommaReturnsEmptyArray() {
            assertThat(converter.parseBetweenValueAuto("abc")).isEmpty();
        }

        @Test
        @DisplayName("more than two parts -> empty array")
        void tooManyPartsReturnsEmptyArray() {
            assertThat(converter.parseBetweenValueAuto("1,2,3")).isEmpty();
        }
    }
}