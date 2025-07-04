package com.sliit.vehiclebiddingsystem.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.entity.Bid;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.entity.VehicleListing;
import com.sliit.vehiclebiddingsystem.repository.BidRepository;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;
import com.sliit.vehiclebiddingsystem.repository.VehicleListingRepository;
import com.sliit.vehiclebiddingsystem.service.AuctionService;

import jakarta.validation.Valid;

@Controller
public class AuctionController {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private VehicleListingRepository vehicleListingRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private UserRepository userRepository;

    // Enhanced User View: List active auctions with pagination and filtering
    @GetMapping("/auctions")
    public String userAuctions(Model model, 
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "12") int size,
                              @RequestParam(required = false) String search,
                              @RequestParam(required = false) String sortBy,
                              @RequestParam(required = false) String sortDir) {
        
        try {
            // Default to active auctions
            Auction.Status status = Auction.Status.ACTIVE;
            
            // Create pageable with sorting
            Pageable pageable = createPageable(page, size, sortBy, sortDir);
            
            // Get paginated auctions - use fallback if new methods fail
            Page<Auction> auctionPage;
            try {
                if (search != null && !search.trim().isEmpty()) {
                    auctionPage = auctionService.getAuctionsByStatusAndSearchWithPagination(status, search, pageable);
                } else {
                    auctionPage = auctionService.getAuctionsByStatusWithPagination(status, pageable);
                }
            } catch (Exception e) {
                // Fallback to simple method if enhanced methods fail
                System.out.println("Enhanced pagination failed, using fallback: " + e.getMessage());
                List<Auction> allAuctions = auctionService.getActiveAuctions();
                int start = page * size;
                int end = Math.min(start + size, allAuctions.size());
                List<Auction> pageContent = allAuctions.subList(start, end);
                
                // Create a simple page implementation
                auctionPage = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, allAuctions.size());
            }
            
            // Add bid counts for each auction
            for (Auction auction : auctionPage.getContent()) {
                try {
                    long bidCount = auctionService.getBidCountForAuction(auction.getAuctionId());
                    long bidderCount = auctionService.getDistinctBidderCountForAuction(auction.getAuctionId());
                    auction.setBidCount(bidCount);
                    auction.setBidderCount(bidderCount);
                } catch (Exception e) {
                    // Set default values if bid count fails
                    auction.setBidCount(0L);
                    auction.setBidderCount(0L);
                }
            }
            
            model.addAttribute("auctions", auctionPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", auctionPage.getTotalPages());
            model.addAttribute("totalElements", auctionPage.getTotalElements());
            model.addAttribute("size", size);
            model.addAttribute("search", search);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("currentStatus", "ACTIVE");
            
            return "user-auctions";
            
        } catch (Exception e) {
            // Log the error and return a fallback
            e.printStackTrace();
            model.addAttribute("error", "Failed to load auctions: " + e.getMessage());
            model.addAttribute("auctions", java.util.Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0);
            model.addAttribute("size", size);
            model.addAttribute("search", search);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("currentStatus", "ACTIVE");
            return "user-auctions";
        }
    }

    // User: List auctions where user has placed bids
    @GetMapping("/user/auctions")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('SALES_MANAGER') or hasRole('CUSTOMER_SERVICE') or hasRole('VEHICLE_INSPECTOR')")
    public String userBiddingHistory(Model model,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "12") int size,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String search) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);
            
            if (user == null) {
                return "redirect:/login";
            }
            
            // Get auctions where user has placed bids
            List<Auction> userAuctions = bidRepository.findDistinctAuctionsByBidderUserId(user.getUserId());
            
            // Apply status filter if provided
            if (status != null && !status.isEmpty()) {
                try {
                    Auction.Status statusEnum = Auction.Status.valueOf(status.toUpperCase());
                    userAuctions = userAuctions.stream()
                        .filter(auction -> auction.getStatus() == statusEnum)
                        .collect(java.util.stream.Collectors.toList());
                } catch (IllegalArgumentException e) {
                    // Invalid status, ignore
                }
            }
            
            // Apply search filter if provided
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase();
                userAuctions = userAuctions.stream()
                    .filter(auction -> 
                        auction.getListing().getMake().toLowerCase().contains(searchLower) ||
                        auction.getListing().getModel().toLowerCase().contains(searchLower) ||
                        auction.getListing().getDescription().toLowerCase().contains(searchLower))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // Sort by timestamp (most recent first)
            userAuctions.sort((a1, a2) -> a2.getStartTime().compareTo(a1.getStartTime()));
            
            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, userAuctions.size());
            List<Auction> pageContent = userAuctions.subList(start, end);
            
            // Create page object
            org.springframework.data.domain.Page<Auction> auctionPage = 
                new org.springframework.data.domain.PageImpl<>(pageContent, 
                    org.springframework.data.domain.PageRequest.of(page, size), 
                    userAuctions.size());
            
            // Get user's bids for each auction
            List<Bid> userBids = bidRepository.findByBidderUserIdWithAuctionAndListing(user.getUserId());
            java.util.Map<Long, Bid> userHighestBids = new java.util.HashMap<>();
            java.util.Map<Long, Long> userBidCounts = new java.util.HashMap<>();
            
            for (Bid bid : userBids) {
                Long auctionId = bid.getAuction().getAuctionId();
                
                // Track highest bid per auction
                if (!userHighestBids.containsKey(auctionId) || 
                    bid.getAmount() > userHighestBids.get(auctionId).getAmount()) {
                    userHighestBids.put(auctionId, bid);
                }
                
                // Count bids per auction
                userBidCounts.put(auctionId, userBidCounts.getOrDefault(auctionId, 0L) + 1);
            }
            
            // Add user's bid information to each auction
            for (Auction auction : pageContent) {
                Long auctionId = auction.getAuctionId();
                Bid userHighestBid = userHighestBids.get(auctionId);
                Long userBidCount = userBidCounts.get(auctionId);
                
                if (userHighestBid != null) {
                    auction.setHighestBid(userHighestBid.getAmount());
                }
                auction.setBidCount(userBidCount != null ? userBidCount : 0L);
                auction.setBidderCount(1L); // User is the bidder
            }
            
            model.addAttribute("auctions", pageContent);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", auctionPage.getTotalPages());
            model.addAttribute("totalElements", auctionPage.getTotalElements());
            model.addAttribute("size", size);
            model.addAttribute("currentStatus", status);
            model.addAttribute("searchTerm", search);
            model.addAttribute("user", user);
            model.addAttribute("userBids", userBids);
            model.addAttribute("userHighestBids", userHighestBids);
            
            return "user-bidding-history";
            
        } catch (Exception e) {
            e.printStackTrace();
            // Try to get user for error page
            User user = null;
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String username = authentication.getName();
                user = userRepository.findByUsername(username);
            } catch (Exception ex) {
                // If we can't get user, continue with null
            }
            
            model.addAttribute("error", "Failed to load your bidding history: " + e.getMessage());
            model.addAttribute("auctions", java.util.Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0);
            model.addAttribute("size", size);
            model.addAttribute("currentStatus", status);
            model.addAttribute("searchTerm", search);
            model.addAttribute("user", user);
            return "user-bidding-history";
        }
    }

    // Seller: List own auctions
    @GetMapping("/seller/auctions")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('SALES_MANAGER') or hasRole('CUSTOMER_SERVICE') or hasRole('VEHICLE_INSPECTOR')")
    public String sellerAuctions(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);
        List<Auction> auctions = auctionService.getAuctionsBySeller(user);
        model.addAttribute("sellerAuctions", auctions);
        return "seller-auctions";
    }

