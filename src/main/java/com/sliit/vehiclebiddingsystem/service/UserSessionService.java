package com.sliit.vehiclebiddingsystem.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;

/**
 * Service to manage user sessions and track active tokens
 * In a production environment, this should be replaced with Redis or a database
 */
@Service
public class UserSessionService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtTokenBlacklistService jwtTokenBlacklistService;

    // In-memory storage for user sessions
    // Key: username, Value: list of active tokens
    private final ConcurrentMap<String, List<String>> userSessions = new ConcurrentHashMap<>();
    
    // In-memory storage for token to user mapping
    // Key: token, Value: username
    private final ConcurrentMap<String, String> tokenToUser = new ConcurrentHashMap<>();

    /**
     * Register a new session for a user
     * @param username The username
     * @param token The JWT token
     */
    public void registerSession(String username, String token) {
        if (username != null && token != null && !token.trim().isEmpty()) {
            // Add token to user's session list
            userSessions.compute(username, (key, tokens) -> {
                if (tokens == null) {
                    return List.of(token);
                } else {
                    return tokens.stream()
                        .filter(t -> !jwtTokenBlacklistService.isTokenBlacklisted(t))
                        .collect(Collectors.toList());
                }
            });
            
            // Map token to user
            tokenToUser.put(token, username);
        }
    }

    /**
     * Remove a session for a user
     * @param username The username
     * @param token The JWT token
     */
    public void removeSession(String username, String token) {
        if (username != null && token != null) {
            userSessions.computeIfPresent(username, (key, tokens) -> 
                tokens.stream()
                    .filter(t -> !t.equals(token))
                    .collect(Collectors.toList())
            );
            tokenToUser.remove(token);
        }
    }

    /**
     * Force logout a specific user by blacklisting all their tokens
     * @param userId The user ID
     */
    public void forceLogoutUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            forceLogoutUser(user.getUsername());
        }
    }

    /**
     * Force logout a specific user by blacklisting all their tokens
     * @param username The username
     */
    public void forceLogoutUser(String username) {
        if (username != null) {
            List<String> userTokens = userSessions.get(username);
            if (userTokens != null) {
                // Blacklist all tokens for this user
                for (String token : userTokens) {
                    jwtTokenBlacklistService.blacklistToken(token);
                    tokenToUser.remove(token);
                }
                // Clear user's session list
                userSessions.remove(username);
            }
        }
    }

    /**
     * Force logout all users by blacklisting all tokens
     */
    public void forceLogoutAllUsers() {
        // Blacklist all tokens
        for (String token : tokenToUser.keySet()) {
            jwtTokenBlacklistService.blacklistToken(token);
        }
        // Clear all sessions
        userSessions.clear();
        tokenToUser.clear();
    }

    /**
     * Get active sessions for a user
     * @param username The username
     * @return List of active tokens for the user
     */
    public List<String> getUserSessions(String username) {
        if (username != null) {
            List<String> tokens = userSessions.get(username);
            if (tokens != null) {
                // Filter out blacklisted tokens
                return tokens.stream()
                    .filter(token -> !jwtTokenBlacklistService.isTokenBlacklisted(token))
                    .collect(Collectors.toList());
            }
        }
        return List.of();
    }

    /**
     * Get the username for a token
     * @param token The JWT token
     * @return The username associated with the token, or null if not found
     */
    public String getUsernameForToken(String token) {
        return tokenToUser.get(token);
    }

    /**
     * Clean up expired sessions (should be called periodically)
     */
    public void cleanupExpiredSessions() {
        // Remove blacklisted tokens from user sessions
        userSessions.forEach((username, tokens) -> {
            List<String> activeTokens = tokens.stream()
                .filter(token -> !jwtTokenBlacklistService.isTokenBlacklisted(token))
                .collect(Collectors.toList());
            
            if (activeTokens.isEmpty()) {
                userSessions.remove(username);
            } else {
                userSessions.put(username, activeTokens);
            }
        });
        
        // Clean up token to user mapping for blacklisted tokens
        tokenToUser.entrySet().removeIf(entry -> 
            jwtTokenBlacklistService.isTokenBlacklisted(entry.getKey())
        );
    }

    /**
     * Get the number of active sessions (for monitoring)
     * @return The count of active sessions
     */
    public int getActiveSessionCount() {
        return userSessions.size();
    }

    /**
     * Get the total number of active JWT tokens across all users
     * @return The total count of active JWT tokens
     */
    public int getTotalActiveTokenCount() {
        int totalActiveTokens = 0;
        for (List<String> userTokens : userSessions.values()) {
            if (userTokens != null) {
                long activeTokens = userTokens.stream()
                    .filter(token -> !jwtTokenBlacklistService.isTokenBlacklisted(token))
                    .count();
                totalActiveTokens += activeTokens;
            }
        }
        return totalActiveTokens;
    }

    /**
     * Get the number of unique users with active sessions
     * @return The count of users who have at least one active JWT token
     */
    public int getActiveUserCount() {
        int activeUsers = 0;
        for (List<String> userTokens : userSessions.values()) {
            if (userTokens != null) {
                boolean hasActiveTokens = userTokens.stream()
                    .anyMatch(token -> !jwtTokenBlacklistService.isTokenBlacklisted(token));
                if (hasActiveTokens) {
                    activeUsers++;
                }
            }
        }
        return activeUsers;
    }

    /**
     * Get detailed session information for a user (for debugging/monitoring)
     * @param username The username
     * @return Map containing session details
     */
    public Map<String, Object> getDetailedSessionInfo(String username) {
        Map<String, Object> sessionInfo = new HashMap<>();
        
        if (username != null) {
            List<String> tokens = userSessions.get(username);
            if (tokens != null) {
                List<String> activeTokens = tokens.stream()
                    .filter(token -> !jwtTokenBlacklistService.isTokenBlacklisted(token))
                    .collect(Collectors.toList());
                
                sessionInfo.put("totalTokens", tokens.size());
                sessionInfo.put("activeTokens", activeTokens.size());
                sessionInfo.put("blacklistedTokens", tokens.size() - activeTokens.size());
                sessionInfo.put("hasActiveSessions", !activeTokens.isEmpty());
            } else {
                sessionInfo.put("totalTokens", 0);
                sessionInfo.put("activeTokens", 0);
                sessionInfo.put("blacklistedTokens", 0);
                sessionInfo.put("hasActiveSessions", false);
            }
        }
        
        return sessionInfo;
    }
}
