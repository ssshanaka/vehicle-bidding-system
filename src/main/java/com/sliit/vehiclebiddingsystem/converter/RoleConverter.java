package com.sliit.vehiclebiddingsystem.converter;

import com.sliit.vehiclebiddingsystem.entity.User.Role;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role attribute) {
        if (attribute == null) {
            return null;
        }
        // Convert enum to lowercase for database storage
        return attribute.name().toLowerCase();
    }

    @Override
    public Role convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        
        // Handle migration from old role values to new ones
        String upperCaseData = dbData.toUpperCase();
        
        // Map old "ADMIN" role to "ADMIN_OFFICER" for backward compatibility
        if ("ADMIN".equals(upperCaseData)) {
            return Role.ADMIN_OFFICER;
        }
        
        try {
            return Role.valueOf(upperCaseData);
        } catch (IllegalArgumentException e) {
            // If role doesn't exist, default to USER
            return Role.USER;
        }
    }
}