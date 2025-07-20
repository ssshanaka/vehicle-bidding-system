package com.sliit.vehiclebiddingsystem.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.entity.Auction.Status;
import com.sliit.vehiclebiddingsystem.entity.Bid;
import com.sliit.vehiclebiddingsystem.repository.AuctionRepository;
import com.sliit.vehiclebiddingsystem.repository.BidRepository;

@Service
@Transactional
public class AuctionExtensionService {

    @Autowired
    private AuctionRepository auctionRepository;
    
    @Autowired
    private BidRepository bidRepository;
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Autowired
    private NotificationService notificationService;

    /**
     * Check for last-second bids and auto-extend auctions
     * Runs every 10 seconds
     */
    @Scheduled(fixedRate = 10000)
    public void checkForLastSecondBids() {
        LocalDateTime now = LocalDateTime.now();
        
        // Find active auctions that are ending soon (within 30 seconds)
        List<Auction> activeAuctions = auctionRepository.findByStatus(Status.ACTIVE);
        
        for (Auction auction : activeAuctions) {
            LocalDateTime endTime = auction.getEndTime();
            long secondsUntilEnd = java.time.Duration.between(now, endTime).getSeconds();
            
            // If auction is ending within 30 seconds, check for recent bids
            if (secondsUntilEnd <= 30 && secondsUntilEnd > 0) {
                checkAndExtendAuction(auction, now);
            }
        }
    }

    /**
     * Check if auction should be extended based on recent bid activity
     */
    private void checkAndExtendAuction(Auction auction, LocalDateTime now) {
        // Get the most recent bid for this auction
        List<Bid> recentBids = bidRepository.findByAuctionAuctionIdOrderByAmountDesc(auction.getAuctionId());
        
        if (!recentBids.isEmpty()) {
            Bid latestBid = recentBids.get(0);
            LocalDateTime bidTime = latestBid.getTimestamp();
            
            // If the latest bid was placed within the last 30 seconds, extend the auction
            long secondsSinceLastBid = java.time.Duration.between(bidTime, now).getSeconds();
            
            if (secondsSinceLastBid <= 30) {
                extendAuction(auction, now);
            }
        }
    }

