package com.sliit.vehiclebiddingsystem.converter;

import com.sliit.vehiclebiddingsystem.entity.VehicleListing.Condition;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ConditionConverter implements AttributeConverter<Condition, String> {
    
    @Override
    public String convertToDatabaseColumn(Condition condition) {
        if (condition == null) {
            return null;
        }
        // Convert enum to title case for database storage
        return condition.name().charAt(0) + condition.name().substring(1).toLowerCase();
    }
    
    @Override
    public Condition convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            // Convert title case from database to uppercase enum
            return Condition.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
