package com.sliit.vehiclebiddingsystem.converter;

import com.sliit.vehiclebiddingsystem.entity.Auction;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class AuctionStatusConverter implements AttributeConverter<Auction.Status, String> {

    @Override
    public String convertToDatabaseColumn(Auction.Status attribute) {
        if (attribute == null) {
            return null;
        }
        // Convert enum to title case for database storage
        return attribute.name().charAt(0) + attribute.name().substring(1).toLowerCase();
    }

    @Override
    public Auction.Status convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            // Convert title case from database to uppercase enum
            return Auction.Status.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}




