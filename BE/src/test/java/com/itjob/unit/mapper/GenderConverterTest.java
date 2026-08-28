package com.itjob.unit.mapper;

import com.itjob.converter.GenderConverter;
import com.itjob.enums.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit - GenderConverter")
class GenderConverterTest {

    private final GenderConverter converter = new GenderConverter();

    @Test
    @DisplayName("convertToDatabaseColumn -> lowercases the enum name")
    void toDatabaseColumnLowercases() {
        assertThat(converter.convertToDatabaseColumn(Gender.MALE)).isEqualTo("male");
        assertThat(converter.convertToDatabaseColumn(Gender.FEMALE)).isEqualTo("female");
        assertThat(converter.convertToDatabaseColumn(Gender.OTHER)).isEqualTo("other");
    }

    @Test
    @DisplayName("convertToDatabaseColumn -> null stays null")
    void toDatabaseColumnNullStaysNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    @DisplayName("convertToEntityAttribute -> uppercases the db value")
    void toEntityAttributeUppercases() {
        assertThat(converter.convertToEntityAttribute("male")).isEqualTo(Gender.MALE);
        assertThat(converter.convertToEntityAttribute("MALE")).isEqualTo(Gender.MALE);
        assertThat(converter.convertToEntityAttribute("female")).isEqualTo(Gender.FEMALE);
    }

    @Test
    @DisplayName("convertToEntityAttribute -> null stays null")
    void toEntityAttributeNullStaysNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    @DisplayName("roundtrip -> db value survives a column -> entity -> column cycle")
    void roundTripIsStable() {
        Gender original = Gender.MALE;
        String db = converter.convertToDatabaseColumn(original);
        assertThat(converter.convertToEntityAttribute(db)).isEqualTo(original);
    }
}