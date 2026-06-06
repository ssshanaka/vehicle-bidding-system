package com.sliit.vehiclebiddingsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostAuctionDTO {
    
    private Long auctionId;
    private VehicleListingDTO listing;
    private WinnerDTO winner;
    private Double highestBid;
    private Integer totalBids;
    private Integer participantCount;
    
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
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WinnerDTO {
        private Long userId;
        private String username;
        private String email;
    }
}




