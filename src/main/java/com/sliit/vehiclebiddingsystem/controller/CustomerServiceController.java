package com.sliit.vehiclebiddingsystem.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.entity.Auction.Status;
import com.sliit.vehiclebiddingsystem.entity.CustomerQuery;
import com.sliit.vehiclebiddingsystem.entity.Notification;
import com.sliit.vehiclebiddingsystem.entity.Report;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.AuctionRepository;
import com.sliit.vehiclebiddingsystem.repository.BidRepository;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;
import com.sliit.vehiclebiddingsystem.service.CseNotificationService;
import com.sliit.vehiclebiddingsystem.service.CustomerQueryService;
import com.sliit.vehiclebiddingsystem.service.CustomerServiceService;
import com.sliit.vehiclebiddingsystem.service.EmailService;
import com.sliit.vehiclebiddingsystem.service.NotificationService;
import com.sliit.vehiclebiddingsystem.service.PdfGenerationService;
import com.sliit.vehiclebiddingsystem.service.ReportService;
import com.sliit.vehiclebiddingsystem.service.TicketConversationService;

@Controller
@RequestMapping("/customer-service")
@PreAuthorize("hasRole('CUSTOMER_SERVICE')")
public class CustomerServiceController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerServiceService customerServiceService;

    @Autowired
    private CustomerQueryService customerQueryService;

    @Autowired
    private PdfGenerationService pdfGenerationService;

    @Autowired
    private CseNotificationService cseNotificationService;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private TicketConversationService ticketConversationService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/dashboard")
    public String customerServiceDashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username);

        // Get dashboard statistics
        Map<String, Object> dashboardStats = customerServiceService.getDashboardStats();
        
        // Get recent auction closures
        List<Map<String, Object>> recentClosures = customerServiceService.getRecentAuctionClosures(5);
        
        // Get pending notifications
        List<Map<String, Object>> pendingNotifications = customerServiceService.getPendingNotifications(10);
        
        // Get recent user activity
        List<Map<String, Object>> recentActivity = customerServiceService.getRecentUserActivity(5);

        model.addAttribute("dashboardStats", dashboardStats);
        model.addAttribute("recentClosures", recentClosures);
        model.addAttribute("pendingNotifications", pendingNotifications);
        model.addAttribute("recentActivity", recentActivity);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("user", currentUser); // Add user attribute for header compatibility

        return "admin-customer-service-dashboard";
    }

    @GetMapping("/reports")
    public String customerServiceReports(Model model, 
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Report> reports = reportService.getAllReportsPaginated(pageable);

            model.addAttribute("reports", reports);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", reports.getTotalPages());

            return "auction-reports";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load reports: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/support")
    public String customerSupportDashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username);

        // Get dashboard statistics
        Map<String, Object> dashboardStats = customerServiceService.getDashboardStats();
        
        // Get recent customer queries
        List<CustomerQuery> recentQueries = customerQueryService.getRecentQueries(
            java.time.LocalDateTime.now().minusDays(7));
        
        // Get query statistics
        long openQueries = customerQueryService.getQueryCountByStatus(CustomerQuery.Status.OPEN);
        long urgentQueries = customerQueryService.getQueryCountByPriority(CustomerQuery.Priority.URGENT);
        long resolvedToday = customerQueryService.getQueryCountByStatus(CustomerQuery.Status.RESOLVED);

        model.addAttribute("dashboardStats", dashboardStats);
        model.addAttribute("recentQueries", recentQueries);
        model.addAttribute("openQueries", openQueries);
        model.addAttribute("urgentQueries", urgentQueries);
        model.addAttribute("resolvedToday", resolvedToday);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("user", currentUser); // Add user attribute for header compatibility

        return "admin-customer-support-dashboard";
    }

    @GetMapping("/api/notifications")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String date) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Notification> notifications = notificationService.getAllNotificationsPaginated(pageable);

            List<Map<String, Object>> notificationData = notifications.getContent().stream()
                .filter(notification -> {
                    // Filter by type if specified
                    if (type != null && !type.isEmpty()) {
                        if (!notification.getType().name().equals(type)) {
                            return false;
                        }
                    }
                    
                    // Filter by date if specified
                    if (date != null && !date.isEmpty()) {
                        try {
                            java.time.LocalDate filterDate = java.time.LocalDate.parse(date);
                            java.time.LocalDate notificationDate = notification.getSentAt().toLocalDate();
                            if (!notificationDate.equals(filterDate)) {
                                return false;
                            }
                        } catch (Exception e) {
                            // If date parsing fails, include the notification
                        }
                    }
                    
                    return true;
                })
                .map(notification -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", notification.getNotificationId());
                    data.put("type", notification.getType().name());
                    data.put("content", notification.getContent());
                    data.put("user", notification.getUser().getUsername());
                    data.put("sentAt", notification.getSentAt());
                    data.put("status", notification.isRead() ? "Read" : "Unread");
                    data.put("auction", notification.getAuction() != null ? 
                        notification.getAuction().getListing().getMake() + " " + 
                        notification.getAuction().getListing().getModel() : "N/A");
                    return data;
                })
                .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(notificationData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/reports")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Report> reports = reportService.getAllReportsPaginated(pageable);

            List<Map<String, Object>> reportData = reports.getContent().stream()
                .map(report -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", report.getReportId());
                    data.put("auction", report.getAuction().getListing().getMake() + " " + 
                             report.getAuction().getListing().getModel());
                    data.put("generatedAt", report.getGeneratedAt());
                    data.put("winner", report.getWinnerUsername());
                    data.put("finalBid", report.getHighestBidAmount() != null ? report.getHighestBidAmount() : 0.0);
                    data.put("status", report.getStatus() != null ? report.getStatus().name() : "ACTIVE");
                    data.put("auctionId", report.getAuction().getAuctionId());
                    return data;
                })
                .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(reportData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/auctions/closed")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getClosedAuctions() {
        try {
            List<Auction> closedAuctions = auctionRepository.findByStatus(Status.CLOSED);

            List<Map<String, Object>> auctionData = closedAuctions.stream()
                .map(auction -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", auction.getAuctionId());
                    data.put("vehicle", auction.getListing().getMake() + " " + 
                             auction.getListing().getModel() + " (" + auction.getListing().getYear() + ")");
                    data.put("endDate", auction.getEndTime());
                    data.put("winner", auction.getWinner() != null ? auction.getWinner().getUsername() : "No winner");
                    data.put("finalBid", auction.getHighestBid() != null ? auction.getHighestBid() : 0.0);
                    return data;
                })
                .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(auctionData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/customer-queries")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getCustomerQueries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<CustomerQuery> queries = customerQueryService.getAllQueriesPaginated(pageable);

            List<Map<String, Object>> queryData = queries.getContent().stream()
                .map(query -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", query.getQueryId());
                    data.put("subject", query.getSubject());
                    data.put("description", query.getDescription());
                    data.put("queryType", query.getQueryType().name());
                    data.put("priority", query.getPriority().name());
                    data.put("status", query.getStatus().name());
                    data.put("createdAt", query.getCreatedAt());
                    data.put("user", query.getUser().getUsername());
                    data.put("assignedTo", query.getAssignedTo() != null ? 
                        query.getAssignedTo().getUsername() : "Unassigned");
                    data.put("resolutionNotes", query.getResolutionNotes());
                    return data;
                })
                .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(queryData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/api/reports/generate/{auctionId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateReport(@PathVariable Long auctionId) {
        try {
            Report report = reportService.generateReport(auctionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reportId", report.getReportId());
            response.put("message", "Report generated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/dashboard/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            Map<String, Object> stats = customerServiceService.getDashboardStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/auctions/{auctionId}/history")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAuctionHistory(@PathVariable Long auctionId) {
        try {
            Map<String, Object> history = customerServiceService.getAuctionHistorySummary(auctionId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }


    @PostMapping("/api/queries/{queryId}/assign")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assignQuery(
            @PathVariable Long queryId,
            @RequestParam Long assignedToUserId) {
        try {
            CustomerQuery query = customerQueryService.assignQuery(queryId, assignedToUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Query assigned successfully");
            response.put("assignedTo", query.getAssignedTo().getUsername());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/queries/{queryId}/resolve")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resolveQuery(
            @PathVariable Long queryId,
            @RequestParam String resolutionNotes) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);

            CustomerQuery query = customerQueryService.resolveQuery(queryId, resolutionNotes, currentUser.getUserId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Query resolved successfully");
            response.put("resolvedAt", query.getResolvedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/queries/{queryId}/escalate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> escalateQuery(
            @PathVariable Long queryId,
            @RequestParam String escalationReason) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);

            CustomerQuery query = customerQueryService.escalateQuery(queryId, escalationReason, currentUser.getUserId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Query escalated successfully");
            response.put("priority", query.getPriority().name());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/queries/{queryId}/respond")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> respondToQuery(
            @PathVariable Long queryId,
            @RequestParam String responseMessage) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);

            CustomerQuery query = customerQueryService.getQueryById(queryId);
            
            // Auto-assign the CSE who responds
            if (query.getAssignedTo() == null) {
                query.setAssignedTo(currentUser);
            }
            
            // Set status to IN_PROGRESS if it's OPEN
            if (query.getStatus() == CustomerQuery.Status.OPEN) {
                query.setStatus(CustomerQuery.Status.IN_PROGRESS);
            }
            
            // Add CSE response to conversation using TicketConversationService
            ticketConversationService.addSupportResponse(query, currentUser, responseMessage);
            
            query.setUpdatedAt(java.time.LocalDateTime.now());
            CustomerQuery savedQuery = customerQueryService.updateQuery(query);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Response added successfully");
            response.put("assignedTo", savedQuery.getAssignedTo().getUsername());
            response.put("status", savedQuery.getStatus().name());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/queries/{queryId}/close")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> closeQuery(
            @PathVariable Long queryId,
            @RequestParam(required = false) String closingNotes) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);

            CustomerQuery query = customerQueryService.getQueryById(queryId);
            
            // Set status to CLOSED
            query.setStatus(CustomerQuery.Status.CLOSED);
            
            // Add closing notes to conversation if provided
            if (closingNotes != null && !closingNotes.trim().isEmpty()) {
                ticketConversationService.addSupportResponse(query, currentUser, "Ticket closed: " + closingNotes);
            } else {
                ticketConversationService.addSupportResponse(query, currentUser, "Ticket closed by support team");
            }
            
            query.setResolvedAt(java.time.LocalDateTime.now());
            query.setUpdatedAt(java.time.LocalDateTime.now());
            
            CustomerQuery savedQuery = customerQueryService.updateQuery(query);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ticket closed successfully");
            response.put("status", savedQuery.getStatus().name());
            response.put("closedAt", savedQuery.getResolvedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/queries/{queryId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getQueryById(@PathVariable Long queryId) {
        try {
            CustomerQuery query = customerQueryService.getQueryById(queryId);
            
            Map<String, Object> queryData = new HashMap<>();
            queryData.put("id", query.getQueryId());
            queryData.put("subject", query.getSubject());
            queryData.put("description", query.getDescription());
            queryData.put("queryType", query.getQueryType().name());
            queryData.put("priority", query.getPriority().name());
            queryData.put("status", query.getStatus().name());
            queryData.put("createdAt", query.getCreatedAt());
            queryData.put("updatedAt", query.getUpdatedAt());
            queryData.put("user", query.getUser().getUsername());
            queryData.put("assignedTo", query.getAssignedTo() != null ? 
                query.getAssignedTo().getUsername() : null);
            queryData.put("resolutionNotes", query.getResolutionNotes());
            queryData.put("resolvedAt", query.getResolvedAt());
            queryData.put("relatedAuctionId", query.getRelatedAuctionId());
            queryData.put("relatedListingId", query.getRelatedListingId());

            return ResponseEntity.ok(queryData);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/queries/{queryId}/conversations")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getTicketConversations(@PathVariable Long queryId) {
        try {
            List<com.sliit.vehiclebiddingsystem.entity.TicketConversation> conversations = 
                ticketConversationService.getConversationsByQueryId(queryId);
            
            List<Map<String, Object>> conversationData = conversations.stream()
                .map(conversation -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", conversation.getConversationId());
                    data.put("message", conversation.getMessage());
                    data.put("messageType", conversation.getMessageType().name());
                    data.put("createdAt", conversation.getCreatedAt());
                    data.put("user", conversation.getUser() != null ? 
                        conversation.getUser().getUsername() : "System");
                    return data;
                })
                .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(conversationData);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(List.of(response));
        }
    }

    @GetMapping("/api/recent-closures")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getRecentClosures() {
        try {
            List<Map<String, Object>> recentClosures = customerServiceService.getRecentAuctionClosures(10);
            return ResponseEntity.ok(recentClosures);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(List.of(response));
        }
    }

    @GetMapping("/api/pending-notifications")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getPendingNotifications() {
        try {
            List<Map<String, Object>> pendingNotifications = customerServiceService.getPendingNotifications(20);
            return ResponseEntity.ok(pendingNotifications);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(List.of(response));
        }
    }

    @GetMapping("/api/notification-stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotificationStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Get actual notification logs to calculate real statistics
            List<com.sliit.vehiclebiddingsystem.entity.NotificationLog> notificationLogs = cseNotificationService.getAllNotificationLogs();
            
            int totalSent = notificationLogs.size();
            int successful = (int) notificationLogs.stream()
                .filter(log -> "SENT".equals(log.getStatus().name()))
                .count();
            int failed = (int) notificationLogs.stream()
                .filter(log -> "FAILED".equals(log.getStatus().name()))
                .count();
            
            stats.put("totalSent", totalSent);
            stats.put("successfulDeliveries", successful);
            stats.put("failedDeliveries", failed);
            stats.put("averageDeliveryTime", "2.3");
            
            // Calculate types breakdown from actual data
            Map<String, Integer> typesBreakdown = new HashMap<>();
            notificationLogs.forEach(log -> {
                String type = log.getNotificationType().name();
                if (type != null) {
                    String displayType = type.replace("_", " ");
                    typesBreakdown.put(displayType, typesBreakdown.getOrDefault(displayType, 0) + 1);
                }
            });
            stats.put("typesBreakdown", typesBreakdown);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/queries")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getCustomerQueries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<CustomerQuery> queries;

            // Apply filters
            if (status != null && !status.isEmpty()) {
                queries = customerQueryService.getQueriesByStatusPaginated(
                    CustomerQuery.Status.valueOf(status), pageable);
            } else if (priority != null && !priority.isEmpty()) {
                queries = customerQueryService.getQueriesByPriorityPaginated(
                    CustomerQuery.Priority.valueOf(priority), pageable);
            } else if (type != null && !type.isEmpty()) {
                queries = customerQueryService.getQueriesByTypePaginated(
                    CustomerQuery.QueryType.valueOf(type), pageable);
            } else if (search != null && !search.isEmpty()) {
                queries = customerQueryService.searchQueriesPaginated(search, pageable);
            } else {
                queries = customerQueryService.getAllQueriesPaginated(pageable);
            }

            List<Map<String, Object>> queryData = queries.getContent().stream()
                .map(query -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", query.getQueryId());
                    data.put("subject", query.getSubject());
                    data.put("description", query.getDescription());
                    data.put("queryType", query.getQueryType().name());
                    data.put("priority", query.getPriority().name());
                    data.put("status", query.getStatus().name());
                    data.put("createdAt", query.getCreatedAt());
                    data.put("user", query.getUser().getUsername());
                    data.put("assignedTo", query.getAssignedTo() != null ? 
                        query.getAssignedTo().getUsername() : "Unassigned");
                    data.put("resolutionNotes", query.getResolutionNotes());
                    return data;
                })
                .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(queryData);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(List.of(response));
        }
    }

    @GetMapping("/api/users")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getUsers() {
        try {
            List<User> users = userRepository.findAll();
            
            List<Map<String, Object>> userData = users.stream()
                .map(user -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("userId", user.getUserId());
                    data.put("username", user.getUsername());
                    data.put("email", user.getEmail());
                    data.put("role", user.getRole().name());
                    data.put("phone", user.getPhone());
                    return data;
                })
                .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(userData);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(List.of(response));
        }
    }

    @PostMapping("/api/auctions/{auctionId}/intervention")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> logManualIntervention(
            @PathVariable Long auctionId,
            @RequestParam String interventionType,
            @RequestParam String details) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);

            customerServiceService.logManualIntervention(auctionId, interventionType, details, currentUser.getUserId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Manual intervention logged successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/reports/{reportId}/details")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getReportDetails(@PathVariable Long reportId) {
        try {
            Report report = reportService.getReportById(reportId);
            if (report == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> details = new HashMap<>();
            details.put("reportId", report.getReportId());
            details.put("auctionId", report.getAuction().getAuctionId());
            details.put("vehicle", report.getAuction().getListing().getMake() + " " + 
                      report.getAuction().getListing().getModel() + " (" + report.getAuction().getListing().getYear() + ")");
            details.put("generatedAt", report.getGeneratedAt());
            details.put("winner", report.getWinnerUsername());
            details.put("seller", report.getSellerUsername());
            details.put("finalBid", report.getHighestBidAmount());
            details.put("totalBids", report.getTotalBids());
            details.put("status", report.getStatus().name());
            details.put("bidSummary", report.getBidSummary());
            details.put("participantList", report.getParticipantList());
            details.put("vehicleDetails", report.getVehicleDetails());
            details.put("timeline", report.getTimeline());
            details.put("contactDetails", report.getContactDetails());

            return ResponseEntity.ok(details);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/dashboard/urgent-items")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getUrgentItems() {
        try {
            List<Map<String, Object>> urgentItems = customerServiceService.getUrgentItems();
            return ResponseEntity.ok(urgentItems);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/dashboard/priority-tasks")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getPriorityTasks() {
        try {
            List<Map<String, Object>> priorityTasks = customerServiceService.getPriorityTasks();
            return ResponseEntity.ok(priorityTasks);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/dashboard/recent-activity")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getRecentActivity() {
        try {
            List<Map<String, Object>> recentActivity = customerServiceService.getRecentActivity();
            return ResponseEntity.ok(recentActivity);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/reports/{reportId}/detailed")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDetailedReport(@PathVariable Long reportId) {
        try {
            Report report = reportService.getReportById(reportId);
            if (report == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> detailedReport = new HashMap<>();
            detailedReport.put("reportId", report.getReportId());
            detailedReport.put("auctionId", report.getAuction().getAuctionId());
            detailedReport.put("vehicle", report.getAuction().getListing().getMake() + " " + 
                      report.getAuction().getListing().getModel() + " (" + report.getAuction().getListing().getYear() + ")");
            detailedReport.put("generatedAt", report.getGeneratedAt());
            detailedReport.put("status", report.getStatus().name());
            
            // Vehicle details - use enhanced fields if available, otherwise fall back to auction data
            detailedReport.put("vehicleCondition", 
                report.getVehicleCondition() != null ? report.getVehicleCondition() : 
                report.getAuction().getListing().getCondition().name());
            detailedReport.put("vehicleFuelType", 
                report.getVehicleFuelType() != null ? report.getVehicleFuelType() : 
                report.getAuction().getListing().getFuelType());
            detailedReport.put("vehicleTransmission", 
                report.getVehicleTransmission() != null ? report.getVehicleTransmission() : 
                report.getAuction().getListing().getTransmission());
            detailedReport.put("vehicleMileage", 
                report.getVehicleMileage() != null ? report.getVehicleMileage() : 
                report.getAuction().getListing().getMileage());
            detailedReport.put("vehicleDescription", report.getAuction().getListing().getDescription());
            
            // Winner information - use enhanced fields if available, otherwise fall back to auction data
            detailedReport.put("winnerUsername", report.getWinnerUsername());
            if (report.getAuction().getWinner() != null) {
                detailedReport.put("winnerEmail", 
                    report.getWinnerEmail() != null ? report.getWinnerEmail() : 
                    report.getAuction().getWinner().getEmail());
                detailedReport.put("winnerPhone", 
                    report.getWinnerPhone() != null ? report.getWinnerPhone() : 
                    report.getAuction().getWinner().getPhone());
            } else {
                detailedReport.put("winnerEmail", null);
                detailedReport.put("winnerPhone", null);
            }
            
            // Seller information - use enhanced fields if available, otherwise fall back to auction data
            detailedReport.put("sellerUsername", report.getSellerUsername());
            detailedReport.put("sellerEmail", 
                report.getSellerEmail() != null ? report.getSellerEmail() : 
                report.getAuction().getListing().getSeller().getEmail());
            detailedReport.put("sellerPhone", 
                report.getSellerPhone() != null ? report.getSellerPhone() : 
                report.getAuction().getListing().getSeller().getPhone());
            
            // Auction details
            detailedReport.put("finalBid", report.getHighestBidAmount());
            detailedReport.put("totalBids", report.getTotalBids());
            detailedReport.put("duration", report.getAuctionDurationMinutes());
            detailedReport.put("startTime", report.getAuction().getStartTime());
            detailedReport.put("endTime", report.getAuction().getEndTime());
            
            // Bid history and other details
            detailedReport.put("bidHistory", report.getBidHistory());
            detailedReport.put("bidSummary", report.getBidSummary());
            detailedReport.put("participantList", report.getParticipantList());
            detailedReport.put("timeline", report.getTimeline());
            detailedReport.put("contactDetails", report.getContactDetails());
            
            // Review information
            detailedReport.put("cseNotes", report.getCseNotes());
            detailedReport.put("reviewedBy", report.getReviewedBy());
            detailedReport.put("reviewedAt", report.getReviewedAt());
            detailedReport.put("isApproved", report.getIsApproved());

            return ResponseEntity.ok(detailedReport);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/api/reports/{reportId}/review")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reviewReport(
            @PathVariable Long reportId,
            @RequestParam String reviewedBy,
            @RequestParam(required = false) String cseNotes) {
        try {
            Report report = reportService.reviewReport(reportId, reviewedBy, cseNotes);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Report reviewed successfully");
            response.put("reportId", report.getReportId());
            response.put("status", report.getStatus().name());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/reports/{reportId}/approve")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> approveReport(
            @PathVariable Long reportId,
            @RequestParam String approvedBy) {
        try {
            Report report = reportService.approveReport(reportId, approvedBy);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Report approved successfully");
            response.put("reportId", report.getReportId());
            response.put("status", report.getStatus().name());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/reports/{reportId}/update-notes")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateReportNotes(
            @PathVariable Long reportId,
            @RequestParam String cseNotes) {
        try {
            Report report = reportService.updateReportNotes(reportId, cseNotes);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Report notes updated successfully");
            response.put("reportId", report.getReportId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/reports/{reportId}/export-pdf")
    @ResponseBody
    public ResponseEntity<byte[]> exportReportToPdf(@PathVariable Long reportId) {
        try {
            byte[] pdfBytes = pdfGenerationService.generateReportPdf(reportId);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=auction-report-" + reportId + ".pdf")
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Enhanced Notification Endpoints for CSE Dashboard

    @PostMapping("/api/notifications/send-winner")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendWinnerNotification(
            @RequestParam Long auctionId,
            @RequestParam(required = false) String customMessage) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);

            com.sliit.vehiclebiddingsystem.entity.NotificationLog log = 
                cseNotificationService.sendWinnerNotification(auctionId, currentUser.getUserId(), customMessage);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Winner notification sent successfully");
            response.put("logId", log.getLogId());
            response.put("recipientCount", log.getRecipientCount());
            response.put("status", log.getStatus().name());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/notifications/send-outbid")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendOutbidNotifications(
            @RequestParam Long auctionId,
            @RequestParam(required = false) String customMessage) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);

            com.sliit.vehiclebiddingsystem.entity.NotificationLog log = 
                cseNotificationService.sendOutbidNotifications(auctionId, currentUser.getUserId(), customMessage);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Outbid notifications sent successfully");
            response.put("logId", log.getLogId());
            response.put("recipientCount", log.getRecipientCount());
            response.put("status", log.getStatus().name());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/notifications/send-auction-closed")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendAuctionClosedNotification(
            @RequestParam Long auctionId,
            @RequestParam(required = false) String customMessage) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);

            com.sliit.vehiclebiddingsystem.entity.NotificationLog log = 
                cseNotificationService.sendAuctionClosedNotification(auctionId, currentUser.getUserId(), customMessage);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Auction closed notifications sent successfully");
            response.put("logId", log.getLogId());
            response.put("recipientCount", log.getRecipientCount());
            response.put("status", log.getStatus().name());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/notifications/send-payment-reminder")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendPaymentReminder(
            @RequestParam Long auctionId,
            @RequestParam(required = false) String customMessage) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);

            com.sliit.vehiclebiddingsystem.entity.NotificationLog log = 
                cseNotificationService.sendPaymentReminder(auctionId, currentUser.getUserId(), customMessage);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment reminder sent successfully");
            response.put("logId", log.getLogId());
            response.put("recipientCount", log.getRecipientCount());
            response.put("status", log.getStatus().name());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/notifications/send-contact-seller")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendContactSellerInfo(
            @RequestParam Long auctionId,
            @RequestParam(required = false) String customMessage) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);

            com.sliit.vehiclebiddingsystem.entity.NotificationLog log = 
                cseNotificationService.sendContactSellerInfo(auctionId, currentUser.getUserId(), customMessage);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Contact seller information sent successfully");
            response.put("logId", log.getLogId());
            response.put("recipientCount", log.getRecipientCount());
            response.put("status", log.getStatus().name());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/notifications/send-custom")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendCustomNotification(
            @RequestParam Long auctionId,
            @RequestParam String recipientType,
            @RequestParam String customMessage) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);

            com.sliit.vehiclebiddingsystem.entity.NotificationLog.RecipientType recipientTypeEnum = 
                com.sliit.vehiclebiddingsystem.entity.NotificationLog.RecipientType.valueOf(recipientType.toUpperCase());

            com.sliit.vehiclebiddingsystem.entity.NotificationLog log = 
                cseNotificationService.sendCustomNotification(auctionId, currentUser.getUserId(), recipientTypeEnum, customMessage);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Custom notification sent successfully");
            response.put("logId", log.getLogId());
            response.put("recipientCount", log.getRecipientCount());
            response.put("status", log.getStatus().name());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/notification-logs")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getNotificationLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long auctionId,
            @RequestParam(required = false) String type) {
        try {
            List<com.sliit.vehiclebiddingsystem.entity.NotificationLog> logs;
            
            if (auctionId != null) {
                logs = cseNotificationService.getNotificationLogsForAuction(auctionId);
            } else {
                logs = cseNotificationService.getAllNotificationLogs();
            }

            List<Map<String, Object>> logData = logs.stream()
                .filter(log -> {
                    if (type != null && !type.isEmpty()) {
                        return log.getNotificationType().name().equals(type);
                    }
                    return true;
                })
                .map(log -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("logId", log.getLogId());
                    data.put("auctionId", log.getAuction().getAuctionId());
                    data.put("vehicle", log.getAuction().getListing().getMake() + " " + 
                             log.getAuction().getListing().getModel() + " (" + 
                             log.getAuction().getListing().getYear() + ")");
                    data.put("notificationType", log.getNotificationType().name());
                    data.put("recipientType", log.getRecipientType().name());
                    data.put("customMessage", log.getCustomMessage());
                    data.put("recipientCount", log.getRecipientCount());
                    data.put("sentAt", log.getSentAt());
                    data.put("sentBy", log.getSentBy().getUsername());
                    data.put("emailSent", log.getEmailSent());
                    data.put("emailSentAt", log.getEmailSentAt());
                    data.put("status", log.getStatus().name());
                    data.put("errorMessage", log.getErrorMessage());
                    data.put("recipientDetails", log.getRecipientDetails());
                    return data;
                })
                .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(logData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/notification-logs/{logId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotificationLogDetails(@PathVariable Long logId) {
        try {
            // This would require adding a getById method to CseNotificationService
            // For now, we'll return a placeholder response
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Method not implemented yet");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/auctions/{auctionId}/details")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAuctionDetails(@PathVariable Long auctionId) {
        try {
            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> new RuntimeException("Auction not found"));

            // Calculate highest bid from actual bids
            Double highestBid = bidRepository.findHighestBidByAuctionId(auctionId);
            if (highestBid == null) {
                highestBid = 0.0;
            }

            Map<String, Object> details = new HashMap<>();
            details.put("auctionId", auction.getAuctionId());
            details.put("vehicle", auction.getListing().getMake() + " " + 
                      auction.getListing().getModel() + " (" + auction.getListing().getYear() + ")");
            details.put("startTime", auction.getStartTime());
            details.put("endTime", auction.getEndTime());
            details.put("status", auction.getStatus().name());
            details.put("highestBid", highestBid);
            details.put("winner", auction.getWinner() != null ? auction.getWinner().getUsername() : "No winner");
            details.put("seller", auction.getListing().getSeller().getUsername());
            details.put("totalBids", auction.getBids() != null ? auction.getBids().size() : 0);
            details.put("listingId", auction.getListing().getListingId());
            details.put("vehicleDescription", auction.getListing().getDescription());
            details.put("vehicleCondition", auction.getListing().getCondition().name());
            details.put("vehicleFuelType", auction.getListing().getFuelType());
            details.put("vehicleTransmission", auction.getListing().getTransmission());
            details.put("vehicleMileage", auction.getListing().getMileage());
            
            // Seller contact information
            Map<String, Object> sellerInfo = new HashMap<>();
            sellerInfo.put("username", auction.getListing().getSeller().getUsername());
            sellerInfo.put("email", auction.getListing().getSeller().getEmail());
            sellerInfo.put("phone", auction.getListing().getSeller().getPhone());
            details.put("sellerInfo", sellerInfo);
            
            // Winner contact information (if exists)
            if (auction.getWinner() != null) {
                Map<String, Object> winnerInfo = new HashMap<>();
                winnerInfo.put("username", auction.getWinner().getUsername());
                winnerInfo.put("email", auction.getWinner().getEmail());
                winnerInfo.put("phone", auction.getWinner().getPhone());
                details.put("winnerInfo", winnerInfo);
            }

            return ResponseEntity.ok(details);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/auctions/for-notifications")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAuctionsForNotifications() {
        try {
            List<Map<String, Object>> auctions = auctionRepository.findAll()
                .stream()
                .filter(auction -> auction.getStatus() == Auction.Status.CLOSED || auction.getStatus() == Auction.Status.ACTIVE)
                .sorted((a, b) -> b.getEndTime().compareTo(a.getEndTime()))
                .map(auction -> {
                    Map<String, Object> auctionData = new HashMap<>();
                    auctionData.put("auctionId", auction.getAuctionId());
                    auctionData.put("vehicle", auction.getListing().getMake() + " " + 
                                 auction.getListing().getModel() + " (" + auction.getListing().getYear() + ")");
                    auctionData.put("status", auction.getStatus().name());
                    auctionData.put("endTime", auction.getEndTime());
                    auctionData.put("winner", auction.getWinner() != null ? auction.getWinner().getUsername() : "No winner");
                    
                    // Calculate highest bid
                    Double highestBid = bidRepository.findHighestBidByAuctionId(auction.getAuctionId());
                    auctionData.put("highestBid", highestBid != null ? highestBid : 0.0);
                    
                    return auctionData;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(auctions);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(List.of(response));
        }
    }

    @GetMapping("/api/auctions/{auctionId}/notification-summary")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAuctionNotificationSummary(@PathVariable Long auctionId) {
        try {
            List<com.sliit.vehiclebiddingsystem.entity.NotificationLog> logs = 
                cseNotificationService.getNotificationLogsForAuction(auctionId);

            Map<String, Object> summary = new HashMap<>();
            summary.put("auctionId", auctionId);
            summary.put("totalNotificationsSent", logs.size());
            
            // Count by type
            Map<String, Long> typeCounts = logs.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    log -> log.getNotificationType().name(),
                    java.util.stream.Collectors.counting()
                ));
            summary.put("notificationsByType", typeCounts);
            
            // Count by status
            Map<String, Long> statusCounts = logs.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    log -> log.getStatus().name(),
                    java.util.stream.Collectors.counting()
                ));
            summary.put("notificationsByStatus", statusCounts);
            
            // Total recipients
            int totalRecipients = logs.stream()
                .mapToInt(log -> log.getRecipientCount())
                .sum();
            summary.put("totalRecipients", totalRecipients);
            
            // Email success rate
            long emailSentCount = logs.stream()
                .filter(log -> log.getEmailSent())
                .count();
            double emailSuccessRate = logs.isEmpty() ? 0.0 : (double) emailSentCount / logs.size() * 100;
            summary.put("emailSuccessRate", Math.round(emailSuccessRate * 100.0) / 100.0);
            
            // Recent notifications
            List<Map<String, Object>> recentNotifications = logs.stream()
                .sorted((a, b) -> b.getSentAt().compareTo(a.getSentAt()))
                .limit(5)
                .map(log -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("logId", log.getLogId());
                    data.put("type", log.getNotificationType().name());
                    data.put("recipientCount", log.getRecipientCount());
                    data.put("sentAt", log.getSentAt());
                    data.put("status", log.getStatus().name());
                    return data;
                })
                .collect(java.util.stream.Collectors.toList());
            summary.put("recentNotifications", recentNotifications);

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/notifications/{notificationId}/resend")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resendNotification(@PathVariable Long notificationId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username);

            // Try to find the notification first
            com.sliit.vehiclebiddingsystem.entity.Notification notification = 
                notificationService.getNotificationById(notificationId);
            
            if (notification == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Notification not found");
                return ResponseEntity.badRequest().body(response);
            }

            // Check if user has permission to resend this notification
            // For now, we'll allow CSE to resend any notification
            // In a more secure implementation, you might want to add additional checks

            // Resend the notification by creating a new one with the same content
            com.sliit.vehiclebiddingsystem.entity.Notification newNotification = new com.sliit.vehiclebiddingsystem.entity.Notification();
            newNotification.setUser(notification.getUser());
            newNotification.setAuction(notification.getAuction());
            newNotification.setType(notification.getType());
            newNotification.setContent(notification.getContent());
            newNotification.setSentAt(java.time.LocalDateTime.now());
            newNotification.setRead(false);
            newNotification.setEmailSent(false);

            com.sliit.vehiclebiddingsystem.entity.Notification savedNotification = 
                notificationService.saveNotification(newNotification);

            // Send email if the original had email
            if (notification.isEmailSent()) {
                try {
                    String subject = "Vehicle Bidding System - " + notification.getType().name();
                    String body = notification.getContent() + 
                                 "\n\nAuction ID: " + notification.getAuction().getAuctionId() +
                                 "\nSent at: " + savedNotification.getSentAt();
                    
                    emailService.send(notification.getUser().getEmail(), subject, body);
                    savedNotification.setEmailSent(true);
                    savedNotification.setEmailSentAt(java.time.LocalDateTime.now());
                    notificationService.saveNotification(savedNotification);
                } catch (Exception e) {
                    // Log error but don't fail the resend
                    System.err.println("Failed to resend email notification: " + e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification resent successfully");
            response.put("notificationId", savedNotification.getNotificationId());
            response.put("emailSent", savedNotification.isEmailSent());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

}
