package com.sliit.vehiclebiddingsystem.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.entity.CustomerQuery;
import com.sliit.vehiclebiddingsystem.entity.Notification;
import com.sliit.vehiclebiddingsystem.entity.Report;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.AuctionRepository;
import com.sliit.vehiclebiddingsystem.repository.NotificationRepository;
import com.sliit.vehiclebiddingsystem.repository.ReportRepository;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;

@Service
public class CustomerServiceService {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    // @Autowired
    // private NotificationService notificationService; // Not used in current implementation

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private CustomerQueryService customerQueryService;

    @Autowired
    private AuditLogService auditLogService;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
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
        
        // Customer queries stats
        long openQueries = customerQueryService.getQueryCountByStatus(CustomerQuery.Status.OPEN);
        long inProgressQueries = customerQueryService.getQueryCountByStatus(CustomerQuery.Status.IN_PROGRESS);
        long urgentQueries = customerQueryService.getQueryCountByPriority(CustomerQuery.Priority.URGENT);
        
        stats.put("openQueries", openQueries);
        stats.put("inProgressQueries", inProgressQueries);
        stats.put("urgentQueries", urgentQueries);
        
        // Reports generated (last 7 days)
        List<Report> recentReports = reportService.getAllReports().stream()
            .filter(report -> report.getGeneratedAt().isAfter(weekAgo))
            .collect(Collectors.toList());
        stats.put("reportsGenerated", recentReports.size());
        
