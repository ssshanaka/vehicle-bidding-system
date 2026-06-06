package com.sliit.vehiclebiddingsystem.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sliit.vehiclebiddingsystem.dto.AuctionDTO;
import com.sliit.vehiclebiddingsystem.dto.BidDTO;
import com.sliit.vehiclebiddingsystem.dto.LiveAuctionDTO;
import com.sliit.vehiclebiddingsystem.dto.PostAuctionDTO;
import com.sliit.vehiclebiddingsystem.dto.VehicleListingDTO;
import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.entity.Auction.Status;
import com.sliit.vehiclebiddingsystem.entity.AuditLog;
import com.sliit.vehiclebiddingsystem.entity.Bid;
import com.sliit.vehiclebiddingsystem.entity.Notification;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.entity.VehicleListing;
import com.sliit.vehiclebiddingsystem.repository.AuctionRepository;
import com.sliit.vehiclebiddingsystem.repository.BidRepository;
import com.sliit.vehiclebiddingsystem.repository.NotificationRepository;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;
import com.sliit.vehiclebiddingsystem.repository.VehicleListingRepository;
import com.sliit.vehiclebiddingsystem.service.AuctionExtensionService;
import com.sliit.vehiclebiddingsystem.service.AuditLogService;

@RestController
@RequestMapping("/api/sales-manager")
public class SalesManagerController {

    @Autowired
    private AuctionRepository auctionRepository;
    
    @Autowired
    private BidRepository bidRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VehicleListingRepository vehicleListingRepository;
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Autowired
    private AuctionExtensionService auctionExtensionService;

    /**
     * Get sales manager dashboard metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            // Active auctions count
            long activeAuctions = auctionRepository.countByStatus(Status.ACTIVE);
            metrics.put("activeAuctions", activeAuctions);
            
            // Pending approvals count (approved vehicle listings ready to be scheduled for auctions)
            long pendingApprovals = vehicleListingRepository.findByStatus(VehicleListing.Status.APPROVED).stream()
                .filter(listing -> listing.getAuction() == null) // Only listings without existing auctions
                .count();
            metrics.put("pendingApprovals", pendingApprovals);
            
            // Auction reports count (closed auctions with reports)
            long auctionReports = auctionRepository.findByStatus(Status.CLOSED).stream()
                .filter(auction -> auction.getReport() != null)
                .count();
            metrics.put("auctionReports", auctionReports);
            
            // Scheduled auctions count (pending auctions)
            long scheduledAuctions = auctionRepository.countByStatus(Status.PENDING);
            metrics.put("scheduledAuctions", scheduledAuctions);
            
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to load metrics: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get paginated auctions with filtering
     */
    // @GetMapping("/auctions")
    // public ResponseEntity<Map<String, Object>> getAuctions(
    //         @RequestParam(defaultValue = "0") int page,
    //         @RequestParam(defaultValue = "10") int size,
    //         @RequestParam(required = false) String status,
    //         @RequestParam(required = false) String date,
    //         @RequestParam(required = false) String search) {
        
    //     try {
    // System.out.println("This is TRY block");
    //         Pageable pageable = PageRequest.of(page, size, Sort.by("endTime").descending());
    //         Page<Auction> auctionPage;
            
    //         // Apply filters
    //         if (status != null && !status.isEmpty()) {
    //             Status auctionStatus = Status.valueOf(status.toUpperCase());
    //             auctionPage = auctionRepository.findByStatus(auctionStatus, pageable);
    //         } else {
    //             auctionPage = auctionRepository.findAll(pageable);
    //         }
            
    //         // Apply search filter if provided
    //         if (search != null && !search.isEmpty()) {
    //             // This would need a custom repository method for search
    //             // For now, we'll filter in memory (not ideal for large datasets)
    //             List<Auction> filteredAuctions = auctionPage.getContent().stream()
    //                 .filter(auction -> 
    //                     auction.getListing().getMake().toLowerCase().contains(search.toLowerCase()) ||
    //                     auction.getListing().getModel().toLowerCase().contains(search.toLowerCase()))
    //                 .collect(Collectors.toList());
                
    //             // Enrich the filtered auctions with bid data
    //             List<Map<String, Object>> enrichedFilteredAuctions = filteredAuctions.stream()
    //                 .map(auction -> {
    //                     Map<String, Object> auctionData = new HashMap<>();
    //                     auctionData.put("auctionId", auction.getAuctionId());
    //                     auctionData.put("listing", auction.getListing());
    //                     auctionData.put("startTime", auction.getStartTime());
    //                     auctionData.put("endTime", auction.getEndTime());
    //                     auctionData.put("status", auction.getStatus().toString());
                        
