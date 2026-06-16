package com.sliit.vehiclebiddingsystem.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.entity.Bid;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.BidRepository;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;
import com.sliit.vehiclebiddingsystem.service.AuctionService;
import com.sliit.vehiclebiddingsystem.service.BidService;

import jakarta.validation.Valid;

@Controller
public class BidController {

    private static final Logger logger = LoggerFactory.getLogger(BidController.class);

    @Autowired
    private BidService bidService;

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BidRepository bidRepository;

    // User: Place bid form
    @GetMapping("/auctions/place-bid/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('SALES_MANAGER') or hasRole('CUSTOMER_SERVICE') or hasRole('VEHICLE_INSPECTOR')")
    public String placeBidForm(@PathVariable Long id, Model model) {
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            logger.debug("Authentication object: {}", authentication);
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("User not authenticated for place-bid page");
                model.addAttribute("errorMessage", "You must be logged in to place a bid.");
                model.addAttribute("currentUsername", null);
                return "place-bid";
            }
            String username = authentication.getName();
            logger.debug("Authenticated username: {}", username);
            User user = userRepository.findByUsername(username);
            
            // Check if user is banned
            if (user != null && user.isBanned()) {
                model.addAttribute("userBanned", true);
                model.addAttribute("currentUsername", username);
                return "place-bid";
            }
            
            // Get auction - this will throw RuntimeException if not found
            logger.debug("Attempting to get auction with ID: {}", id);
            Auction auction = auctionService.getAuctionById(id);
            logger.debug("Successfully retrieved auction: {}", auction.getAuctionId());
            
            // Validate auction has required data
            if (auction.getListing() == null) {
                logger.warn("Auction {} has null listing", id);
                model.addAttribute("errorMessage", "Auction data is incomplete. Please contact support.");
                return "place-bid";
            }
            
            // Populate bid count for display
            Long bidCount = bidRepository.countByAuctionAuctionId(id);
            auction.setBidCount(bidCount);
            
            // Check if auction is active
            if (!Auction.Status.ACTIVE.equals(auction.getStatus())) {
                model.addAttribute("auctionClosed", true);
                model.addAttribute("auction", auction);
                return "place-bid";
            }
            
            // Check if auction has ended
            if (auction.getCurrentEndTime() != null && auction.getCurrentEndTime().isBefore(java.time.LocalDateTime.now())) {
                model.addAttribute("auctionClosed", true);
                model.addAttribute("auction", auction);
                return "place-bid";
            }

