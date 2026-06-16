package com.sliit.vehiclebiddingsystem.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sliit.vehiclebiddingsystem.repository.ResetTokenRepository;

@Service
public class TokenCleanupService {

    @Autowired
    private ResetTokenRepository resetTokenRepository;

    /**
     * Clean up expired reset tokens every hour
     * This scheduled task runs at the top of every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    @Transactional
    public void cleanupExpiredResetTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            resetTokenRepository.deleteExpiredTokens(now);
            
            System.out.println("[TOKEN_CLEANUP] Cleaned up expired reset tokens at " + now);
        } catch (Exception e) {
            System.err.println("[TOKEN_CLEANUP] Error cleaning up expired tokens: " + e.getMessage());
        }
    }

    /**
     * Manual cleanup method for expired tokens
     * Can be called programmatically if needed
     */
    @Transactional
    public void cleanupExpiredTokensManually() {
        LocalDateTime now = LocalDateTime.now();
        resetTokenRepository.deleteExpiredTokens(now);
    }
}
