package com.sliit.vehiclebiddingsystem.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.sliit.vehiclebiddingsystem.entity.Bid;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.BidRepository;

@Service
public class BidService {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationService notificationService;

    public Bid placeBid(Bid bid) {
        Double currentHighest = bidRepository.findHighestBidByAuctionId(bid.getAuction().getAuctionId());
        if (currentHighest != null && bid.getAmount() <= currentHighest) {
            throw new IllegalArgumentException("Bid must be higher than current highest bid");
        }

        // Get the previous highest bidder before placing the new bid
        User previousHighestBidder = bidRepository.findHighestBidderByAuctionId(bid.getAuction().getAuctionId());

        Bid saved = bidRepository.save(bid);
        
        // Send outbid notification to previous highest bidder if they exist and are different from current bidder
        if (previousHighestBidder != null && !previousHighestBidder.getUserId().equals(bid.getBidder().getUserId())) {
            try {
                notificationService.createOutbidNotification(
                    bid.getAuction().getAuctionId(), 
                    previousHighestBidder.getUserId(), 
                    bid.getAmount()
                );
            } catch (Exception e) {
                // Log error but don't fail the bid placement
                System.err.println("Error sending outbid notification: " + e.getMessage());
            }
        }
        
        // Extend auction if needed using the existing auction object
        try {
            auctionService.extendAuctionIfNeeded(bid.getAuction(), LocalDateTime.now());
        } catch (Exception e) {
            // Log the error but don't fail the bid placement
            System.err.println("Error extending auction: " + e.getMessage());
        }

        // Broadcast bid update
        messagingTemplate.convertAndSend("/topic/auctions/" + bid.getAuction().getAuctionId() + "/bids", saved);

        return saved;
    }
}