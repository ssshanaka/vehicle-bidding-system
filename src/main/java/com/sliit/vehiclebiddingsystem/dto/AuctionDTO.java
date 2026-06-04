package com.sliit.vehiclebiddingsystem.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuctionDTO {
    
    private Long auctionId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime currentEndTime;
    private Integer extensionDuration;
    private Long winnerId;
    private String winnerUsername;
    private VehicleListingDTO listing;
    private Double highestBid;
    private Integer bidderCount;
    private Long totalBids;
    
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
        private String transmission;
        private String fuelType;
        private String description;
        private String inspectorNotes;
        private String rejectionReason;
        private Long sellerId;
        private String sellerUsername;
    }
}




