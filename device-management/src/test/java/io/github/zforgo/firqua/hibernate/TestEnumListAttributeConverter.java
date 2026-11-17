package io.github.zforgo.firqua.hibernate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;

class TestEnumListAttributeConverter implements AttributeConverter<List<TestEnum>, String> {

    private static final String separator = ";";

    @Override
    public String convertToDatabaseColumn(List<TestEnum> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute.stream().map(TestEnum::name).collect(Collectors.joining(separator));
    }

    @Override
    public List<TestEnum> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return Arrays.stream(dbData.split(separator))
                .map(String::trim)
                .map(TestEnum::valueOf)
                .toList();
    }
}