            model.addAttribute("auction", auction);
            model.addAttribute("bidForm", new Bid());
            model.addAttribute("userBanned", false);
            model.addAttribute("auctionClosed", false);
            model.addAttribute("currentUser", user);
            model.addAttribute("currentUsername", username);
            return "place-bid";
            
        } catch (RuntimeException e) {
            // Handle auction not found or other runtime exceptions
            logger.error("RuntimeException in GET place-bid for auction {}: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "Auction not found or is no longer available.");
            model.addAttribute("currentUsername", null);
            return "place-bid";
        } catch (Exception e) {
            // Handle any other unexpected errors
            logger.error("Unexpected error in GET place-bid for auction {}: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while loading the auction. Please try again.");
            model.addAttribute("currentUsername", null);
            return "place-bid";
        }
    }

    // User: Submit bid
    @PostMapping("/auctions/place-bid/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('SALES_MANAGER') or hasRole('CUSTOMER_SERVICE') or hasRole('VEHICLE_INSPECTOR')")
    public String placeBid(@PathVariable Long id, @Valid @ModelAttribute("bidForm") Bid bid,
                           BindingResult result, Model model) {
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                model.addAttribute("errorMessage", "You must be logged in to place a bid.");
                return "place-bid";
            }
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);
            
            // Check if user is banned
            if (user != null && user.isBanned()) {
                model.addAttribute("userBanned", true);
                return "place-bid";
            }
            
            // Get auction - this will throw RuntimeException if not found
            Auction auction = auctionService.getAuctionById(id);
            
            // Validate auction has required data
            if (auction.getListing() == null) {
                model.addAttribute("errorMessage", "Auction data is incomplete. Please contact support.");
                return "place-bid";
            }
            
            // Populate bid count for display
            Long bidCount = bidRepository.countByAuctionAuctionId(id);
            auction.setBidCount(bidCount);
            
            // Check if auction is active
            if (!Auction.Status.ACTIVE.equals(auction.getStatus())) {
                model.addAttribute("auctionClosed", true);
                model.addAttribute("auction", auction);
                model.addAttribute("bidForm", new Bid());
                return "place-bid";
            }
            
            // Check if auction has ended
            if (auction.getCurrentEndTime() != null && auction.getCurrentEndTime().isBefore(java.time.LocalDateTime.now())) {
                model.addAttribute("auctionClosed", true);
                model.addAttribute("auction", auction);
                model.addAttribute("bidForm", new Bid());
                return "place-bid";
            }
            
            // Handle validation errors
            if (result.hasErrors()) {
                model.addAttribute("auction", auction);
                model.addAttribute("userBanned", false);
                model.addAttribute("auctionClosed", false);
                return "place-bid";
            }

            // Set bid details
            bid.setBidder(user);
            bid.setAuction(auction);
            bid.setListing(auction.getListing());

            // Place the bid
            try {
                bidService.placeBid(bid);
                logger.info("Bid placed successfully for auction {} by user {}", id, username);
                
                // Try to refresh auction data to get updated highest bid
                try {
                    auction = auctionService.getAuctionById(id);
                    Long updatedBidCount = bidRepository.countByAuctionAuctionId(id);
                    auction.setBidCount(updatedBidCount);
                } catch (RuntimeException refreshException) {
                    // If we can't refresh the auction data, log the error but don't fail
                    logger.warn("Could not refresh auction data after successful bid placement for auction {}: {}", id, refreshException.getMessage());
                    // Keep the original auction data
                }
                
                // Add success message and updated auction data
                model.addAttribute("successMessage", "Bid placed successfully! Your bid of LKR " + 
                    String.format("%.2f", bid.getAmount()) + " has been recorded.");
                model.addAttribute("auction", auction);
                model.addAttribute("bidForm", new Bid());
                model.addAttribute("userBanned", false);
                model.addAttribute("auctionClosed", false);
                model.addAttribute("currentUser", user);
                model.addAttribute("currentUsername", username);
                
                // Stay on the same page with updated data
                return "place-bid";
            } catch (Exception e) {
                logger.error("Error placing bid for auction {} by user {}: {}", id, username, e.getMessage(), e);
                throw e; // Re-throw to be caught by outer catch blocks
            }
            
        } catch (IllegalArgumentException e) {
            // Handle bid validation errors (e.g., bid too low)
            try {
                Auction auction = auctionService.getAuctionById(id);
                Long bidCount = bidRepository.countByAuctionAuctionId(id);
                auction.setBidCount(bidCount);
                model.addAttribute("auction", auction);
                // Get current username from authentication context
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                model.addAttribute("currentUsername", auth != null ? auth.getName() : null);
            } catch (Exception ex) {
                // If we can't load auction, just show error
                model.addAttribute("auction", null);
                model.addAttribute("currentUsername", null);
            }
            model.addAttribute("userBanned", false);
            model.addAttribute("auctionClosed", false);
            model.addAttribute("errorMessage", e.getMessage());
            return "place-bid";
            
        } catch (RuntimeException e) {
            // Handle auction not found or other runtime exceptions
            logger.error("RuntimeException in POST place-bid for auction {}: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "Auction not found or is no longer available.");
            model.addAttribute("auction", null);
            model.addAttribute("currentUsername", null);
            return "place-bid";
            
        } catch (Exception e) {
            // Handle other errors
            logger.error("Unexpected error in POST place-bid for auction {}: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while placing your bid. Please try again.");
            model.addAttribute("auction", null);
            model.addAttribute("currentUsername", null);
            return "place-bid";
        }
    }
}