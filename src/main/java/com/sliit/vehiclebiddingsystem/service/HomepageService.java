package com.sliit.vehiclebiddingsystem.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.sliit.vehiclebiddingsystem.dto.HomepageAuctionDto;
import com.sliit.vehiclebiddingsystem.dto.HomepageStatsDto;
import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.entity.Bid;
import com.sliit.vehiclebiddingsystem.entity.VehicleImage;
import com.sliit.vehiclebiddingsystem.entity.VehicleListing;
import com.sliit.vehiclebiddingsystem.repository.AuctionRepository;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;
import com.sliit.vehiclebiddingsystem.repository.VehicleListingRepository;

/**
 * Service for homepage data operations
 */
@Service
public class HomepageService {

    private static final Logger logger = LoggerFactory.getLogger(HomepageService.class);

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private VehicleListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get featured auctions for homepage display
     */
    public List<HomepageAuctionDto> getFeaturedAuctions() {
        List<HomepageAuctionDto> featuredAuctions = new ArrayList<>();
        
        try {
            // Get up to 3 active (live) auctions
            List<Auction> activeAuctions = auctionRepository.findTop5ByStatusOrderByEndTimeAsc(
                Auction.Status.ACTIVE, PageRequest.of(0, 3));
            
            for (Auction auction : activeAuctions) {
                featuredAuctions.add(mapAuctionToDto(auction));
            }
            
            // Get up to 3 pending (scheduled) auctions
            List<Auction> pendingAuctions = auctionRepository.findTop3ByStatusOrderByStartTimeAsc(
                Auction.Status.PENDING, PageRequest.of(0, 3));
            
            for (Auction auction : pendingAuctions) {
                featuredAuctions.add(mapAuctionToDto(auction));
            }
            
            logger.info("Retrieved {} featured auctions ({} active, {} scheduled)", 
                featuredAuctions.size(), activeAuctions.size(), pendingAuctions.size());
            
        } catch (Exception e) {
            logger.error("Error retrieving featured auctions", e);
        }
        
        return featuredAuctions;
    }

    /**
     * Get homepage statistics
     */
    public HomepageStatsDto getHomepageStats() {
        try {
            long totalListings = listingRepository.count();
            long activeAuctions = auctionRepository.countByStatus(Auction.Status.ACTIVE);
            long totalUsers = userRepository.count();
            long completedAuctions = auctionRepository.countByStatus(Auction.Status.CLOSED);
            
            return new HomepageStatsDto(totalListings, activeAuctions, totalUsers, completedAuctions);
        } catch (Exception e) {
            logger.error("Error retrieving homepage statistics", e);
            return new HomepageStatsDto(0, 0, 0, 0);
        }
    }


}