    //                     // Get highest bid
    //                     Double highestBid = bidRepository.findHighestBidByAuctionId(auction.getAuctionId());
    //                     auctionData.put("highestBid", highestBid != null ? highestBid : 0.0);
                        
    //                     // Get bidder count
    //                     List<User> bidders = bidRepository.findDistinctBiddersByAuctionId(auction.getAuctionId());
    //                     auctionData.put("bidderCount", bidders.size());
                        
    //                     return auctionData;
    //                 })
    //                 .collect(Collectors.toList());
                
    //             // Create a custom page response
    //             Map<String, Object> response = new HashMap<>();
    //             response.put("content", enrichedFilteredAuctions);
    //             response.put("totalElements", enrichedFilteredAuctions.size());
    //             response.put("totalPages", 1);
    //             response.put("size", size);
    //             response.put("number", page);
    //             response.put("first", page == 0);
    //             response.put("last", true);
    //             response.put("numberOfElements", enrichedFilteredAuctions.size());
                
    //             return ResponseEntity.ok(response);
    //         }
            
    //         // Add bidder count and highest bid to each auction
    //         List<Map<String, Object>> enrichedAuctions = auctionPage.getContent().stream()
    //             .map(auction -> {
    //                 Map<String, Object> auctionData = new HashMap<>();
    //                 auctionData.put("auctionId", auction.getAuctionId());
    //                 auctionData.put("listing", auction.getListing());
    //                 auctionData.put("startTime", auction.getStartTime());
    //                 auctionData.put("endTime", auction.getEndTime());
    //                 auctionData.put("status", auction.getStatus().toString());
                    
    //                 // Get highest bid
    //                 Double highestBid = bidRepository.findHighestBidByAuctionId(auction.getAuctionId());
    //                 auctionData.put("highestBid", highestBid != null ? highestBid : 0.0);
                    
    //                 // Get bidder count
    //                 List<User> bidders = bidRepository.findDistinctBiddersByAuctionId(auction.getAuctionId());
    //                 auctionData.put("bidderCount", bidders.size());
                    
    //                 return auctionData;
    //             })
    //             .collect(Collectors.toList());
            
    //         Map<String, Object> response = new HashMap<>();
    //         response.put("content", enrichedAuctions);
    //         response.put("totalElements", auctionPage.getTotalElements());
    //         response.put("totalPages", auctionPage.getTotalPages());
    //         response.put("size", auctionPage.getSize());
    //         response.put("number", auctionPage.getNumber());
    //         response.put("first", auctionPage.isFirst());
    //         response.put("last", auctionPage.isLast());
    //         response.put("numberOfElements", auctionPage.getNumberOfElements());
            
