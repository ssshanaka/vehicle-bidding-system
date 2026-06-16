package com.sliit.vehiclebiddingsystem.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidDTO {
    
    private Long bidId;
    private Long auctionId;
    private Double amount;
    private LocalDateTime timestamp;
    private BidderDTO bidder;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BidderDTO {
        private Long userId;
        private String username;
        private String email;
        private String phone;
    }
}

