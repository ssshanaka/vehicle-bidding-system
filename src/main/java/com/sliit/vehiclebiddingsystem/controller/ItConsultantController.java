package com.sliit.vehiclebiddingsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;
import com.sliit.vehiclebiddingsystem.service.ItConsultantService;

@RestController
@RequestMapping("/admin/api/it-consultant")
public class ItConsultantController {

    @Autowired
    private ItConsultantService itConsultantService;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * Get security metrics for the dashboard
     */
    @GetMapping("/security-metrics")
    public ResponseEntity<?> getSecurityMetrics() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getRole() != User.Role.IT_CONSULTANT) {
                return ResponseEntity.status(403).body("Access denied");
            }
            return ResponseEntity.ok(itConsultantService.getSecurityMetrics());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving security metrics: " + e.getMessage());
        }
    }

    /**
     * Get recent security logs
     */
    @GetMapping("/security-logs")
    public ResponseEntity<?> getSecurityLogs() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getRole() != User.Role.IT_CONSULTANT) {
                return ResponseEntity.status(403).body("Access denied");
            }
            return ResponseEntity.ok(itConsultantService.getRecentSecurityLogs());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving security logs: " + e.getMessage());
        }
    }

    /**
     * Get user analytics data
     */
    @GetMapping("/user-analytics")
    public ResponseEntity<?> getUserAnalytics() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getRole() != User.Role.IT_CONSULTANT) {
                return ResponseEntity.status(403).body("Access denied");
            }
            return ResponseEntity.ok(itConsultantService.getUserAnalytics());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving user analytics: " + e.getMessage());
        }
    }

    /**
     * Get locked accounts
     */
    @GetMapping("/locked-accounts")
    public ResponseEntity<?> getLockedAccounts() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getRole() != User.Role.IT_CONSULTANT) {
                return ResponseEntity.status(403).body("Access denied");
            }
            return ResponseEntity.ok(itConsultantService.getLockedAccounts());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving locked accounts: " + e.getMessage());
        }
    }

    /**
     * Get unauthorized access attempts
     */
    @GetMapping("/unauthorized-attempts")
    public ResponseEntity<?> getUnauthorizedAttempts() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getRole() != User.Role.IT_CONSULTANT) {
                return ResponseEntity.status(403).body("Access denied");
            }
            return ResponseEntity.ok(itConsultantService.getUnauthorizedAttempts());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving unauthorized attempts: " + e.getMessage());
        }
    }

    /**
     * Unlock a user account
     */
    @PostMapping("/unlock-user/{userId}")
    public ResponseEntity<?> unlockUser(@PathVariable Long userId) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getRole() != User.Role.IT_CONSULTANT) {
                return ResponseEntity.status(403).body("Access denied");
            }
            itConsultantService.unlockUser(userId);
            return ResponseEntity.ok("User unlocked successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error unlocking user: " + e.getMessage());
        }
    }

    /**
     * Get user statistics
     */
    @GetMapping("/user-statistics")
    public ResponseEntity<?> getUserStatistics() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getRole() != User.Role.IT_CONSULTANT) {
                return ResponseEntity.status(403).body("Access denied");
            }
            return ResponseEntity.ok(itConsultantService.getUserStatistics());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving user statistics: " + e.getMessage());
        }
    }

    /**
     * Get system performance metrics
     */
    @GetMapping("/system-performance")
    public ResponseEntity<?> getSystemPerformanceMetrics() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getRole() != User.Role.IT_CONSULTANT) {
                return ResponseEntity.status(403).body("Access denied");
            }
            return ResponseEntity.ok(itConsultantService.getSystemPerformanceMetrics());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving system performance metrics: " + e.getMessage());
        }
    }

    /**
     * Get active user sessions
     */
    @GetMapping("/active-sessions")
    public ResponseEntity<?> getActiveSessions() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getRole() != User.Role.IT_CONSULTANT) {
                return ResponseEntity.status(403).body("Access denied");
            }
            return ResponseEntity.ok(itConsultantService.getActiveSessions());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving active sessions: " + e.getMessage());
        }
    }

    /**
     * Force logout a specific user
     */
    @PostMapping("/force-logout-user/{userId}")
    public ResponseEntity<?> forceLogoutUser(@PathVariable Long userId) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getRole() != User.Role.IT_CONSULTANT) {
                return ResponseEntity.status(403).body("Access denied");
            }
            itConsultantService.forceLogoutUser(userId);
            return ResponseEntity.ok("User force logged out successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error force logging out user: " + e.getMessage());
        }
    }

    /**
     * Force logout all users
     */
    @PostMapping("/force-logout-all")
    public ResponseEntity<?> forceLogoutAll() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getRole() != User.Role.IT_CONSULTANT) {
                return ResponseEntity.status(403).body("Access denied");
            }
            itConsultantService.forceLogoutAll();
            return ResponseEntity.ok("All users force logged out successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error force logging out all users: " + e.getMessage());
        }
    }

    /**
     * Get detailed user information
     */
    @GetMapping("/user-details/{userId}")
    public ResponseEntity<?> getUserDetails(@PathVariable Long userId) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser.getRole() != User.Role.IT_CONSULTANT) {
                return ResponseEntity.status(403).body("Access denied");
            }
            return ResponseEntity.ok(itConsultantService.getUserDetails(userId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving user details: " + e.getMessage());
        }
    }

    /**
     * Helper method to get current authenticated user
     */
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
