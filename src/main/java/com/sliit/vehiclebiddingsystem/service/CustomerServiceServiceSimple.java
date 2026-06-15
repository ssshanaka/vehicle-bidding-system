package com.sliit.vehiclebiddingsystem.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.entity.Notification;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.AuctionRepository;
import com.sliit.vehiclebiddingsystem.repository.NotificationRepository;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;

@Service
public class CustomerServiceServiceSimple {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Recent auction closures (last 7 days)
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            List<Auction> allClosedAuctions = auctionRepository.findByStatus(Auction.Status.CLOSED);
            List<Auction> recentClosedAuctions = allClosedAuctions.stream()
                .filter(auction -> auction.getEndTime().isAfter(weekAgo))
                .collect(Collectors.toList());
            stats.put("recentAuctionClosures", recentClosedAuctions.size());
            
            // Pending notifications (unread) - using a custom query
            long pendingNotifications = notificationRepository.findAll().stream()
                .filter(notification -> !notification.isRead())
                .count();
            stats.put("pendingNotifications", pendingNotifications);
            
            // User activity stats
            LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);
            List<User> recentActiveUsers = userRepository.findRecentlyActiveUsers(dayAgo);
            stats.put("recentActiveUsers", recentActiveUsers.size());
            
            // Winners notified (last 7 days) - using a custom query
            List<Notification> allNotifications = notificationRepository.findAll();
            List<Notification> winnerNotifications = allNotifications.stream()
                .filter(notification -> notification.getType() == Notification.Type.WINNER && 
                        notification.getSentAt().isAfter(weekAgo))
                .collect(Collectors.toList());
            stats.put("winnersNotified", winnerNotifications.size());
            
            // Customer queries stats - simplified
            stats.put("openQueries", 0);
            stats.put("inProgressQueries", 0);
            stats.put("urgentQueries", 0);
            
            // Reports generated (last 7 days) - simplified
            stats.put("reportsGenerated", 0);
            
        } catch (Exception e) {
            // Return default stats if there's an error
            stats.put("recentAuctionClosures", 0);
            stats.put("pendingNotifications", 0);
            stats.put("winnersNotified", 0);
            stats.put("openQueries", 0);
            stats.put("inProgressQueries", 0);
            stats.put("urgentQueries", 0);
            stats.put("reportsGenerated", 0);
            stats.put("recentActiveUsers", 0);
        }
        
        return stats;
    }

    public List<Map<String, Object>> getRecentAuctionClosures(int limit) {
        try {
            return auctionRepository.findByStatus(Auction.Status.CLOSED)
                .stream()
                .sorted((a, b) -> b.getEndTime().compareTo(a.getEndTime()))
                .limit(limit)
                .map(auction -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("auctionId", auction.getAuctionId());
                    data.put("vehicle", auction.getListing().getMake() + " " + 
                             auction.getListing().getModel() + " (" + auction.getListing().getYear() + ")");
                    data.put("endTime", auction.getEndTime());
                    data.put("winner", auction.getWinner() != null ? auction.getWinner().getUsername() : "No winner");
                    data.put("finalBid", auction.getHighestBid() != null ? auction.getHighestBid() : 0.0);
                    data.put("totalBids", auction.getBids() != null ? auction.getBids().size() : 0);
                    return data;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Map<String, Object>> getPendingNotifications(int limit) {
        try {
            return notificationRepository.findAll()
                .stream()
                .filter(notification -> !notification.isRead())
                .sorted((a, b) -> b.getSentAt().compareTo(a.getSentAt()))
                .limit(limit)
                .map(notification -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("notificationId", notification.getNotificationId());
                    data.put("type", notification.getType().name());
                    data.put("content", notification.getContent());
                    data.put("user", notification.getUser().getUsername());
                    data.put("sentAt", notification.getSentAt());
                    data.put("auction", notification.getAuction() != null ? 
                        notification.getAuction().getListing().getMake() + " " + 
                        notification.getAuction().getListing().getModel() : "N/A");
                    return data;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Map<String, Object>> getRecentUserActivity(int limit) {
        try {
            LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);
            return userRepository.findRecentlyActiveUsers(dayAgo)
                .stream()
                .limit(limit)
                .map(user -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("userId", user.getUserId());
                    data.put("username", user.getUsername());
                    data.put("email", user.getEmail());
                    data.put("lastLogin", user.getLastLoginTime());
                    data.put("role", user.getRole().name());
                    return data;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }
}

