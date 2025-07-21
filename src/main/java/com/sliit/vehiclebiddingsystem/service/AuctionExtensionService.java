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

    /**
     * Extend auction by the configured extension duration
     */
    private void extendAuction(Auction auction, LocalDateTime now) {
        int extensionSeconds = auction.getExtensionDuration() != null ? auction.getExtensionDuration() : 30;
        LocalDateTime newEndTime = auction.getEndTime().plusSeconds(extensionSeconds);
        
        // Update the auction end time
        auction.setEndTime(newEndTime);
        auction.setCurrentEndTime(newEndTime);
        auctionRepository.save(auction);
        
        // Log the auto-extension
        auditLogService.logSystemAction("AUTO_EXTEND_AUCTION", auction.getAuctionId(), "AUCTION", 
            "Auction auto-extended by " + extensionSeconds + " seconds due to last-second bid activity");
        
        System.out.println("Auto-extended auction " + auction.getAuctionId() + " by " + extensionSeconds + " seconds");
    }

    /**
     * Manually extend an auction (called by sales manager)
     */
    public void manuallyExtendAuction(Long auctionId, int extensionMinutes, String reason) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        if (auction == null || auction.getStatus() != Status.ACTIVE) {
            throw new IllegalArgumentException("Auction not found or not active");
        }
        
        LocalDateTime newEndTime = auction.getEndTime().plusMinutes(extensionMinutes);
        auction.setEndTime(newEndTime);
        auction.setCurrentEndTime(newEndTime);
        auctionRepository.save(auction);
        
        // Log the manual extension
        auditLogService.logSystemAction("MANUAL_EXTEND_AUCTION", auctionId, "AUCTION", 
            "Auction manually extended by " + extensionMinutes + " minutes. Reason: " + reason);
    }

    /**
     * Check for auctions that have ended and close them
     * Runs every minute
     */
    @Scheduled(fixedRate = 60000)
    public void closeExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> activeAuctions = auctionRepository.findByStatus(Status.ACTIVE);
        
        for (Auction auction : activeAuctions) {
            if (auction.getEndTime().isBefore(now)) {
                closeAuction(auction);
            }
        }
    }

    /**
     * Close an expired auction
     */
    private void closeAuction(Auction auction) {
        auction.setStatus(Status.CLOSED);
        
        // Ensure end time is after start time to satisfy database constraint
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = auction.getStartTime();
        LocalDateTime endTime = now.isAfter(startTime) ? now : startTime.plusMinutes(1);
        
        auction.setEndTime(endTime);
        auction.setCurrentEndTime(endTime);
        auctionRepository.save(auction);
        
        // Determine winner (highest bidder)
        List<Bid> bids = bidRepository.findByAuctionAuctionIdOrderByAmountDesc(auction.getAuctionId());
        if (!bids.isEmpty()) {
            Bid winningBid = bids.get(0);
            auction.setWinner(winningBid.getBidder());
            auctionRepository.save(auction);
        }
        
        // Send notifications for auction closure
        try {
            notificationService.createAuctionClosureNotification(auction.getAuctionId());
            if (auction.getWinner() != null) {
                notificationService.createWinnerNotification(auction.getAuctionId());
            }
        } catch (Exception e) {
            // Log error but don't fail the auction closure
            System.err.println("Error sending auction closure notifications: " + e.getMessage());
        }
        
        // Log the auction closure
        auditLogService.logSystemAction("CLOSE_AUCTION", auction.getAuctionId(), "AUCTION", 
            "Auction automatically closed due to end time");
        
        System.out.println("Closed auction " + auction.getAuctionId());
    }

    /**
     * Check for low engagement auctions and send alerts
     * Runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000)
    public void checkLowEngagementAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> activeAuctions = auctionRepository.findByStatus(Status.ACTIVE);
        
        for (Auction auction : activeAuctions) {
            // Check if auction has been running for at least 1 hour
            long hoursRunning = java.time.Duration.between(auction.getStartTime(), now).toHours();
            
            if (hoursRunning >= 1) {
                List<Bid> bids = bidRepository.findByAuctionAuctionIdOrderByAmountDesc(auction.getAuctionId());
                
                // Alert if fewer than 3 bids
                if (bids.size() < 3) {
                    // This would trigger a notification to the sales manager
                    System.out.println("Low engagement alert: Auction " + auction.getAuctionId() + 
                        " has only " + bids.size() + " bids after " + hoursRunning + " hours");
                }
            }
        }
    }
}
