package com.itjob.converter;
import com.itjob.entity.Gender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GenderConverter implements AttributeConverter<Gender, String> {

    @Override
    public String convertToDatabaseColumn(Gender gender) {
        return gender == null ? null : gender.name().toLowerCase();
    }

    @Override
    public Gender convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Gender.valueOf(dbData.toUpperCase());
    }
}
