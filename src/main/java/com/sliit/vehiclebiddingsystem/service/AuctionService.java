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

 