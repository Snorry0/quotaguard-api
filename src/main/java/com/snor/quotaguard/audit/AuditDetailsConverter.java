package com.snor.quotaguard.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

@Converter
public class AuditDetailsConverter implements AttributeConverter<Map<String, String>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(Map<String, String> details) {
        if (details == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(details);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialize audit details", ex);
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String columnValue) {
        if (columnValue == null || columnValue.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(columnValue, MAP_TYPE);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not deserialize audit details", ex);
        }
    }
}