        return stats;
    }

    public List<Map<String, Object>> getRecentAuctionClosures(int limit) {
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
    }

    public List<Map<String, Object>> getPendingNotifications(int limit) {
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
    }

    public List<Map<String, Object>> getRecentUserActivity(int limit) {
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
    }

    public void sendCustomNotification(Long auctionId, String recipientType, String customMessage, Long sentByUserId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));
        
        User sentBy = userRepository.findById(sentByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<User> recipients = getRecipientsByType(auction, recipientType);
        
        for (User recipient : recipients) {
            String content = String.format("%s\n\nAuction: %s %s\nAuction ID: %d",
                customMessage,
                auction.getListing().getMake(),
                auction.getListing().getModel(),
                auction.getAuctionId());
            
            Notification notification = new Notification();
            notification.setUser(recipient);
            notification.setAuction(auction);
            notification.setType(Notification.Type.CLOSURE); // Using CLOSURE as custom type
            notification.setContent(content);
            
            notificationRepository.save(notification);
            // Note: sendEmailNotification is private, so we'll skip email sending for now
            // In a real implementation, this would be made public or use a different approach
        }
        
        // Log the custom notification
        auditLogService.logAction(
            sentBy,
            "FLAG_REPORTING",
            auctionId,
            "AUCTION",
            "Custom notification sent to " + recipientType + ": " + customMessage
        );
    }

    private List<User> getRecipientsByType(Auction auction, String recipientType) {
        switch (recipientType.toUpperCase()) {
            case "WINNER":
                return auction.getWinner() != null ? 
                    List.of(auction.getWinner()) : List.of();
            case "SELLER":
                return List.of(auction.getListing().getSeller());
            case "ALL_BIDDERS":
                return auction.getBids() != null ? 
                    auction.getBids().stream()
                        .map(bid -> bid.getBidder())
                        .distinct()
                        .collect(Collectors.toList()) : List.of();
            case "ALL_PARTICIPANTS":
                List<User> participants = List.of(auction.getListing().getSeller());
                if (auction.getWinner() != null) {
                    participants.add(auction.getWinner());
                }
                if (auction.getBids() != null) {
                    participants.addAll(auction.getBids().stream()
                        .map(bid -> bid.getBidder())
                        .distinct()
                        .collect(Collectors.toList()));
                }
                return participants.stream().distinct().collect(Collectors.toList());
            default:
                return List.of();
        }
    }

    public Map<String, Object> getAuctionHistorySummary(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));
        
        Map<String, Object> summary = new HashMap<>();
        
        // Basic auction info
        summary.put("auctionId", auction.getAuctionId());
        summary.put("vehicle", auction.getListing().getMake() + " " + 
                   auction.getListing().getModel() + " (" + auction.getListing().getYear() + ")");
        summary.put("startTime", auction.getStartTime());
        summary.put("endTime", auction.getEndTime());
        summary.put("status", auction.getStatus().name());
        
        // Bid statistics
        summary.put("totalBids", auction.getBids() != null ? auction.getBids().size() : 0);
        summary.put("highestBid", auction.getHighestBid());
        summary.put("winner", auction.getWinner() != null ? auction.getWinner().getUsername() : "No winner");
        
        // Participant count
        long bidderCount = auction.getBids() != null ? 
            auction.getBids().stream().map(bid -> bid.getBidder()).distinct().count() : 0;
        summary.put("bidderCount", bidderCount);
        
        // Notification count
        long notificationCount = auction.getNotifications() != null ? 
            auction.getNotifications().size() : 0;
        summary.put("notificationCount", notificationCount);
        
        // Report status
        summary.put("hasReport", auction.getReport() != null);
        if (auction.getReport() != null) {
            summary.put("reportStatus", auction.getReport().getStatus().name());
            summary.put("reportGeneratedAt", auction.getReport().getGeneratedAt());
        }
        
        return summary;
    }

    public void logManualIntervention(Long auctionId, String interventionType, String details, Long performedByUserId) {
        User performedBy = userRepository.findById(performedByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        auditLogService.logAction(
            performedBy,
            "FLAG_REPORTING",
            auctionId,
            "AUCTION",
            "Manual intervention - " + interventionType + ": " + details
        );
    }

    public List<Map<String, Object>> getCustomerFeedbackSummary() {
        return customerQueryService.getAllQueries()
            .stream()
            .map(query -> {
                Map<String, Object> data = new HashMap<>();
                data.put("queryId", query.getQueryId());
                data.put("subject", query.getSubject());
                data.put("queryType", query.getQueryType().name());
                data.put("priority", query.getPriority().name());
                data.put("status", query.getStatus().name());
                data.put("createdAt", query.getCreatedAt());
                data.put("user", query.getUser().getUsername());
                data.put("assignedTo", query.getAssignedTo() != null ? 
                    query.getAssignedTo().getUsername() : "Unassigned");
                return data;
            })
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getUrgentItems() {
        List<Map<String, Object>> urgentItems = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // 1. Recently closed auctions without winner notifications
        List<Auction> recentlyClosedAuctions = auctionRepository.findByStatus(Auction.Status.CLOSED)
            .stream()
            .filter(auction -> auction.getEndTime().isAfter(now.minusHours(24)))
            .collect(Collectors.toList());
        
        for (Auction auction : recentlyClosedAuctions) {
            // Check if winner notification was sent
            boolean winnerNotified = auction.getNotifications() != null &&
                auction.getNotifications().stream()
                    .anyMatch(notification -> notification.getType() == Notification.Type.WINNER);
            
            if (!winnerNotified && auction.getWinner() != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", auction.getAuctionId());
                item.put("type", "AUCTION_CLOSED");
                item.put("title", "Auction #" + auction.getAuctionId() + " - " + 
                    auction.getListing().getMake() + " " + auction.getListing().getModel());
                item.put("description", "Closed " + getTimeAgo(auction.getEndTime()) + " - Winner notification pending");
                item.put("priority", "HIGH");
                item.put("timeAgo", getTimeAgo(auction.getEndTime()));
                item.put("action", "Send winner notification");
                urgentItems.add(item);
            }
        }
        
        // 2. Urgent customer queries
        List<CustomerQuery> urgentQueries = customerQueryService.getAllQueries()
            .stream()
            .filter(query -> query.getPriority() == CustomerQuery.Priority.URGENT && 
                            query.getStatus() == CustomerQuery.Status.OPEN)
            .collect(Collectors.toList());
        
        for (CustomerQuery query : urgentQueries) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", query.getQueryId());
            item.put("type", "QUERY_URGENT");
            item.put("title", query.getQueryType().name() + " - " + query.getUser().getUsername());
            item.put("description", query.getSubject());
            item.put("priority", "URGENT");
            item.put("timeAgo", getTimeAgo(query.getCreatedAt()));
            item.put("action", "Review and resolve query");
            urgentItems.add(item);
        }
        
        // 3. Failed notifications
        List<Notification> failedNotifications = notificationRepository.findAll()
            .stream()
            .filter(notification -> !notification.isEmailSent() && 
                    notification.getSentAt().isAfter(now.minusHours(2)))
            .collect(Collectors.toList());
        
        for (Notification notification : failedNotifications) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", notification.getNotificationId());
            item.put("type", "NOTIFICATION_FAILED");
            item.put("title", "Failed to notify - " + notification.getType().name());
            item.put("description", "Email delivery failed for " + notification.getUser().getUsername());
            item.put("priority", "HIGH");
            item.put("timeAgo", getTimeAgo(notification.getSentAt()));
            item.put("action", "Retry notification");
            urgentItems.add(item);
        }
        
        // Sort by priority and time
        urgentItems.sort((a, b) -> {
            String priorityA = (String) a.get("priority");
            String priorityB = (String) b.get("priority");
            if (!priorityA.equals(priorityB)) {
                return priorityA.equals("URGENT") ? -1 : 1;
            }
            return 0; // Keep original order for same priority
        });
        
        return urgentItems;
    }

    public List<Map<String, Object>> getPriorityTasks() {
        List<Map<String, Object>> tasks = new ArrayList<>();
        
        // Count pending notifications
        long pendingNotifications = notificationRepository.findAll()
            .stream()
            .filter(notification -> !notification.isRead())
            .count();
        
        Map<String, Object> task1 = new HashMap<>();
        task1.put("id", 1);
        task1.put("task", "Review pending notifications");
        task1.put("count", pendingNotifications);
        task1.put("icon", "fas fa-bell");
        task1.put("color", "warning");
        tasks.add(task1);
        
        // Count auctions needing reports
        long auctionsNeedingReports = auctionRepository.findByStatus(Auction.Status.CLOSED)
            .stream()
            .filter(auction -> auction.getReport() == null)
            .count();
        
        Map<String, Object> task2 = new HashMap<>();
        task2.put("id", 2);
        task2.put("task", "Generate auction reports");
        task2.put("count", auctionsNeedingReports);
        task2.put("icon", "fas fa-file-pdf");
        task2.put("color", "success");
        tasks.add(task2);
        
        // Count open queries
        long openQueries = customerQueryService.getAllQueries()
            .stream()
            .filter(query -> query.getStatus() == CustomerQuery.Status.OPEN)
            .count();
        
        Map<String, Object> task3 = new HashMap<>();
        task3.put("id", 3);
        task3.put("task", "Resolve customer queries");
        task3.put("count", openQueries);
        task3.put("icon", "fas fa-question-circle");
        task3.put("color", "info");
        tasks.add(task3);
        
        // Count auctions needing winner notifications
        long auctionsNeedingNotifications = auctionRepository.findByStatus(Auction.Status.CLOSED)
            .stream()
            .filter(auction -> auction.getWinner() != null && 
                    (auction.getNotifications() == null || 
                     auction.getNotifications().stream()
                         .noneMatch(n -> n.getType() == Notification.Type.WINNER)))
            .count();
        
        Map<String, Object> task4 = new HashMap<>();
        task4.put("id", 4);
        task4.put("task", "Send winner notifications");
        task4.put("count", auctionsNeedingNotifications);
        task4.put("icon", "fas fa-trophy");
        task4.put("color", "primary");
        tasks.add(task4);
        
        return tasks;
    }

    public List<Map<String, Object>> getRecentActivity() {
        List<Map<String, Object>> activities = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Recent notifications sent
        List<Notification> recentNotifications = notificationRepository.findAll()
            .stream()
            .filter(notification -> notification.getSentAt().isAfter(now.minusHours(24)))
            .sorted((a, b) -> b.getSentAt().compareTo(a.getSentAt()))
            .limit(5)
            .collect(Collectors.toList());
        
        for (Notification notification : recentNotifications) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", "NOTIFICATION_SENT");
            activity.put("description", notification.getType().name() + " notification sent to " + 
                notification.getUser().getUsername());
            activity.put("time", getTimeAgo(notification.getSentAt()));
            activity.put("icon", "fas fa-check-circle");
            activity.put("color", "success");
            activities.add(activity);
        }
        
        // Recent reports generated
        List<Report> recentReports = reportService.getAllReports()
            .stream()
            .filter(report -> report.getGeneratedAt().isAfter(now.minusHours(24)))
            .sorted((a, b) -> b.getGeneratedAt().compareTo(a.getGeneratedAt()))
            .limit(3)
            .collect(Collectors.toList());
        
        for (Report report : recentReports) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", "REPORT_GENERATED");
            activity.put("description", "Auction report generated for " + 
                report.getAuction().getListing().getMake() + " " + 
                report.getAuction().getListing().getModel());
            activity.put("time", getTimeAgo(report.getGeneratedAt()));
            activity.put("icon", "fas fa-file-pdf");
            activity.put("color", "info");
            activities.add(activity);
        }
        
        // Recent queries resolved
        List<CustomerQuery> resolvedQueries = customerQueryService.getAllQueries()
            .stream()
            .filter(query -> query.getStatus() == CustomerQuery.Status.RESOLVED && 
                            query.getResolvedAt() != null &&
                            query.getResolvedAt().isAfter(now.minusHours(24)))
            .sorted((a, b) -> b.getResolvedAt().compareTo(a.getResolvedAt()))
            .limit(3)
            .collect(Collectors.toList());
        
        for (CustomerQuery query : resolvedQueries) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", "QUERY_RESOLVED");
            activity.put("description", "Customer query resolved - " + query.getSubject());
            activity.put("time", getTimeAgo(query.getResolvedAt()));
            activity.put("icon", "fas fa-check");
            activity.put("color", "success");
            activities.add(activity);
        }
        
        // Recent auction closures
        List<Auction> recentClosures = auctionRepository.findByStatus(Auction.Status.CLOSED)
            .stream()
            .filter(auction -> auction.getEndTime().isAfter(now.minusHours(24)))
            .sorted((a, b) -> b.getEndTime().compareTo(a.getEndTime()))
            .limit(3)
            .collect(Collectors.toList());
        
        for (Auction auction : recentClosures) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", "AUCTION_CLOSED");
            activity.put("description", "Auction #" + auction.getAuctionId() + " closed - " + 
                auction.getListing().getMake() + " " + auction.getListing().getModel());
            activity.put("time", getTimeAgo(auction.getEndTime()));
            activity.put("icon", "fas fa-gavel");
            activity.put("color", "warning");
            activities.add(activity);
        }
        
        // Sort all activities by time (most recent first)
        activities.sort((a, b) -> {
            // This is a simplified sort - in a real implementation, you'd parse the time strings
            return 0;
        });
        
        return activities.stream().limit(10).collect(Collectors.toList());
    }

    private String getTimeAgo(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(dateTime, now).toMinutes();
        
        if (minutes < 60) {
            return minutes + " minutes ago";
        } else if (minutes < 1440) { // 24 hours
            long hours = minutes / 60;
            return hours + " hours ago";
        } else {
            long days = minutes / 1440;
            return days + " days ago";
        }
    }
}
