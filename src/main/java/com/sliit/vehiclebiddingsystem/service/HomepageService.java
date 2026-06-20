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

    /**
     * Map Auction entity to HomepageAuctionDto
     */
    private HomepageAuctionDto mapAuctionToDto(Auction auction) {
        HomepageAuctionDto dto = new HomepageAuctionDto();
        
        try {
            dto.setId(auction.getAuctionId());
            dto.setStatus(auction.getStatus().name());
            dto.setStartTime(auction.getStartTime());
            dto.setEndTime(auction.getEndTime());
            
            // Get vehicle information from the listing
            VehicleListing listing = auction.getListing();
            if (listing != null) {
                dto.setVehicleMake(listing.getMake());
                dto.setVehicleModel(listing.getModel());
                dto.setVehicleYear(listing.getYear());
                dto.setCondition(listing.getCondition().name());
                
                // Get the first image URL
                List<VehicleImage> images = listing.getImages();
                if (images != null && !images.isEmpty()) {
                    dto.setVehicleImageUrl(images.get(0).getImageUrl());
                }
            }
            
            // Get current bid information
            List<Bid> bids = auction.getBids();
            if (bids != null && !bids.isEmpty()) {
                dto.setBidCount(bids.size());
                // Get the highest bid
                Bid highestBid = bids.stream()
                    .max((b1, b2) -> b1.getAmount().compareTo(b2.getAmount()))
                    .orElse(null);
                if (highestBid != null) {
                    dto.setCurrentBid(BigDecimal.valueOf(highestBid.getAmount()));
                }
            } else {
                dto.setBidCount(0);
                // Set starting bid to 0 if no bids exist
                dto.setCurrentBid(BigDecimal.ZERO);
            }
            
            // Set starting bid to 0 for now (no starting bid field in Auction entity)
            dto.setStartingBid(BigDecimal.ZERO);
            
        } catch (Exception e) {
            logger.error("Error mapping auction to DTO for auction ID: {}", auction.getAuctionId(), e);
        }
        
        return dto;
    }
}
