package com.itjob.unit.util;

import com.itjob.util.FilterParser;
import com.itjob.util.FilterParser.FilterComponents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit - FilterParser")
class FilterParserTest {

    @Test
    @DisplayName("parses a valid equality filter")
    void parsesEquality() {
        FilterComponents c = FilterParser.parse("name:John");
        assertThat(c).isNotNull();
        assertThat(c.getFieldName()).isEqualTo("name");
        assertThat(c.getOperator()).isEqualTo(":");
        assertThat(c.getValue()).isEqualTo("John");
        assertThat(c.isValid()).isTrue();
    }

    @Test
    @DisplayName("parses various operators correctly")
    void parsesVariousOperators() {
        FilterComponents greaterEqual = FilterParser.parse("age>=18");
        assertThat(greaterEqual.getFieldName()).isEqualTo("age");
        assertThat(greaterEqual.getOperator()).isEqualTo(">=");
        assertThat(greaterEqual.getValue()).isEqualTo("18");

        FilterComponents like = FilterParser.parse("name~john");
        assertThat(like.getFieldName()).isEqualTo("name");
        assertThat(like.getOperator()).isEqualTo("~");
        assertThat(like.getValue()).isEqualTo("john");

        FilterComponents in = FilterParser.parse("status@active,pending");
        assertThat(in.getFieldName()).isEqualTo("status");
        assertThat(in.getOperator()).isEqualTo("@");
        assertThat(in.getValue()).isEqualTo("active,pending");
    }

    @Test
    @DisplayName("null -> null")
    void nullFilterReturnsNull() {
        assertThat(FilterParser.parse(null)).isNull();
    }

    @Test
    @DisplayName("empty string -> null")
    void emptyFilterReturnsNull() {
        assertThat(FilterParser.parse("")).isNull();
    }

    @Test
    @DisplayName("filter without operator -> null")
    void filterWithoutOperatorReturnsNull() {
        assertThat(FilterParser.parse("name")).isNull();
        assertThat(FilterParser.parse("abc123")).isNull();
    }

    @Test
    @DisplayName("parsed components are marked valid")
    void validComponentsAreMarkedValid() {
        FilterComponents c = FilterParser.parse("status:active");
        assertThat(c.isValid()).isTrue();
    }
}