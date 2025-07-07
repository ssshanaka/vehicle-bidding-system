package com.sliit.vehiclebiddingsystem.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.service.AuctionService;

@Controller
public class AuctionWebSocketController {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/auctions/{auctionId}/subscribe")
    @SendTo("/topic/auctions/{auctionId}")
    public Map<String, Object> subscribeToAuction(Long auctionId) {
        try {
            if (auctionId == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Auction ID is required");
                error.put("timestamp", System.currentTimeMillis());
                return error;
            }
            
            Auction auction = auctionService.getAuctionById(auctionId);
            if (auction == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Auction not found");
                error.put("auctionId", auctionId);
                error.put("timestamp", System.currentTimeMillis());
                return error;
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("auctionId", auction.getAuctionId());
            response.put("status", auction.getStatus().name());
            response.put("isActive", auctionService.isAuctionActive(auctionId));
            response.put("timeRemaining", auctionService.getTimeRemaining(auctionId));
            response.put("highestBid", auction.getHighestBid());
            response.put("currentEndTime", auction.getCurrentEndTime());
            response.put("extensionDuration", auction.getExtensionDuration());
            response.put("timestamp", System.currentTimeMillis());
            
            return response;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get auction data: " + e.getMessage());
            error.put("auctionId", auctionId);
            error.put("timestamp", System.currentTimeMillis());
            return error;
        }
    }

