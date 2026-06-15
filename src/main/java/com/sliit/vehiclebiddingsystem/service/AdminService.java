package com.sliit.vehiclebiddingsystem.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.entity.AuditLog;
import com.sliit.vehiclebiddingsystem.entity.AuditLog.ActionType;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.entity.VehicleListing;
import com.sliit.vehiclebiddingsystem.entity.VehicleListing.Status;
import com.sliit.vehiclebiddingsystem.repository.AuctionRepository;
import com.sliit.vehiclebiddingsystem.repository.AuditLogRepository;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;
import com.sliit.vehiclebiddingsystem.repository.VehicleListingRepository;

@Service
@Transactional
public class AdminService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VehicleListingRepository vehicleListingRepository;
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private AuctionRepository auctionRepository;

    // Dashboard Statistics
    public AdminDashboardStats getDashboardStats() {
        try {
            long totalUsers = userRepository.count();
            long bannedUsers = userRepository.countByIsBannedTrue();
            long pendingListings = vehicleListingRepository.countByStatus(Status.PENDING);
            long activeAuctions = auctionRepository.countByStatus(com.sliit.vehiclebiddingsystem.entity.Auction.Status.ACTIVE);
            
            return new AdminDashboardStats(totalUsers, bannedUsers, pendingListings, activeAuctions);
        } catch (Exception e) {
            System.err.println("Error getting dashboard stats: " + e.getMessage());
            e.printStackTrace();
            // Return default stats if there's an error
            return new AdminDashboardStats(0, 0, 0, 0);
        }
    }

    // Listing Moderation
    public List<VehicleListing> getPendingListings() {
        Pageable pageable = PageRequest.of(0, 10);
        return vehicleListingRepository.findByStatus(Status.PENDING, pageable).getContent();
    }
    
    public List<VehicleListing> getRejectedListings() {
        Pageable pageable = PageRequest.of(0, 10);
        return vehicleListingRepository.findByStatus(Status.REJECTED, pageable).getContent();
    }
    
    public List<VehicleListing> getApprovedListings() {
        Pageable pageable = PageRequest.of(0, 10);
        return vehicleListingRepository.findByStatus(Status.APPROVED, pageable).getContent();
    }
    
    public List<Auction> getPastAuctions() {
        Pageable pageable = PageRequest.of(0, 10);
        return auctionRepository.findByStatus(com.sliit.vehiclebiddingsystem.entity.Auction.Status.CLOSED, pageable).getContent();
    }

    // Full page methods
    public Page<VehicleListing> getPendingListingsPage(Pageable pageable) {
        return vehicleListingRepository.findByStatus(Status.PENDING, pageable);
    }
    
    public Page<VehicleListing> getRejectedListingsPage(Pageable pageable) {
        return vehicleListingRepository.findByStatus(Status.REJECTED, pageable);
    }
    
    public Page<VehicleListing> getApprovedListingsPage(Pageable pageable) {
        return vehicleListingRepository.findByStatus(Status.APPROVED, pageable);
    }
    
    public Page<Auction> getPastAuctionsPage(Pageable pageable) {
        return auctionRepository.findByStatus(com.sliit.vehiclebiddingsystem.entity.Auction.Status.CLOSED, pageable);
    }

    // Auto-unban expired temporary bans
    @Scheduled(fixedRate = 60000) // Run every minute
    public void autoUnbanExpiredUsers() {
        List<User> bannedUsers = userRepository.findAll().stream()
            .filter(User::isBanned)
            .filter(user -> "temp".equals(user.getBanType()))
            .filter(user -> user.getLockoutUntil() != null && user.getLockoutUntil().isBefore(LocalDateTime.now()))
            .toList();
        
        for (User user : bannedUsers) {
            user.setBanned(false);
            user.setBanType(null);
            user.setBanDuration(null);
            user.setLockoutUntil(null);
            userRepository.save(user);
            
            // Log the auto-unban action
            User systemAdmin = getSystemAdmin();
            logAdminAction(systemAdmin, ActionType.APPROVE, user.getUserId(), "USER", "Auto-unbanned: Temporary ban expired");
        }
    }

    // Auto-reject expired listings
    @Scheduled(fixedRate = 60000) // Run every minute
    public void autoRejectExpiredListings() {
        List<VehicleListing> pendingListings = vehicleListingRepository.findByStatus(Status.PENDING);
        LocalDateTime now = LocalDateTime.now();
        
        for (VehicleListing listing : pendingListings) {
            // Check if this listing has an associated auction with a start time that has passed
            if (listing.getAuction() != null && 
                listing.getAuction().getStartTime() != null && 
                listing.getAuction().getStartTime().isBefore(now)) {
                
                // Auto-reject the listing
                listing.setStatus(Status.REJECTED);
                listing.setRejectionReason("Auto-rejected: Listing expired before admin review");
                listing.setInspectorNotes("System automatically rejected due to scheduled auction start time passing without admin approval");
                vehicleListingRepository.save(listing);
                
                // Log the action (using system admin user)
                User systemAdmin = getSystemAdmin();
                logAdminAction(systemAdmin, ActionType.REJECT, listing.getListingId(), "VEHICLE_LISTING", "Auto-rejected: Listing expired before admin review");
            }
        }
    }
    
    // Helper method to get or create system admin user
    private User getSystemAdmin() {
        User systemAdmin = userRepository.findByUsername("system");
        if (systemAdmin == null) {
            systemAdmin = new User();
            systemAdmin.setUsername("system");
            systemAdmin.setEmail("system@vehiclebidding.com");
            systemAdmin.setRole(User.Role.ADMIN_OFFICER);
            systemAdmin.setBanned(false);
            systemAdmin = userRepository.save(systemAdmin);
        }
        return systemAdmin;
    }
    
    public void approveListing(Long listingId, User admin, String reason) {
        Optional<VehicleListing> listingOpt = vehicleListingRepository.findById(listingId);
        if (listingOpt.isPresent()) {
            VehicleListing listing = listingOpt.get();
            listing.setStatus(Status.APPROVED);
            listing.setInspectorNotes(reason);
            vehicleListingRepository.save(listing);
            
            // Log the action
            logAdminAction(admin, ActionType.APPROVE, listingId, "VEHICLE_LISTING", reason);
        }
    }
    
    public void rejectListing(Long listingId, User admin, String rejectionReason, String inspectorNotes) {
        Optional<VehicleListing> listingOpt = vehicleListingRepository.findById(listingId);
        if (listingOpt.isPresent()) {
            VehicleListing listing = listingOpt.get();
            listing.setStatus(Status.REJECTED);
            listing.setRejectionReason(rejectionReason);
            listing.setInspectorNotes(inspectorNotes != null ? inspectorNotes : "");
            vehicleListingRepository.save(listing);
            
            // Log the action
            logAdminAction(admin, ActionType.REJECT, listingId, "VEHICLE_LISTING", rejectionReason);
        }
    }

    // User Management
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public List<User> searchUsers(String searchTerm) {
        return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(searchTerm, searchTerm);
    }
    
    public void banUser(Long userId, User admin, String banType, Integer banDuration, String reason) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setBanned(true);
            user.setBanType(banType);
            user.setBanDuration(banDuration);
            
            // Calculate lockoutUntil based on ban type
            if ("temp".equals(banType) && banDuration != null) {
                user.setLockoutUntil(LocalDateTime.now().plusDays(banDuration));
            } else if ("perm".equals(banType)) {
                user.setLockoutUntil(LocalDateTime.now().plusYears(100)); // Effectively permanent
            }
            
            userRepository.save(user);
            
            // Log the action
            logAdminAction(admin, ActionType.BAN, userId, "USER", reason);
        }
    }
    
    public void unbanUser(Long userId, User admin, String reason) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setBanned(false);
            user.setBanType(null);
            user.setBanDuration(null);
            user.setLockoutUntil(null); // Clear lockout
            userRepository.save(user);
            
            // Log the action
            logAdminAction(admin, ActionType.APPROVE, userId, "USER", reason);
        }
    }
    
    public void changeUserRole(Long userId, User admin, User.Role newRole, String reason) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setRole(newRole);
            userRepository.save(user);
            
            // Log the action
            logAdminAction(admin, ActionType.APPROVE, userId, "USER", reason);
        }
    }

    // Activity Logs
    public Page<AuditLog> getActivityLogs(Pageable pageable) {
        return auditLogRepository.findAllOrderByTimestampDesc(pageable);
    }
    
    public List<AuditLog> getActivityLogsByActionType(ActionType actionType) {
        return auditLogRepository.findByActionType(actionType);
    }
    
    public List<AuditLog> getActivityLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByTimestampBetween(startDate, endDate);
    }
    
    public List<AuditLog> getActivityLogsByActionTypeAndDateRange(ActionType actionType, 
                                                                 LocalDateTime startDate, 
                                                                 LocalDateTime endDate) {
        return auditLogRepository.findByActionTypeAndTimestampBetween(actionType, startDate, endDate);
    }

    // Role-based access control methods
    public boolean canManageUsers(User admin) {
        return admin.getRole() == User.Role.ADMIN_OFFICER || admin.getRole() == User.Role.IT_CONSULTANT;
    }
    
    public boolean canModerateListings(User admin) {
        return admin.getRole() == User.Role.ADMIN_OFFICER || 
               admin.getRole() == User.Role.IT_CONSULTANT || 
               admin.getRole() == User.Role.VEHICLE_INSPECTOR;
    }
    
    public boolean canManageAuctions(User admin) {
        return admin.getRole() == User.Role.ADMIN_OFFICER || 
               admin.getRole() == User.Role.IT_CONSULTANT || 
               admin.getRole() == User.Role.SALES_MANAGER;
    }
    
    public boolean canAccessReports(User admin) {
        return admin.getRole() == User.Role.ADMIN_OFFICER || 
               admin.getRole() == User.Role.IT_CONSULTANT || 
               admin.getRole() == User.Role.CUSTOMER_SERVICE;
    }
    
    public boolean canViewSecurityLogs(User admin) {
        return admin.getRole() == User.Role.ADMIN_OFFICER || admin.getRole() == User.Role.IT_CONSULTANT;
    }

    // Vehicle Inspector Dashboard methods
    public long getPendingListingsCount() {
        return vehicleListingRepository.countByStatus(Status.PENDING);
    }
    
    public long getApprovedTodayCount() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        try {
            // Get approved listings first
            List<VehicleListing> approvedListings = vehicleListingRepository.findByStatus(Status.APPROVED);
            
            return approvedListings.stream()
                .filter(listing -> {
                    // Check if listing was approved today by looking at audit logs
                    return auditLogRepository.findByTargetTypeAndTargetId("VEHICLE_LISTING", listing.getListingId())
                        .stream()
                        .anyMatch(log -> log.getActionType() == ActionType.APPROVE && 
                                 log.getTimestamp().isAfter(startOfDay) && 
                                 log.getTimestamp().isBefore(endOfDay));
                })
                .count();
        } catch (Exception e) {
            System.err.println("Error getting approved today count: " + e.getMessage());
            return 0;
        }
    }
    
    public long getRejectedTodayCount() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        try {
            // Get rejected listings first
            List<VehicleListing> rejectedListings = vehicleListingRepository.findByStatus(Status.REJECTED);
            
            return rejectedListings.stream()
                .filter(listing -> {
                    // Check if listing was rejected today by looking at audit logs
                    return auditLogRepository.findByTargetTypeAndTargetId("VEHICLE_LISTING", listing.getListingId())
                        .stream()
                        .anyMatch(log -> log.getActionType() == ActionType.REJECT && 
                                 log.getTimestamp().isAfter(startOfDay) && 
                                 log.getTimestamp().isBefore(endOfDay));
                })
                .count();
        } catch (Exception e) {
            System.err.println("Error getting rejected today count: " + e.getMessage());
            return 0;
        }
    }
    
    public long getTotalInspectedCount() {
        try {
            long approvedCount = vehicleListingRepository.countByStatus(Status.APPROVED);
            long rejectedCount = vehicleListingRepository.countByStatus(Status.REJECTED);
            return approvedCount + rejectedCount;
        } catch (Exception e) {
            System.err.println("Error getting total inspected count: " + e.getMessage());
            return 0;
        }
    }
    
    public List<VehicleListing> getVehicleInspectorListings(String status, String search, Integer year) {
        System.out.println("AdminService: getVehicleInspectorListings called with status=" + status + ", search=" + search + ", year=" + year);
        try {
            Status listingStatus = null;
            if (status != null && !status.isEmpty()) {
                try {
                    listingStatus = Status.valueOf(status.toUpperCase());
                    System.out.println("AdminService: Filtering by status: " + listingStatus);
                } catch (IllegalArgumentException e) {
                    System.err.println("AdminService: Invalid status provided: " + status + ". Will fetch all listings. Error: " + e.getMessage());
                    // listingStatus remains null, will fetch all
                }
            } else {
                System.out.println("AdminService: No status filter, will fetch all listings");
            }
            
            // Use efficient database-level filtering instead of in-memory filtering
            List<VehicleListing> listings = vehicleListingRepository.findVehicleInspectorListings(listingStatus, search, year);
            System.out.println("AdminService: Found " + listings.size() + " listings using efficient query");
            return listings;
        } catch (Exception e) {
            System.err.println("Error getting vehicle inspector listings: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    public VehicleListing getVehicleInspectorListingDetails(Long id) {
        Optional<VehicleListing> listing = vehicleListingRepository.findByIdWithImages(id);
        return listing.orElse(null);
    }

    // Helper method to log admin actions
    private void logAdminAction(User admin, ActionType actionType, Long targetId, String targetType, String reason) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAdmin(admin); // Can be null for system actions
        auditLog.setActionType(actionType);
        auditLog.setTargetId(targetId);
        auditLog.setTargetType(targetType);
        auditLog.setReason(reason);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }

    // DTO for dashboard stats
    public static class AdminDashboardStats {
        private final long totalUsers;
        private final long bannedUsers;
        private final long pendingListings;
        private final long activeAuctions;

        public AdminDashboardStats(long totalUsers, long bannedUsers, long pendingListings, long activeAuctions) {
            this.totalUsers = totalUsers;
            this.bannedUsers = bannedUsers;
            this.pendingListings = pendingListings;
            this.activeAuctions = activeAuctions;
        }

        public long getTotalUsers() { return totalUsers; }
        public long getBannedUsers() { return bannedUsers; }
        public long getPendingListings() { return pendingListings; }
        public long getActiveAuctions() { return activeAuctions; }
    }
}
