package com.sliit.vehiclebiddingsystem.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final Map<String, RequestTracker> requestTrackers = new ConcurrentHashMap<>();

    // Rate limiting configuration for password reset requests
    private static final int MAX_REQUESTS_PER_HOUR = 3; // Maximum 3 password reset requests per hour
    private static final int MAX_REQUESTS_PER_DAY = 5;  // Maximum 5 password reset requests per day

    public boolean isAllowed(String identifier) {
        RequestTracker tracker = requestTrackers.computeIfAbsent(identifier, k -> new RequestTracker());
        return tracker.isAllowed();
    }

    public long getAvailableTokens(String identifier) {
        RequestTracker tracker = requestTrackers.get(identifier);
        return tracker != null ? tracker.getAvailableTokens() : MAX_REQUESTS_PER_HOUR;
    }

    public long getTimeUntilNextRefill(String identifier) {
        RequestTracker tracker = requestTrackers.get(identifier);
        if (tracker != null) {
            return tracker.getTimeUntilNextRefill();
        }
        return 0;
    }

    public void resetBucket(String identifier) {
        requestTrackers.remove(identifier);
    }

    private static class RequestTracker {
        private int hourlyRequests = 0;
        private int dailyRequests = 0;
        private LocalDateTime lastHourlyReset = LocalDateTime.now();
        private LocalDateTime lastDailyReset = LocalDateTime.now();

        public boolean isAllowed() {
            LocalDateTime now = LocalDateTime.now();
            
            // Reset hourly counter if an hour has passed
            if (now.isAfter(lastHourlyReset.plusHours(1))) {
                hourlyRequests = 0;
                lastHourlyReset = now;
            }
            
            // Reset daily counter if a day has passed
            if (now.isAfter(lastDailyReset.plusDays(1))) {
                dailyRequests = 0;
                lastDailyReset = now;
            }
            
            // Check if we're within limits
            if (hourlyRequests >= MAX_REQUESTS_PER_HOUR || dailyRequests >= MAX_REQUESTS_PER_DAY) {
                return false;
            }
            
            // Increment counters
            hourlyRequests++;
            dailyRequests++;
            return true;
        }

        public long getAvailableTokens() {
            LocalDateTime now = LocalDateTime.now();
            
            // Reset counters if time has passed
            if (now.isAfter(lastHourlyReset.plusHours(1))) {
                hourlyRequests = 0;
                lastHourlyReset = now;
            }
            
            if (now.isAfter(lastDailyReset.plusDays(1))) {
                dailyRequests = 0;
                lastDailyReset = now;
            }
            
            return Math.max(0, Math.min(MAX_REQUESTS_PER_HOUR - hourlyRequests, MAX_REQUESTS_PER_DAY - dailyRequests));
        }

        public long getTimeUntilNextRefill() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextHourlyReset = lastHourlyReset.plusHours(1);
            LocalDateTime nextDailyReset = lastDailyReset.plusDays(1);
            
            if (hourlyRequests >= MAX_REQUESTS_PER_HOUR) {
                return java.time.Duration.between(now, nextHourlyReset).getSeconds();
            }
            
            if (dailyRequests >= MAX_REQUESTS_PER_DAY) {
                return java.time.Duration.between(now, nextDailyReset).getSeconds();
            }
            
            return 0;
        }
    }
}