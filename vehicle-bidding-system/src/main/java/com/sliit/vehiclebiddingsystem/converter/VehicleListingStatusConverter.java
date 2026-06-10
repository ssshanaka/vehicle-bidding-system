package com.sliit.vehiclebiddingsystem.converter;

import com.sliit.vehiclebiddingsystem.entity.VehicleListing;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class VehicleListingStatusConverter implements AttributeConverter<VehicleListing.Status, String> {

    @Override
    public String convertToDatabaseColumn(VehicleListing.Status attribute) {
        if (attribute == null) {
            return null;
        }
        // Convert enum to title case for database storage
        return attribute.name().charAt(0) + attribute.name().substring(1).toLowerCase();
    }

    @Override
    public VehicleListing.Status convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            // Convert title case from database to uppercase enum
            return VehicleListing.Status.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}




