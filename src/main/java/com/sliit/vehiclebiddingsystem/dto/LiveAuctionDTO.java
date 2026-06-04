package com.sliit.vehiclebiddingsystem.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiveAuctionDTO {
    
    private Long auctionId;
    private LocalDateTime endTime;
    private Double highestBid;
    private VehicleListingDTO listing;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleListingDTO {
        private Long listingId;
        private String make;
        private String model;
        private Integer year;
        private Integer mileage;
        private String condition;
        private String status;
    }
}




