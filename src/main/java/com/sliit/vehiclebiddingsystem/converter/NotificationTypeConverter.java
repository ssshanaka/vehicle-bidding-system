package com.sliit.vehiclebiddingsystem.converter;

import com.sliit.vehiclebiddingsystem.entity.Notification.Type;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class NotificationTypeConverter implements AttributeConverter<Type, String> {
    
    @Override
    public String convertToDatabaseColumn(Type type) {
        if (type == null) {
            return null;
        }
        // Convert enum to title case for database storage
        return type.name().charAt(0) + type.name().substring(1).toLowerCase();
    }
    
    @Override
    public Type convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            // Convert title case from database to uppercase enum
            return Type.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
