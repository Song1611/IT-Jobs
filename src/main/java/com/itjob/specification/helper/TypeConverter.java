package com.itjob.specification.helper;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Component
@Slf4j
public class TypeConverter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public Object convertAuto(String value) {
        if (value == null) {
            return null;
        }

        //Integer
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}

        //Long
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {}

        //Double
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {}

        //Boolean
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }

        //LocalDate
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException ignored) {}

        //Default: String
        return value;
    }

    public List<Object> convertListAuto (String value) {
        if (value == null) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .map(this::convertAuto)
                .collect(Collectors.toList());
    }

    public Object[] parseBetweenValueAuto(String value) {
        if (value == null) {
            return new Object[0];
        }

        String[] parts = value.split(",");
        if (parts.length != 2) {
            log.warn("Invalid BETWEEN value format: {}", value);
            return new Object[0];
        }

        Object min = convertAuto(parts[0].trim());
        Object max = convertAuto(parts[1].trim());

        return new Object[]{min, max};
    }


}
