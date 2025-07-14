package com.sliit.vehiclebiddingsystem.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.entity.Auction.Status;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.AuctionRepository;
import com.sliit.vehiclebiddingsystem.repository.BidRepository;

@Service
public class AuctionService {

    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationService notificationService;

    public List<Auction> getAllAuctions() {
        List<Auction> auctions = auctionRepository.findAll();
        updateHighestBids(auctions);
        return auctions;
    }

    public List<Auction> getActiveAuctions() {
        List<Auction> auctions = auctionRepository.findByStatus(Status.ACTIVE);
        updateHighestBids(auctions);
        return auctions;
    }

    public List<Auction> getAuctionsBySeller(User seller) {
        List<Auction> auctions = auctionRepository.findBySeller(seller);
        updateHighestBids(auctions);
        return auctions;
    }

    private void updateHighestBids(List<Auction> auctions) {
        for (Auction auction : auctions) {
            Double highest = bidRepository.findHighestBidByAuctionId(auction.getAuctionId());
            auction.setHighestBid(highest != null ? highest : 0.0);
        }
    }

    public Auction getAuctionById(Long id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        Double highest = bidRepository.findHighestBidByAuctionId(auction.getAuctionId());
        auction.setHighestBid(highest != null ? highest : 0.0);

        return auction;
    }

    public Auction createAuction(Auction auction) {
        if (auction.getEndTime().isBefore(auction.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        if (!"APPROVED".equals(auction.getListing().getStatus())) {
            throw new IllegalArgumentException("Vehicle listing must be approved before creating auction");
        }

        auction.setStatus(Status.PENDING);
        auction.setCurrentEndTime(auction.getEndTime()); // Set initial current end time

        Auction saved = auctionRepository.save(auction);
        broadcastAuctionUpdate(saved);

        return saved;
    }

    public Auction updateAuction(Auction auction) {
        if (auction.getEndTime().isBefore(auction.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        Auction updated = auctionRepository.save(auction);
        broadcastAuctionUpdate(updated);
        return updated;
    }

    public Auction startAuction(Long id) {
        Auction auction = getAuctionById(id);

        if (Status.PENDING.equals(auction.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            auction.setStartTime(now);
            auction.setCurrentEndTime(auction.getEndTime());
            auction.setStatus(Status.ACTIVE);

            Auction updated = auctionRepository.save(auction);
            broadcastAuctionUpdate(updated);
            return updated;
        }

        return auction;
    }

    public void endAuction(Long id) {
        Auction auction = getAuctionById(id);

        if (Status.ACTIVE.equals(auction.getStatus())) {
            auction.setStatus(Status.CLOSED);
            auction.setWinner(bidRepository.findHighestBidderByAuctionId(id));

            Auction updated = auctionRepository.save(auction);
            broadcastAuctionUpdate(updated);
            
            // Send notifications
            notificationService.createAuctionClosureNotification(id);
            if (updated.getWinner() != null) {
                notificationService.createWinnerNotification(id);
            }
        }
    }

    public void extendAuctionIfNeeded(Long id, LocalDateTime bidTime) {
        Auction auction = getAuctionById(id);
        extendAuctionIfNeeded(auction, bidTime);
    }
    
    public void extendAuctionIfNeeded(Auction auction, LocalDateTime bidTime) {
        if (Status.ACTIVE.equals(auction.getStatus())) {
            LocalDateTime threshold = auction.getCurrentEndTime().minusSeconds(30);

            if (bidTime.isAfter(threshold)) {
                auction.setCurrentEndTime(auction.getCurrentEndTime().plusSeconds(auction.getExtensionDuration()));

                Auction updated = auctionRepository.save(auction);
                broadcastAuctionUpdate(updated);

                logger.info("Auction {} extended to {}", auction.getAuctionId(), auction.getCurrentEndTime());
            }
        }
    }

    // Scheduled job: End expired auctions every 1 min
    @Scheduled(fixedRate = 60000)
    public void checkAndEndExpiredAuctions() {
        List<Auction> activeAuctions = getActiveAuctions();
        LocalDateTime now = LocalDateTime.now();

        for (Auction auction : activeAuctions) {
            if (auction.getCurrentEndTime().isBefore(now)) {
                endAuction(auction.getAuctionId());
            }
        }
    }

    // Scheduled job: Start scheduled approved auctions
    @Scheduled(fixedRate = 60000)
    public void checkAndStartScheduledAuctions() {
        List<Auction> pendingAuctions = auctionRepository.findByStatus(Status.PENDING);
        LocalDateTime now = LocalDateTime.now();

        for (Auction auction : pendingAuctions) {
            if (auction.getStartTime().isBefore(now)) {
                startAuction(auction.getAuctionId());
            }
        }
    }

    public List<Auction> getAuctionsByStatus(Status status) {
        List<Auction> auctions = auctionRepository.findByStatus(status);
        updateHighestBids(auctions);
        return auctions;
    }

    public List<Auction> getUpcomingAuctions() {
        List<Auction> auctions = auctionRepository.findByStatus(Status.PENDING);
        LocalDateTime now = LocalDateTime.now();
        return auctions.stream()
                .filter(auction -> auction.getStartTime().isAfter(now))
                .toList();
    }

    public boolean isAuctionActive(Long auctionId) {
        Auction auction = getAuctionById(auctionId);
        return Status.ACTIVE.equals(auction.getStatus()) && 
               auction.getCurrentEndTime().isAfter(LocalDateTime.now());
    }

    public long getTimeRemaining(Long auctionId) {
        Auction auction = getAuctionById(auctionId);
        if (!Status.ACTIVE.equals(auction.getStatus())) {
            return 0;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (auction.getCurrentEndTime().isBefore(now)) {
            return 0;
        }
        
        return java.time.Duration.between(now, auction.getCurrentEndTime()).getSeconds();
    }

