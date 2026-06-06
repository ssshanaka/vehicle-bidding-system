package com.sliit.vehiclebiddingsystem.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sliit.vehiclebiddingsystem.dto.VehicleListingDTO;
import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.entity.AuditLog;
import com.sliit.vehiclebiddingsystem.entity.AuditLog.ActionType;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.entity.VehicleListing;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;
import com.sliit.vehiclebiddingsystem.service.AdminService;
import com.sliit.vehiclebiddingsystem.service.AdminService.AdminDashboardStats;
import com.sliit.vehiclebiddingsystem.service.CustomerServiceServiceSimple;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CustomerServiceServiceSimple customerServiceServiceSimple;
    

    // Dashboard
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            User currentUser = getCurrentUser();
            AdminDashboardStats stats = adminService.getDashboardStats();
            
            // Add role-specific permissions to model
            model.addAttribute("stats", stats);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("canManageUsers", adminService.canManageUsers(currentUser));
            model.addAttribute("canModerateListings", adminService.canModerateListings(currentUser));
            model.addAttribute("canManageAuctions", adminService.canManageAuctions(currentUser));
            model.addAttribute("canAccessReports", adminService.canAccessReports(currentUser));
            model.addAttribute("canViewSecurityLogs", adminService.canViewSecurityLogs(currentUser));
            
            // Redirect to role-specific dashboard template
            switch (currentUser.getRole()) {
                case SALES_MANAGER:
                    return "admin-sales-manager-dashboard";
                case CUSTOMER_SERVICE:
                    // Add CSE-specific attributes for the customer service dashboard
                    try {
                        Map<String, Object> dashboardStats = customerServiceServiceSimple.getDashboardStats();
                        List<Map<String, Object>> recentClosures = customerServiceServiceSimple.getRecentAuctionClosures(5);
                        List<Map<String, Object>> pendingNotifications = customerServiceServiceSimple.getPendingNotifications(10);
                        List<Map<String, Object>> recentActivity = customerServiceServiceSimple.getRecentUserActivity(5);
                        
                        model.addAttribute("dashboardStats", dashboardStats);
                        model.addAttribute("recentClosures", recentClosures);
                        model.addAttribute("pendingNotifications", pendingNotifications);
                        model.addAttribute("recentActivity", recentActivity);
                    } catch (Exception e) {
                        // If CSE service fails, provide default values
                        Map<String, Object> dashboardStats = new java.util.HashMap<>();
                        dashboardStats.put("recentAuctionClosures", 0);
                        dashboardStats.put("pendingNotifications", 0);
                        dashboardStats.put("winnersNotified", 0);
                        dashboardStats.put("openQueries", 0);
                        dashboardStats.put("inProgressQueries", 0);
                        dashboardStats.put("urgentQueries", 0);
                        dashboardStats.put("reportsGenerated", 0);
                        dashboardStats.put("recentActiveUsers", 0);
                        
                        model.addAttribute("dashboardStats", dashboardStats);
                        model.addAttribute("recentClosures", new java.util.ArrayList<>());
                        model.addAttribute("pendingNotifications", new java.util.ArrayList<>());
                        model.addAttribute("recentActivity", new java.util.ArrayList<>());
                    }
                    return "admin-customer-service-dashboard";
                case VEHICLE_INSPECTOR:
                    return "admin-vehicle-inspector-dashboard";
                case ADMIN_OFFICER:
                    return "admin-officer-dashboard";
                case IT_CONSULTANT:
                    return "admin-it-consultant-dashboard";
                default:
                    return "admin-dashboard";
            }
        } catch (Exception e) {
            // Log the error and return a generic error page
            System.err.println("Error in admin dashboard: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "An error occurred while loading the dashboard: " + e.getMessage());
            return "error";
        }
    }

    // Listing Moderation
    @GetMapping("/listings")
    public String listingModeration(Model model) {
        User currentUser = getCurrentUser();
        
        // Check if user can moderate listings
        if (!adminService.canModerateListings(currentUser)) {
            return "redirect:/admin/dashboard?error=access_denied";
        }
        
        List<VehicleListing> pendingListings = adminService.getPendingListings();
        List<VehicleListing> rejectedListings = adminService.getRejectedListings();
        List<VehicleListing> approvedListings = adminService.getApprovedListings();
        List<Auction> pastAuctions = adminService.getPastAuctions();
        
        model.addAttribute("pendingListings", pendingListings);
        model.addAttribute("rejectedListings", rejectedListings);
        model.addAttribute("approvedListings", approvedListings);
        model.addAttribute("pastAuctions", pastAuctions);
        model.addAttribute("currentUser", currentUser);
        return "admin-listing-moderation";
    }

    @PostMapping("/listings/{id}/approve")
    @ResponseBody
    public ResponseEntity<String> approveListing(@PathVariable Long id, @RequestParam String reason) {
        try {
            User admin = getCurrentUser();
            adminService.approveListing(id, admin, reason);
            return ResponseEntity.ok("Listing approved successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error approving listing: " + e.getMessage());
        }
    }

    @PostMapping("/listings/{id}/reject")
    @ResponseBody
    public ResponseEntity<String> rejectListing(@PathVariable Long id, 
                                              @RequestParam String rejectionReason,
                                              @RequestParam(required = false) String inspectorNotes) {
        try {
            User admin = getCurrentUser();
            adminService.rejectListing(id, admin, rejectionReason, inspectorNotes);
            return ResponseEntity.ok("Listing rejected successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error rejecting listing: " + e.getMessage());
        }
    }

    // Full listing pages
    @GetMapping("/listings/pending")
    public String pendingListings(Model model, @RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 20);
        Page<VehicleListing> pendingListings = adminService.getPendingListingsPage(pageable);
        model.addAttribute("listings", pendingListings);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pendingListings.getTotalPages());
        return "admin-pending-listings";
    }

    @GetMapping("/listings/rejected")
    public String rejectedListings(Model model, @RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 20);
        Page<VehicleListing> rejectedListings = adminService.getRejectedListingsPage(pageable);
        model.addAttribute("listings", rejectedListings);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", rejectedListings.getTotalPages());
        return "admin-rejected-listings";
    }

    @GetMapping("/listings/approved")
    public String approvedListings(Model model, @RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 20);
        Page<VehicleListing> approvedListings = adminService.getApprovedListingsPage(pageable);
        model.addAttribute("listings", approvedListings);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", approvedListings.getTotalPages());
        return "admin-approved-listings";
    }

    @GetMapping("/auctions/past")
    public String pastAuctions(Model model, @RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 20);
        Page<Auction> pastAuctions = adminService.getPastAuctionsPage(pageable);
        model.addAttribute("auctions", pastAuctions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pastAuctions.getTotalPages());
        return "admin-past-auctions";
    }

    // User Management
    @GetMapping("/users")
    public String userManagement(Model model, @RequestParam(required = false) String search) {
        User currentUser = getCurrentUser();
        
        // Check if user can manage users
        if (!adminService.canManageUsers(currentUser)) {
            return "redirect:/admin/dashboard?error=access_denied";
        }
        
        List<User> users;
        if (search != null && !search.trim().isEmpty()) {
            users = adminService.searchUsers(search);
        } else {
            users = adminService.getAllUsers();
        }
        
        model.addAttribute("users", users);
        model.addAttribute("search", search);
        model.addAttribute("currentUser", currentUser);
        return "admin-user-management";
    }

    @PostMapping("/users/{id}/ban")
    @ResponseBody
    public ResponseEntity<String> banUser(@PathVariable Long id,
                                        @RequestParam String banType,
                                        @RequestParam(required = false) Integer banDuration,
                                        @RequestParam String reason) {
        try {
            User admin = getCurrentUser();
            
            // Check if user can manage users
            if (!adminService.canManageUsers(admin)) {
                return ResponseEntity.status(403).body("Access denied: Insufficient permissions");
            }
            
            adminService.banUser(id, admin, banType, banDuration, reason);
            return ResponseEntity.ok("User banned successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error banning user: " + e.getMessage());
        }
    }

    @PostMapping("/users/{id}/unban")
    @ResponseBody
    public ResponseEntity<String> unbanUser(@PathVariable Long id, @RequestParam String reason) {
        try {
            User admin = getCurrentUser();
            
            // Check if user can manage users
            if (!adminService.canManageUsers(admin)) {
                return ResponseEntity.status(403).body("Access denied: Insufficient permissions");
            }
            
            adminService.unbanUser(id, admin, reason);
            return ResponseEntity.ok("User unbanned successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error unbanning user: " + e.getMessage());
        }
    }

    @PostMapping("/users/{id}/change-role")
    @ResponseBody
    public ResponseEntity<String> changeUserRole(@PathVariable Long id,
                                               @RequestParam String newRole,
                                               @RequestParam String reason) {
        try {
            User admin = getCurrentUser();
            
            // Check if user can manage users
            if (!adminService.canManageUsers(admin)) {
                return ResponseEntity.status(403).body("Access denied: Insufficient permissions");
            }
            
            User.Role role = User.Role.valueOf(newRole.toUpperCase());
            adminService.changeUserRole(id, admin, role, reason);
            return ResponseEntity.ok("User role changed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error changing user role: " + e.getMessage());
        }
    }

    // Activity Logs
    @GetMapping("/activity-logs")
    public String activityLogs(Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(required = false) String actionType,
                             @RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String endDate) {
        
        User currentUser = getCurrentUser();
        
        // Check if user can view security logs
        if (!adminService.canViewSecurityLogs(currentUser)) {
            return "redirect:/admin/dashboard?error=access_denied";
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> logs;
        
        if (actionType != null && !actionType.isEmpty() && startDate != null && endDate != null) {
            ActionType type = ActionType.valueOf(actionType.toUpperCase());
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            adminService.getActivityLogsByActionTypeAndDateRange(type, start, end);
            // Convert to page manually for simplicity
            logs = Page.empty();
        } else if (actionType != null && !actionType.isEmpty()) {
            ActionType type = ActionType.valueOf(actionType.toUpperCase());
            adminService.getActivityLogsByActionType(type);
            logs = Page.empty();
        } else if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);
            adminService.getActivityLogsByDateRange(start, end);
            logs = Page.empty();
        } else {
            logs = adminService.getActivityLogs(pageable);
        }
        
        model.addAttribute("logs", logs);
        model.addAttribute("actionTypes", ActionType.values());
        model.addAttribute("currentUser", currentUser);
        return "admin-activity-logs";
    }

    // API endpoints for dashboard data
    @GetMapping("/api/users")
    @ResponseBody
    public ResponseEntity<List<User>> getUsersApi(@RequestParam(required = false) String role,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String search) {
        try {
            User currentUser = getCurrentUser();
            
            // Check if user can manage users
            if (!adminService.canManageUsers(currentUser)) {
                return ResponseEntity.status(403).body(null);
            }
            
            List<User> users = adminService.getAllUsers();
            
            // Apply filters
            if (role != null && !role.isEmpty()) {
                users = users.stream()
                    .filter(user -> user.getRole() != null && user.getRole().name().equals(role))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            if (status != null && !status.isEmpty()) {
                if ("BANNED".equals(status)) {
                    users = users.stream()
                        .filter(User::isBanned)
                        .collect(java.util.stream.Collectors.toList());
                } else if ("ACTIVE".equals(status)) {
                    users = users.stream()
                        .filter(user -> !user.isBanned() && (user.getLockoutUntil() == null || user.getLockoutUntil().isBefore(java.time.LocalDateTime.now())))
                        .collect(java.util.stream.Collectors.toList());
                } else if ("LOCKED".equals(status)) {
                    users = users.stream()
                        .filter(user -> !user.isBanned() && user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(java.time.LocalDateTime.now()))
                        .collect(java.util.stream.Collectors.toList());
                }
            }
            
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase();
                users = users.stream()
                    .filter(user -> 
                        (user.getUsername() != null && user.getUsername().toLowerCase().contains(searchLower)) ||
                        (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchLower))
                    )
                    .collect(java.util.stream.Collectors.toList());
            }
            
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/api/dashboard-stats")
    @ResponseBody
    public ResponseEntity<AdminDashboardStats> getDashboardStatsApi() {
        try {
            User currentUser = getCurrentUser();
            
            // Check if user can access admin dashboard
            if (!adminService.canManageUsers(currentUser) && 
                !adminService.canModerateListings(currentUser) && 
                !adminService.canManageAuctions(currentUser) && 
                !adminService.canAccessReports(currentUser) && 
                !adminService.canViewSecurityLogs(currentUser)) {
                return ResponseEntity.status(403).body(null);
            }
            
            AdminDashboardStats stats = adminService.getDashboardStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/api/activity-logs")
    @ResponseBody
    public ResponseEntity<List<AuditLog>> getActivityLogsApi(@RequestParam(required = false) String actionType,
                                                             @RequestParam(required = false) String startDate,
                                                             @RequestParam(required = false) String endDate,
                                                             @RequestParam(required = false) String logId) {
        try {
            User currentUser = getCurrentUser();
            
            // Check if user can view security logs
            if (!adminService.canViewSecurityLogs(currentUser)) {
                return ResponseEntity.status(403).body(null);
            }
            
            List<AuditLog> logs;
            
            // If logId is provided, search by specific log ID first
            if (logId != null && !logId.isEmpty()) {
                try {
                    Long id = Long.parseLong(logId);
                    logs = adminService.getActivityLogs(PageRequest.of(0, 100)).getContent()
                        .stream()
                        .filter(log -> log.getLogId().equals(id))
                        .collect(java.util.stream.Collectors.toList());
                } catch (NumberFormatException e) {
                    // If logId is not a valid number, return empty list
                    logs = java.util.Collections.emptyList();
                }
            } else if (actionType != null && !actionType.isEmpty() && startDate != null && endDate != null) {
                ActionType type = ActionType.valueOf(actionType.toUpperCase());
                LocalDateTime start = LocalDateTime.parse(startDate);
                LocalDateTime end = LocalDateTime.parse(endDate);
                logs = adminService.getActivityLogsByActionTypeAndDateRange(type, start, end);
            } else if (actionType != null && !actionType.isEmpty()) {
                ActionType type = ActionType.valueOf(actionType.toUpperCase());
                logs = adminService.getActivityLogsByActionType(type);
            } else if (startDate != null && endDate != null) {
                LocalDateTime start = LocalDateTime.parse(startDate);
                LocalDateTime end = LocalDateTime.parse(endDate);
                logs = adminService.getActivityLogsByDateRange(start, end);
            } else {
                logs = adminService.getActivityLogs(PageRequest.of(0, 100)).getContent();
            }
            
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Vehicle Inspector Dashboard API endpoints
    @GetMapping("/api/vehicle-inspector/metrics")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> getVehicleInspectorMetrics() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(null);
            }
            
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);
            if (currentUser == null) {
                return ResponseEntity.status(401).body(null);
            }
            
            // Check if user is a vehicle inspector
            if (currentUser.getRole() != User.Role.VEHICLE_INSPECTOR) {
                return ResponseEntity.status(403).body(null);
            }
            
            java.util.Map<String, Object> metrics = new java.util.HashMap<>();
            
            // Get pending listings count
            long pendingListings = adminService.getPendingListingsCount();
            metrics.put("pendingListings", pendingListings);
            
            // Get approved today count
            long approvedToday = adminService.getApprovedTodayCount();
            metrics.put("approvedToday", approvedToday);
            
            // Get rejected today count
            long rejectedToday = adminService.getRejectedTodayCount();
            metrics.put("rejectedToday", rejectedToday);
            
            // Get total inspected count
            long totalInspected = adminService.getTotalInspectedCount();
            metrics.put("totalInspected", totalInspected);
            
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            System.err.println("Error in getVehicleInspectorMetrics: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/api/vehicle-inspector/listings")
    @ResponseBody
    public ResponseEntity<List<VehicleListingDTO>> getVehicleInspectorListings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer year) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                System.err.println("AdminController.getVehicleInspectorListings: No authentication found");
                return ResponseEntity.status(401).body(null);
            }
            
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);
            if (currentUser == null) {
                System.err.println("AdminController.getVehicleInspectorListings: No user found for username: " + username);
                return ResponseEntity.status(401).body(null);
            }
            
            System.out.println("AdminController.getVehicleInspectorListings: User authenticated: " + username + ", Role: " + currentUser.getRole());
            
            // Check if user is a vehicle inspector
            if (currentUser.getRole() != User.Role.VEHICLE_INSPECTOR) {
                System.err.println("AdminController.getVehicleInspectorListings: Access denied for role: " + currentUser.getRole());
                return ResponseEntity.status(403).body(null);
            }
            
            List<VehicleListing> listings = adminService.getVehicleInspectorListings(status, search, year);
            System.out.println("AdminController.getVehicleInspectorListings: Found " + listings.size() + " listings, converting to DTOs");
            
            // Convert entities to DTOs to avoid lazy loading issues
            List<VehicleListingDTO> listingDTOs = listings.stream()
                .map(VehicleListingDTO::new)
                .collect(java.util.stream.Collectors.toList());
            
            System.out.println("AdminController.getVehicleInspectorListings: Returning " + listingDTOs.size() + " DTOs");
            return ResponseEntity.ok(listingDTOs);
        } catch (Exception e) {
            System.err.println("Error in getVehicleInspectorListings: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/api/vehicle-inspector/listings/{id}")
    @ResponseBody
    public ResponseEntity<VehicleListingDTO> getVehicleInspectorListingDetails(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(null);
            }
            
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);
            if (currentUser == null) {
                return ResponseEntity.status(401).body(null);
            }
            
            // Check if user is a vehicle inspector
            if (currentUser.getRole() != User.Role.VEHICLE_INSPECTOR) {
                return ResponseEntity.status(403).body(null);
            }
            
            VehicleListing listing = adminService.getVehicleInspectorListingDetails(id);
            if (listing != null) {
                VehicleListingDTO listingDTO = new VehicleListingDTO(listing);
                return ResponseEntity.ok(listingDTO);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("Error in getVehicleInspectorListingDetails: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }


    // Helper method to get current user
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);
            if (user != null) {
                return user;
            }
        }
        throw new RuntimeException("No authenticated user found");
    }
}