    //         return ResponseEntity.ok(response);
    //     } catch (Exception e) {
    // System.out.println("This is CATCH block");
    //         Map<String, Object> error = new HashMap<>();
    //         error.put("error", "Failed to load auctions: " + e.getMessage());
    //         return ResponseEntity.internalServerError().body(error);
    //     }
    // }

    
    @GetMapping("/auctions")
public ResponseEntity<Map<String, Object>> getAuctions(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String date,
        @RequestParam(required = false) String search) {
    System.out.println("Error fetching *****");

    try {
        System.out.println("This is TRY block");
        Pageable pageable = PageRequest.of(page, size, Sort.by("endTime").descending());
        Page<Auction> auctionPage;

        // Filter by status
        if (status != null && !status.isEmpty()) {
            Status auctionStatus = Status.valueOf(status.toUpperCase());
            auctionPage = auctionRepository.findByStatus(auctionStatus, pageable);
        } else {
            auctionPage = auctionRepository.findAll(pageable);
        }

        // Apply in-memory filters (for small datasets only)
        List<Auction> filteredAuctions = auctionPage.getContent();
        
        // Apply date filter
        if (date != null && !date.isEmpty()) {
            LocalDateTime filterDate = LocalDate.parse(date).atStartOfDay();
            LocalDateTime nextDay = filterDate.plusDays(1);
            
            filteredAuctions = filteredAuctions.stream()
                .filter(auction -> 
                    auction.getStartTime() != null &&
                    auction.getStartTime().isAfter(filterDate) &&
                    auction.getStartTime().isBefore(nextDay)
                )
                .collect(Collectors.toList());
        }
        
        // Apply search filter
        if (search != null && !search.isEmpty()) {
            filteredAuctions = filteredAuctions.stream()
                .filter(auction ->
                    auction.getListing() != null &&
                    (
                        (auction.getListing().getMake() != null &&
                         auction.getListing().getMake().toLowerCase().contains(search.toLowerCase())) ||
                        (auction.getListing().getModel() != null &&
                         auction.getListing().getModel().toLowerCase().contains(search.toLowerCase()))
                    )
                )
                .collect(Collectors.toList());
        }

         // Convert to DTOs to avoid circular references
         List<AuctionDTO> enrichedAuctions = filteredAuctions.stream()
             .map(auction -> {
                 AuctionDTO dto = new AuctionDTO();
                 dto.setAuctionId(auction.getAuctionId());
                 dto.setStatus(auction.getStatus().toString());
                 dto.setStartTime(auction.getStartTime());
                 dto.setEndTime(auction.getEndTime());
                 dto.setCurrentEndTime(auction.getCurrentEndTime());
                 dto.setExtensionDuration(auction.getExtensionDuration());
                 dto.setWinnerId(auction.getWinner() != null ? auction.getWinner().getUserId() : null);
                 dto.setWinnerUsername(auction.getWinner() != null ? auction.getWinner().getUsername() : null);

                 // Convert listing to DTO
                 if (auction.getListing() != null) {
                     AuctionDTO.VehicleListingDTO listingDTO = new AuctionDTO.VehicleListingDTO();
                     listingDTO.setListingId(auction.getListing().getListingId());
                     listingDTO.setMake(auction.getListing().getMake());
                     listingDTO.setModel(auction.getListing().getModel());
                     listingDTO.setYear(auction.getListing().getYear());
                     listingDTO.setMileage(auction.getListing().getMileage());
                     listingDTO.setCondition(auction.getListing().getCondition().toString());
                     listingDTO.setStatus(auction.getListing().getStatus().toString());
                     listingDTO.setTransmission(auction.getListing().getTransmission());
                     listingDTO.setFuelType(auction.getListing().getFuelType());
                     listingDTO.setDescription(auction.getListing().getDescription());
                     listingDTO.setInspectorNotes(auction.getListing().getInspectorNotes());
                     listingDTO.setRejectionReason(auction.getListing().getRejectionReason());
                     listingDTO.setSellerId(auction.getListing().getSeller().getUserId());
                     listingDTO.setSellerUsername(auction.getListing().getSeller().getUsername());
                     dto.setListing(listingDTO);
                 }

                 // Get highest bid
                 Double highestBid = bidRepository.findHighestBidByAuctionId(auction.getAuctionId());
                 dto.setHighestBid(highestBid != null ? highestBid : 0.0);

                 // Get bidder count
                 List<User> bidders = bidRepository.findDistinctBiddersByAuctionId(auction.getAuctionId());
                 dto.setBidderCount(bidders != null ? bidders.size() : 0);

                 // Get total bids count
                 Long totalBids = bidRepository.countByAuctionAuctionId(auction.getAuctionId());
                 dto.setTotalBids(totalBids != null ? totalBids : 0L);

                 return dto;
             })
             .collect(Collectors.toList());

        // Build response map
        Map<String, Object> response = new HashMap<>();
        response.put("content", enrichedAuctions);
        response.put("totalElements", auctionPage.getTotalElements());
        response.put("totalPages", auctionPage.getTotalPages());
        response.put("size", auctionPage.getSize());
        response.put("number", auctionPage.getNumber());
        response.put("first", auctionPage.isFirst());
        response.put("last", auctionPage.isLast());
        response.put("numberOfElements", auctionPage.getNumberOfElements());

        System.out.println("Returning " + enrichedAuctions.size() + " auctions");
        return ResponseEntity.ok(response);

    } catch (Exception e) {
        e.printStackTrace();
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Failed to load auctions: " + e.getMessage());
        return ResponseEntity.internalServerError().body(error);
    }
    }

