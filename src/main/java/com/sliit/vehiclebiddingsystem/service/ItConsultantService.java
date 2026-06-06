package com.sliit.vehiclebiddingsystem.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sliit.vehiclebiddingsystem.entity.AuditLog;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.AuditLogRepository;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;

@Service
public class ItConsultantService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private UserSessionService userSessionService;

    /**
     * Get security metrics for the dashboard
     */
    public Map<String, Object> getSecurityMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Failed login attempts (users with failed attempts)
        long failedLogins = userRepository.countUsersWithFailedAttempts();
        metrics.put("failedLogins", failedLogins);
        
        // Locked accounts
        long lockedAccounts = userRepository.countLockedAccounts();
        metrics.put("lockedAccounts", lockedAccounts);
        
        // Active sessions (total active JWT tokens across all users)
        int activeSessions = userSessionService.getTotalActiveTokenCount();
        int activeUsers = userSessionService.getActiveUserCount();
        metrics.put("activeSessions", activeSessions);
        metrics.put("activeUsers", activeUsers);
        
        // System health (calculated based on security events)
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        long securityEvents = auditLogRepository.countSecurityEventsSince(last24Hours);
        long unauthorizedEvents = auditLogRepository.countUnauthorizedAttemptsSince(last24Hours);
        
        // Calculate health percentage (simplified calculation)
        double healthPercentage = Math.max(0, 100 - (securityEvents * 2) - (unauthorizedEvents * 5));
        metrics.put("systemHealth", Math.round(healthPercentage) + "%");
        
        return metrics;
    }

    /**
     * Get recent security logs
     */
    public List<AuditLog> getRecentSecurityLogs() {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        return auditLogRepository.findRecentSecurityEvents(last24Hours);
    }

    /**
     * Get user analytics data
     */
    public List<User> getUserAnalytics() {
        return userRepository.findAll();
    }

    /**
     * Get locked accounts
     */
    public List<User> getLockedAccounts() {
        return userRepository.findLockedUsers();
    }

    /**
     * Get unauthorized access attempts
     */
    public List<AuditLog> getUnauthorizedAttempts() {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        return auditLogRepository.findUnauthorizedAttemptsSince(last24Hours);
    }

    /**
     * Get users with failed login attempts
     */
    public List<User> getUsersWithFailedAttempts() {
        return userRepository.findUsersWithFailedAttempts();
    }

    /**
     * Unlock a user account
     */
    public void unlockUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setLockoutUntil(null);
            user.setFailedAttempts(0);
            userRepository.save(user);
        }
    }

    /**
     * Get user statistics for analytics
     */
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Total users by role
        for (User.Role role : User.Role.values()) {
            long count = userRepository.countByRole(role);
            stats.put(role.name().toLowerCase() + "Count", count);
        }
        
        // Total users
        stats.put("totalUsers", userRepository.count());
        
        // Banned users
        stats.put("bannedUsers", userRepository.countByIsBannedTrue());
        
        // Users with failed attempts
        stats.put("usersWithFailedAttempts", userRepository.countUsersWithFailedAttempts());
        
        // Locked accounts
        stats.put("lockedAccounts", userRepository.countLockedAccounts());
        
        return stats;
    }

    /**
     * Get system performance metrics (simplified)
     */
    public Map<String, Object> getSystemPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Simulate some performance metrics
        // In a real system, these would come from monitoring tools
        metrics.put("serverResponseTime", "90ms");
        metrics.put("databasePerformance", "95%");
        metrics.put("memoryUsage", "70%");
        
        return metrics;
    }

    /**
     * Get active user sessions
     * Returns users who have active JWT tokens (currently logged in)
     */
    public List<User> getActiveSessions() {
        // Get all users and filter those with active sessions
        List<User> allUsers = userRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        
        return allUsers.stream()
            .filter(user -> !user.isBanned())
            .filter(user -> user.getLockoutUntil() == null || user.getLockoutUntil().isBefore(now))
            .filter(user -> !userSessionService.getUserSessions(user.getUsername()).isEmpty())
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Force logout a specific user
     * This invalidates all their JWT tokens and clears their session
     */
    public void forceLogoutUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            // Force logout the user (blacklist all their tokens)
            userSessionService.forceLogoutUser(userId);
            
            // Reset failed attempts and clear lockout
            user.setFailedAttempts(0);
            user.setLockoutUntil(null);
            userRepository.save(user);
            
            // Log the action
            auditLogRepository.save(createAuditLog("WARNING", userId, "USER", "User force logged out by IT Consultant"));
        }
    }

    /**
     * Force logout all users
     * This invalidates all JWT tokens and clears all sessions
     */
    public void forceLogoutAll() {
        // Force logout all users (blacklist all tokens)
        userSessionService.forceLogoutAllUsers();
        
        List<User> allUsers = userRepository.findAll();
        
        for (User user : allUsers) {
            // Reset failed attempts and clear lockout
            user.setFailedAttempts(0);
            user.setLockoutUntil(null);
            userRepository.save(user);
        }
        
        // Log the action
        auditLogRepository.save(createAuditLog("WARNING", 0L, "SYSTEM", "All users force logged out by IT Consultant"));
    }

    /**
     * Get detailed user information including security history
     */
    public Map<String, Object> getUserDetails(Long userId) {
        Map<String, Object> userDetails = new HashMap<>();
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            userDetails.put("error", "User not found");
            return userDetails;
        }
        
        // Basic user information
        userDetails.put("user", user);
        
        // Security information
        Map<String, Object> securityInfo = new HashMap<>();
        securityInfo.put("isBanned", user.isBanned());
        securityInfo.put("banType", user.getBanType());
        securityInfo.put("banDuration", user.getBanDuration());
        securityInfo.put("failedAttempts", user.getFailedAttempts());
        securityInfo.put("lockoutUntil", user.getLockoutUntil());
        securityInfo.put("lastLoginTime", user.getLastLoginTime());
        
        // Check if user has active sessions
        List<String> activeSessions = userSessionService.getUserSessions(user.getUsername());
        Map<String, Object> detailedSessionInfo = userSessionService.getDetailedSessionInfo(user.getUsername());
        
        securityInfo.put("activeSessions", activeSessions.size());
        securityInfo.put("hasActiveSessions", !activeSessions.isEmpty());
        securityInfo.put("activeSessionTokens", activeSessions); // For debugging/monitoring
        securityInfo.put("sessionDetails", detailedSessionInfo); // Detailed session information
        
        userDetails.put("securityInfo", securityInfo);
        
        // Get audit logs related to this user
        List<AuditLog> userAuditLogs = auditLogRepository.findByTargetTypeAndTargetId("USER", userId);
        userDetails.put("auditLogs", userAuditLogs);
        
        // Calculate security score (simplified)
        int securityScore = 100;
        if (user.isBanned()) securityScore -= 50;
        if (user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(LocalDateTime.now())) securityScore -= 30;
        if (user.getFailedAttempts() > 0) securityScore -= (user.getFailedAttempts() * 5);
        if (user.getFailedAttempts() > 3) securityScore -= 20;
        
        securityInfo.put("securityScore", Math.max(0, securityScore));
        
        return userDetails;
    }

    /**
     * Helper method to create audit log entries
     */
    private AuditLog createAuditLog(String actionType, Long targetId, String targetType, String reason) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActionType(AuditLog.ActionType.valueOf(actionType));
        auditLog.setTargetId(targetId);
        auditLog.setTargetType(targetType);
        auditLog.setReason(reason);
        auditLog.setTimestamp(LocalDateTime.now());
        return auditLog;
    }
}
