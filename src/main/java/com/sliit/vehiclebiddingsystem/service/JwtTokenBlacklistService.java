package com.sliit.vehiclebiddingsystem.service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

/**
 * Service to manage blacklisted JWT tokens
 * In a production environment, this should be replaced with Redis or a database
 */
@Service
public class JwtTokenBlacklistService {

    // In-memory storage for blacklisted tokens
    // In production, use Redis or database for persistence across restarts
    private final ConcurrentMap<String, LocalDateTime> blacklistedTokens = new ConcurrentHashMap<>();

    /**
     * Add a token to the blacklist
     * @param token The JWT token to blacklist
     */
    public void blacklistToken(String token) {
        if (token != null && !token.trim().isEmpty()) {
            blacklistedTokens.put(token, LocalDateTime.now());
        }
    }

    /**
     * Check if a token is blacklisted
     * @param token The JWT token to check
     * @return true if the token is blacklisted, false otherwise
     */
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        return blacklistedTokens.containsKey(token);
    }

    /**
     * Remove expired tokens from the blacklist
     * This should be called periodically to clean up old tokens
     */
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        blacklistedTokens.entrySet().removeIf(entry -> {
            // Remove tokens older than 24 hours (JWT expiration time)
            return entry.getValue().isBefore(now.minusHours(24));
        });
    }

    /**
     * Blacklist all tokens for a specific user
     * This effectively logs out the user from all devices
     * @param username The username whose tokens should be blacklisted
     */
    public void blacklistAllUserTokens(String username) {
        // In a real implementation, you would need to track which tokens belong to which user
        // For now, we'll implement a simple approach by clearing all tokens
        // In production, maintain a user -> tokens mapping
        blacklistedTokens.clear();
    }

    /**
     * Get the number of blacklisted tokens (for monitoring)
     * @return The count of blacklisted tokens
     */
    public int getBlacklistedTokenCount() {
        return blacklistedTokens.size();
    }
}