    /**
     * Get bid history for an auction
     */
    @GetMapping("/auctions/{auctionId}/bids")
    public ResponseEntity<List<BidDTO>> getBidHistory(@PathVariable Long auctionId) {
        try {
            List<Bid> bids = bidRepository.findByAuctionAuctionIdOrderByAmountDesc(auctionId);
            
            List<BidDTO> bidDTOs = bids.stream()
                .map(bid -> {
                    BidDTO dto = new BidDTO();
                    dto.setBidId(bid.getBidId());
                    dto.setAuctionId(bid.getAuction().getAuctionId());
                    dto.setAmount(bid.getAmount());
                    dto.setTimestamp(bid.getTimestamp());
                    
                    // Convert bidder to DTO
                    BidDTO.BidderDTO bidderDTO = new BidDTO.BidderDTO();
                    bidderDTO.setUserId(bid.getBidder().getUserId());
                    bidderDTO.setUsername(bid.getBidder().getUsername());
                    bidderDTO.setEmail(bid.getBidder().getEmail());
                    bidderDTO.setPhone(bid.getBidder().getPhone());
                    dto.setBidder(bidderDTO);
                    
                    return dto;
                })
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(bidDTOs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Start an auction
     */
    @PostMapping("/auctions/{auctionId}/start")
    public ResponseEntity<Map<String, Object>> startAuction(
            @PathVariable Long auctionId,
            Authentication authentication) {
        try {
            Auction auction = auctionRepository.findById(auctionId).orElse(null);
            if (auction == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Auction not found");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (auction.getStatus() != Status.PENDING) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Auction is not in pending status");
                return ResponseEntity.badRequest().body(error);
            }
            
            auction.setStatus(Status.ACTIVE);
            auctionRepository.save(auction);
            
            // Log the action
            User currentUser = userRepository.findByUsername(authentication.getName());
            if (currentUser == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "User not found");
                return ResponseEntity.badRequest().body(error);
            }
            auditLogService.logAction(currentUser, "START_AUCTION", auctionId, "AUCTION", "Auction started by sales manager");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Auction started successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to start auction: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Extend an auction
     */
    @PostMapping("/auctions/{auctionId}/extend")
    public ResponseEntity<Map<String, Object>> extendAuction(
            @PathVariable Long auctionId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            Auction auction = auctionRepository.findById(auctionId).orElse(null);
            if (auction == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Auction not found");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (auction.getStatus() != Status.ACTIVE) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Auction is not active");
                return ResponseEntity.badRequest().body(error);
            }
            
            Integer duration = (Integer) request.get("duration");
            String reason = (String) request.get("reason");
            
            if (duration == null || duration <= 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Invalid extension duration");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Use the extension service for manual extension
            auctionExtensionService.manuallyExtendAuction(auctionId, duration, reason);
            
            // Log the action
            User currentUser = userRepository.findByUsername(authentication.getName());
            if (currentUser == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "User not found");
                return ResponseEntity.badRequest().body(error);
            }
            auditLogService.logAction(currentUser, "EXTEND_AUCTION", auctionId, "AUCTION", 
                "Auction extended by " + duration + " minutes. Reason: " + reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Auction extended successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to extend auction: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * End an auction
     */
    @PostMapping("/auctions/{auctionId}/end")
    public ResponseEntity<Map<String, Object>> endAuction(
            @PathVariable Long auctionId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            Auction auction = auctionRepository.findById(auctionId).orElse(null);
            if (auction == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Auction not found");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (auction.getStatus() != Status.ACTIVE) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Auction is not active");
                return ResponseEntity.badRequest().body(error);
            }
            
            String reason = (String) request.get("reason");
            
            // End the auction
            auction.setStatus(Status.CLOSED);
            
            // Ensure end time is after start time to satisfy database constraint
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = auction.getStartTime();
            LocalDateTime endTime = now.isAfter(startTime) ? now : startTime.plusMinutes(1);
            
            auction.setEndTime(endTime);
            auction.setCurrentEndTime(endTime);
            auctionRepository.save(auction);
            
            // Determine winner (highest bidder) for manually closed auction
            List<Bid> bids = bidRepository.findByAuctionAuctionIdOrderByAmountDesc(auctionId);
            if (!bids.isEmpty()) {
                Bid winningBid = bids.get(0); // Highest bid is first due to ORDER BY amount DESC
                auction.setWinner(winningBid.getBidder());
                auctionRepository.save(auction);
            }
            
            // Log the action
            User currentUser = userRepository.findByUsername(authentication.getName());
            if (currentUser == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "User not found");
                return ResponseEntity.badRequest().body(error);
            }
            // Create audit log message with winner information
            String auditMessage = "Auction ended by sales manager. Reason: " + reason;
            if (!bids.isEmpty()) {
                auditMessage += ". Winner: " + bids.get(0).getBidder().getUsername() + " (Highest bid: $" + bids.get(0).getAmount() + ")";
            } else {
                auditMessage += ". No bids placed - no winner determined.";
            }
            
            auditLogService.logAction(currentUser, "END_AUCTION", auctionId, "AUCTION", auditMessage);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            
            // Create success message with winner information
            String successMessage = "Auction ended successfully";
            if (!bids.isEmpty()) {
                successMessage += ". Winner: " + bids.get(0).getBidder().getUsername() + " (Highest bid: $" + bids.get(0).getAmount() + ")";
            } else {
                successMessage += ". No bids were placed.";
            }
            
            response.put("message", successMessage);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to end auction: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get notifications for sales manager
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<Map<String, Object>>> getNotifications() {
        try {
            // Get recent auction-related notifications
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            List<Notification> notifications = notificationRepository.findBySentAtAfterOrderBySentAtDesc(since);
            
            List<Map<String, Object>> notificationData = notifications.stream()
                .map(notification -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", notification.getNotificationId());
                    data.put("type", notification.getType().toString());
                    data.put("content", notification.getContent());
                    data.put("sentAt", notification.getSentAt());
                    data.put("isRead", notification.isRead());
                    
                    // Generate title based on type
                    String title = switch (notification.getType()) {
                        case AUCTION_ENDING -> "Auction Ending Soon";
                        case AUCTION_STARTED -> "Auction Started";
                        case BID_PLACED -> "New Bid Placed";
                        case OUTBID -> "Bid Outbid";
                        case CLOSURE -> "Auction Closed";
                        case WINNER -> "Auction Winner";
                        default -> "Notification";
                    };
                    data.put("title", title);
                    
                    return data;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(notificationData);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export auctions to CSV
     */
    @GetMapping("/auctions/export")
    public ResponseEntity<String> exportAuctions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String search) {
        try {
            List<Auction> auctions;
            
            if (status != null && !status.isEmpty()) {
                Status auctionStatus = Status.valueOf(status.toUpperCase());
                auctions = auctionRepository.findByStatus(auctionStatus);
            } else {
                auctions = auctionRepository.findAll();
            }
            
            // Generate CSV content
            StringBuilder csv = new StringBuilder();
            csv.append("Auction ID,Vehicle Make,Vehicle Model,Year,Start Time,End Time,Status,Current Bid,Bidder Count\n");
            
            for (Auction auction : auctions) {
                Double highestBid = bidRepository.findHighestBidByAuctionId(auction.getAuctionId());
                List<User> bidders = bidRepository.findDistinctBiddersByAuctionId(auction.getAuctionId());
                
                csv.append(auction.getAuctionId()).append(",");
                csv.append(auction.getListing().getMake()).append(",");
                csv.append(auction.getListing().getModel()).append(",");
                csv.append(auction.getListing().getYear()).append(",");
                csv.append(auction.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append(",");
                csv.append(auction.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append(",");
                csv.append(auction.getStatus().toString()).append(",");
                csv.append(highestBid != null ? highestBid : 0).append(",");
                csv.append(bidders.size()).append("\n");
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "auctions_export.csv");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(csv.toString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get approved vehicles for auction creation
     */
    @GetMapping("/approved-vehicles/{listingId}")
    public ResponseEntity<VehicleListingDTO> getApprovedVehicleDetails(@PathVariable Long listingId) {
        try {
            VehicleListing listing = vehicleListingRepository.findByIdWithImages(listingId).orElse(null);
            if (listing == null) {
                return ResponseEntity.notFound().build();
            }

            // Check if listing is approved
            if (listing.getStatus() != VehicleListing.Status.APPROVED) {
                return ResponseEntity.badRequest().build();
            }

            VehicleListingDTO dto = new VehicleListingDTO(listing);
            
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/approved-vehicles")
    public ResponseEntity<List<VehicleListingDTO>> getApprovedVehicles() {
        try {
            List<VehicleListing> approvedListings = vehicleListingRepository.findByStatus(VehicleListing.Status.APPROVED);
            
            List<VehicleListingDTO> vehicles = approvedListings.stream()
                .filter(listing -> listing.getAuction() == null) // Only vehicles without existing auctions
                .map(listing -> {
                    VehicleListingDTO dto = new VehicleListingDTO();
                    dto.setListingId(listing.getListingId());
                    dto.setMake(listing.getMake());
                    dto.setModel(listing.getModel());
                    dto.setYear(listing.getYear());
                    dto.setMileage(listing.getMileage());
                    dto.setCondition(listing.getCondition());
                    dto.setStatus(listing.getStatus());
                    dto.setTransmission(listing.getTransmission());
                    dto.setFuelType(listing.getFuelType());
                    dto.setDescription(listing.getDescription());
                    dto.setInspectorNotes(listing.getInspectorNotes());
                    dto.setRejectionReason(listing.getRejectionReason());
                    dto.setSellerId(listing.getSeller().getUserId());
                    dto.setSellerUsername(listing.getSeller().getUsername());
                    return dto;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create new auction
     */
    @PostMapping("/auctions")
    public ResponseEntity<Map<String, Object>> createAuction(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            Long listingId = Long.valueOf(request.get("listingId").toString());
            String startTimeStr = request.get("startTime").toString();
            String endTimeStr = request.get("endTime").toString();
            Integer extensionDuration = Integer.valueOf(request.get("extensionDuration").toString());
            String notes = request.get("notes") != null ? request.get("notes").toString() : "";
            
            // Validate listing exists and is approved
            VehicleListing listing = vehicleListingRepository.findById(listingId).orElse(null);
            if (listing == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Vehicle listing not found");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (listing.getStatus() != VehicleListing.Status.APPROVED) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Vehicle listing is not approved");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (listing.getAuction() != null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Vehicle already has an auction");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Validate times - handle ISO format with timezone
            LocalDateTime startTime;
            LocalDateTime endTime;
            
            try {
                // Try parsing as ISO format first (with timezone)
                if (startTimeStr.contains("T") && (startTimeStr.endsWith("Z") || startTimeStr.contains("+"))) {
                    startTime = LocalDateTime.parse(startTimeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        .atZone(ZoneId.systemDefault()).toLocalDateTime();
                } else {
                    startTime = LocalDateTime.parse(startTimeStr);
                }
                
                if (endTimeStr.contains("T") && (endTimeStr.endsWith("Z") || endTimeStr.contains("+"))) {
                    endTime = LocalDateTime.parse(endTimeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        .atZone(ZoneId.systemDefault()).toLocalDateTime();
                } else {
                    endTime = LocalDateTime.parse(endTimeStr);
                }
            } catch (DateTimeParseException e) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Invalid date format: " + e.getMessage());
                return ResponseEntity.badRequest().body(error);
            }
            
            if (endTime.isBefore(startTime)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "End time must be after start time");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Debug logging for timezone issues
            LocalDateTime now = LocalDateTime.now();
            System.out.println("DEBUG: Current time: " + now);
            System.out.println("DEBUG: Parsed start time: " + startTime);
            System.out.println("DEBUG: Start time is before now: " + startTime.isBefore(now));
            System.out.println("DEBUG: Time difference (minutes): " + java.time.Duration.between(startTime, now).toMinutes());
            
            // Allow scheduling up to 2 minutes in the past to account for timezone/clock differences
            if (startTime.isBefore(now.minusMinutes(2))) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Start time must be in the future");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Create auction
            Auction auction = new Auction();
            auction.setListing(listing);
            auction.setStartTime(startTime);
            auction.setEndTime(endTime);
            auction.setCurrentEndTime(endTime);
            auction.setExtensionDuration(extensionDuration);
            auction.setStatus(Status.PENDING);
            
            auctionRepository.save(auction);
            
            // Log the action
            User currentUser = userRepository.findByUsername(authentication.getName());
            if (currentUser == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "User not found");
                return ResponseEntity.badRequest().body(error);
            }
            auditLogService.logAction(currentUser, AuditLog.ActionType.CREATE_AUCTION.toString(), auction.getAuctionId(), "AUCTION", 
                "Auction created for " + listing.getMake() + " " + listing.getModel() + ". Notes: " + notes);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Auction created successfully");
            response.put("auctionId", auction.getAuctionId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to create auction: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get live auctions for monitoring
     */
    @GetMapping("/upcoming-starts")
    public ResponseEntity<List<AuctionDTO>> getUpcomingStarts() {
        try {
            // Get up to 3 pending auctions scheduled to start soon
            List<Auction> upcomingAuctions = auctionRepository.findTop3ByStatusOrderByStartTimeAsc(
                Status.PENDING, PageRequest.of(0, 3));
            
            List<AuctionDTO> upcomingDTOs = upcomingAuctions.stream()
                .map(auction -> {
                    AuctionDTO dto = new AuctionDTO();
                    dto.setAuctionId(auction.getAuctionId());
                    dto.setStatus(auction.getStatus().toString());
                    dto.setStartTime(auction.getStartTime());
                    dto.setEndTime(auction.getEndTime());
                    dto.setCurrentEndTime(auction.getCurrentEndTime());
                    dto.setExtensionDuration(auction.getExtensionDuration());
                    dto.setWinnerId(auction.getWinner() != null ? auction.getWinner().getUserId() : null);
                    dto.setWinnerUsername(auction.getWinner() != null ? auction.getWinner().getUsername() : null);

                    // Convert listing to DTO
                    if (auction.getListing() != null) {
                        AuctionDTO.VehicleListingDTO listingDTO = new AuctionDTO.VehicleListingDTO();
                        listingDTO.setListingId(auction.getListing().getListingId());
                        listingDTO.setMake(auction.getListing().getMake());
                        listingDTO.setModel(auction.getListing().getModel());
                        listingDTO.setYear(auction.getListing().getYear());
                        listingDTO.setMileage(auction.getListing().getMileage());
                        listingDTO.setCondition(auction.getListing().getCondition().toString());
                        listingDTO.setStatus(auction.getListing().getStatus().toString());
                        listingDTO.setTransmission(auction.getListing().getTransmission());
                        listingDTO.setFuelType(auction.getListing().getFuelType());
                        listingDTO.setDescription(auction.getListing().getDescription());
                        listingDTO.setInspectorNotes(auction.getListing().getInspectorNotes());
                        listingDTO.setRejectionReason(auction.getListing().getRejectionReason());
                        listingDTO.setSellerId(auction.getListing().getSeller().getUserId());
                        listingDTO.setSellerUsername(auction.getListing().getSeller().getUsername());
                        dto.setListing(listingDTO);
                    }

                    // Get highest bid
                    Double highestBid = bidRepository.findHighestBidByAuctionId(auction.getAuctionId());
                    dto.setHighestBid(highestBid != null ? highestBid : 0.0);

                    // Get bidder count
                    List<User> bidders = bidRepository.findDistinctBiddersByAuctionId(auction.getAuctionId());
                    dto.setBidderCount(bidders != null ? bidders.size() : 0);

                    // Get total bids count
                    Long totalBids = bidRepository.countByAuctionAuctionId(auction.getAuctionId());
                    dto.setTotalBids(totalBids != null ? totalBids : 0L);

                    return dto;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(upcomingDTOs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/recently-closed")
    public ResponseEntity<List<AuctionDTO>> getRecentlyClosed() {
        try {
            // Get the 3 most recently closed auctions
            List<Auction> recentAuctions = auctionRepository.findTop3ByStatusOrderByEndTimeDesc(
                Status.CLOSED, PageRequest.of(0, 3));
            
            List<AuctionDTO> recentDTOs = recentAuctions.stream()
                .map(auction -> {
                    AuctionDTO dto = new AuctionDTO();
                    dto.setAuctionId(auction.getAuctionId());
                    dto.setStatus(auction.getStatus().toString());
                    dto.setStartTime(auction.getStartTime());
                    dto.setEndTime(auction.getEndTime());
                    dto.setCurrentEndTime(auction.getCurrentEndTime());
                    dto.setExtensionDuration(auction.getExtensionDuration());
                    dto.setWinnerId(auction.getWinner() != null ? auction.getWinner().getUserId() : null);
                    dto.setWinnerUsername(auction.getWinner() != null ? auction.getWinner().getUsername() : null);

                    // Convert listing to DTO
                    if (auction.getListing() != null) {
                        AuctionDTO.VehicleListingDTO listingDTO = new AuctionDTO.VehicleListingDTO();
                        listingDTO.setListingId(auction.getListing().getListingId());
                        listingDTO.setMake(auction.getListing().getMake());
                        listingDTO.setModel(auction.getListing().getModel());
                        listingDTO.setYear(auction.getListing().getYear());
                        listingDTO.setMileage(auction.getListing().getMileage());
                        listingDTO.setCondition(auction.getListing().getCondition().toString());
                        listingDTO.setStatus(auction.getListing().getStatus().toString());
                        listingDTO.setTransmission(auction.getListing().getTransmission());
                        listingDTO.setFuelType(auction.getListing().getFuelType());
                        listingDTO.setDescription(auction.getListing().getDescription());
                        listingDTO.setInspectorNotes(auction.getListing().getInspectorNotes());
                        listingDTO.setRejectionReason(auction.getListing().getRejectionReason());
                        listingDTO.setSellerId(auction.getListing().getSeller().getUserId());
                        listingDTO.setSellerUsername(auction.getListing().getSeller().getUsername());
                        dto.setListing(listingDTO);
                    }

                    // Get highest bid
                    Double highestBid = bidRepository.findHighestBidByAuctionId(auction.getAuctionId());
                    dto.setHighestBid(highestBid != null ? highestBid : 0.0);

                    // Get bidder count
                    List<User> bidders = bidRepository.findDistinctBiddersByAuctionId(auction.getAuctionId());
                    dto.setBidderCount(bidders != null ? bidders.size() : 0);

                    // Get total bids count
                    Long totalBids = bidRepository.countByAuctionAuctionId(auction.getAuctionId());
                    dto.setTotalBids(totalBids != null ? totalBids : 0L);

                    return dto;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(recentDTOs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/live-auctions")
    public ResponseEntity<List<LiveAuctionDTO>> getLiveAuctions() {
        try {
            // Get up to 5 active auctions
            List<Auction> activeAuctions = auctionRepository.findTop5ByStatusOrderByEndTimeAsc(
                Status.ACTIVE, PageRequest.of(0, 5));
            
            List<LiveAuctionDTO> liveAuctions = activeAuctions.stream()
                .map(auction -> {
                    LiveAuctionDTO dto = new LiveAuctionDTO();
                    dto.setAuctionId(auction.getAuctionId());
                    dto.setEndTime(auction.getEndTime());
                    
                    // Get highest bid
                    Double highestBid = bidRepository.findHighestBidByAuctionId(auction.getAuctionId());
                    dto.setHighestBid(highestBid != null ? highestBid : 0.0);
                    
                    // Convert listing to DTO
                    if (auction.getListing() != null) {
                        LiveAuctionDTO.VehicleListingDTO listingDTO = new LiveAuctionDTO.VehicleListingDTO();
                        listingDTO.setListingId(auction.getListing().getListingId());
                        listingDTO.setMake(auction.getListing().getMake());
                        listingDTO.setModel(auction.getListing().getModel());
                        listingDTO.setYear(auction.getListing().getYear());
                        listingDTO.setMileage(auction.getListing().getMileage());
                        listingDTO.setCondition(auction.getListing().getCondition().toString());
                        listingDTO.setStatus(auction.getListing().getStatus().toString());
                        dto.setListing(listingDTO);
                    }
                    
                    return dto;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(liveAuctions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get performance analytics
     */
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        try {
            Map<String, Object> analytics = new HashMap<>();
            
            // Total revenue from closed auctions
            List<Auction> closedAuctions = auctionRepository.findByStatus(Status.CLOSED);
            double totalRevenue = closedAuctions.stream()
                .mapToDouble(auction -> {
                    Double highestBid = bidRepository.findHighestBidByAuctionId(auction.getAuctionId());
                    return highestBid != null ? highestBid : 0.0;
                })
                .sum();
            analytics.put("totalRevenue", totalRevenue);
            
            // Win rate (auctions with winners)
            long auctionsWithWinners = closedAuctions.stream()
                .filter(auction -> auction.getWinner() != null)
                .count();
            double winRate = closedAuctions.isEmpty() ? 0.0 : (double) auctionsWithWinners / closedAuctions.size() * 100;
            analytics.put("winRate", winRate);
            
            // Chart data for bids over the last 7 days
            List<Integer> chartData = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (int i = 6; i >= 0; i--) {
                LocalDateTime startOfDay = now.minusDays(i).withHour(0).withMinute(0).withSecond(0);
                LocalDateTime endOfDay = now.minusDays(i).withHour(23).withMinute(59).withSecond(59);
                long bidsCount = bidRepository.countByTimestampBetween(startOfDay, endOfDay);
                chartData.add((int) bidsCount);
            }
            analytics.put("chartData", chartData);
            
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get post-auction details
     */
    @GetMapping("/auctions/{auctionId}/post-auction")
    public ResponseEntity<PostAuctionDTO> getPostAuctionDetails(@PathVariable Long auctionId) {
        try {
            Auction auction = auctionRepository.findById(auctionId).orElse(null);
            if (auction == null) {
                return ResponseEntity.notFound().build();
            }
            
            PostAuctionDTO dto = new PostAuctionDTO();
            dto.setAuctionId(auction.getAuctionId());
            
            // Convert listing to DTO
            if (auction.getListing() != null) {
                PostAuctionDTO.VehicleListingDTO listingDTO = new PostAuctionDTO.VehicleListingDTO();
                listingDTO.setListingId(auction.getListing().getListingId());
                listingDTO.setMake(auction.getListing().getMake());
                listingDTO.setModel(auction.getListing().getModel());
                listingDTO.setYear(auction.getListing().getYear());
                listingDTO.setMileage(auction.getListing().getMileage());
                listingDTO.setCondition(auction.getListing().getCondition().toString());
                listingDTO.setStatus(auction.getListing().getStatus().toString());
                dto.setListing(listingDTO);
            }
            
            // Convert winner to DTO
            if (auction.getWinner() != null) {
                PostAuctionDTO.WinnerDTO winnerDTO = new PostAuctionDTO.WinnerDTO();
                winnerDTO.setUserId(auction.getWinner().getUserId());
                winnerDTO.setUsername(auction.getWinner().getUsername());
                winnerDTO.setEmail(auction.getWinner().getEmail());
                dto.setWinner(winnerDTO);
            }
            
            // Get bid statistics
            List<Bid> bids = bidRepository.findByAuctionAuctionIdOrderByAmountDesc(auctionId);
            Double highestBid = bidRepository.findHighestBidByAuctionId(auctionId);
            List<User> bidders = bidRepository.findDistinctBiddersByAuctionId(auctionId);
            
            dto.setHighestBid(highestBid != null ? highestBid : 0.0);
            dto.setTotalBids(bids.size());
            dto.setParticipantCount(bidders.size());
            
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Flag auction for reporting
     */
    @PostMapping("/auctions/{auctionId}/flag-reporting")
    public ResponseEntity<Map<String, Object>> flagForReporting(
            @PathVariable Long auctionId,
            Authentication authentication) {
        try {
            Auction auction = auctionRepository.findById(auctionId).orElse(null);
            if (auction == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Auction not found");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Log the action
            User currentUser = userRepository.findByUsername(authentication.getName());
            if (currentUser == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "User not found");
                return ResponseEntity.badRequest().body(error);
            }
            auditLogService.logAction(currentUser, "FLAG_REPORTING", auctionId, "AUCTION", 
                "Auction flagged for reporting by sales manager");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Auction flagged for reporting successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to flag auction: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
