package com.sliit.vehiclebiddingsystem.converter;

import com.sliit.vehiclebiddingsystem.entity.AuditLog.ActionType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AuditLogActionTypeConverter implements AttributeConverter<ActionType, String> {
    
    @Override
    public String convertToDatabaseColumn(ActionType actionType) {
        if (actionType == null) {
            return null;
        }
        // Convert enum to title case for database storage
        return actionType.name().charAt(0) + actionType.name().substring(1).toLowerCase();
    }
    
    @Override
    public ActionType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            // Convert title case from database to uppercase enum
            return ActionType.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
