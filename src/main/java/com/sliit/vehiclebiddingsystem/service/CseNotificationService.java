package com.sliit.vehiclebiddingsystem.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.entity.Bid;
import com.sliit.vehiclebiddingsystem.entity.Notification;
import com.sliit.vehiclebiddingsystem.entity.NotificationLog;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.AuctionRepository;
import com.sliit.vehiclebiddingsystem.repository.NotificationLogRepository;
import com.sliit.vehiclebiddingsystem.repository.NotificationRepository;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;

@Service
public class CseNotificationService {

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Send winner congratulations notification
     */
    public NotificationLog sendWinnerNotification(Long auctionId, Long sentByUserId, String customMessage) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        User sentBy = userRepository.findById(sentByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (auction.getWinner() == null) {
            throw new RuntimeException("No winner found for this auction");
        }

        String message = customMessage != null && !customMessage.trim().isEmpty() ? 
            customMessage : generateWinnerMessage(auction);

        NotificationLog log = new NotificationLog();
        log.setAuction(auction);
        log.setSentBy(sentBy);
        log.setNotificationType(NotificationLog.NotificationType.WINNER_CONGRATULATIONS);
        log.setRecipientType(NotificationLog.RecipientType.WINNER);
        log.setCustomMessage(message);
        log.setRecipientCount(1);

        try {
            // Create notification for winner
            Notification notification = new Notification();
            notification.setUser(auction.getWinner());
            notification.setAuction(auction);
            notification.setType(Notification.Type.WINNER);
            notification.setContent(message);
            notificationRepository.save(notification);

            // Send email
            sendEmailNotification(auction.getWinner(), "Congratulations! You Won the Auction", message, auction);

            // Update notification's emailSent flag
            notification.setEmailSent(true);
            notification.setEmailSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.setEmailSent(true);
            log.setEmailSentAt(LocalDateTime.now());
            log.setStatus(NotificationLog.Status.SENT);
            log.setRecipientDetails(generateRecipientDetails(auction.getWinner()));

        } catch (Exception e) {
            log.setStatus(NotificationLog.Status.FAILED);
            log.setErrorMessage(e.getMessage());
        }

        return notificationLogRepository.save(log);
    }

    /**
     * Send outbid notifications to all users who were outbid
     */
    public NotificationLog sendOutbidNotifications(Long auctionId, Long sentByUserId, String customMessage) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        User sentBy = userRepository.findById(sentByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<User> outbidUsers = getOutbidUsers(auction);
        
        if (outbidUsers.isEmpty()) {
            throw new RuntimeException("No outbid users found for this auction");
        }

        String message = customMessage != null && !customMessage.trim().isEmpty() ? 
            customMessage : generateOutbidMessage(auction);

        NotificationLog log = new NotificationLog();
        log.setAuction(auction);
        log.setSentBy(sentBy);
        log.setNotificationType(NotificationLog.NotificationType.OUTBID_NOTIFICATION);
        log.setRecipientType(NotificationLog.RecipientType.OUTBID_USERS);
        log.setCustomMessage(message);
        log.setRecipientCount(outbidUsers.size());

        int successCount = 0;
        List<String> recipientDetails = new ArrayList<>();

        try {
            for (User user : outbidUsers) {
                try {
                    // Create notification
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setAuction(auction);
                    notification.setType(Notification.Type.OUTBID);
                    notification.setContent(message);
                    notificationRepository.save(notification);

                    // Send email
                    sendEmailNotification(user, "You Were Outbid", message, auction);
                    
                    // Update notification's emailSent flag
                    notification.setEmailSent(true);
                    notification.setEmailSentAt(LocalDateTime.now());
                    notificationRepository.save(notification);
                    
                    successCount++;
                    recipientDetails.add(generateRecipientDetails(user));

                } catch (Exception e) {
                    recipientDetails.add(generateRecipientDetails(user) + " - FAILED: " + e.getMessage());
                }
            }

            if (successCount == outbidUsers.size()) {
                log.setStatus(NotificationLog.Status.SENT);
            } else if (successCount > 0) {
                log.setStatus(NotificationLog.Status.PARTIAL_SUCCESS);
            } else {
                log.setStatus(NotificationLog.Status.FAILED);
            }

            log.setEmailSent(successCount > 0);
            log.setEmailSentAt(LocalDateTime.now());
            log.setRecipientDetails(String.join("; ", recipientDetails));

        } catch (Exception e) {
            log.setStatus(NotificationLog.Status.FAILED);
            log.setErrorMessage(e.getMessage());
        }

        return notificationLogRepository.save(log);
    }

    /**
     * Send auction closed notification to all participants
     */
    public NotificationLog sendAuctionClosedNotification(Long auctionId, Long sentByUserId, String customMessage) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        User sentBy = userRepository.findById(sentByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<User> participants = getAllParticipants(auction);
        
        if (participants.isEmpty()) {
            throw new RuntimeException("No participants found for this auction");
        }

        String message = customMessage != null && !customMessage.trim().isEmpty() ? 
            customMessage : generateAuctionClosedMessage(auction);

        NotificationLog log = new NotificationLog();
        log.setAuction(auction);
        log.setSentBy(sentBy);
        log.setNotificationType(NotificationLog.NotificationType.AUCTION_CLOSED);
        log.setRecipientType(NotificationLog.RecipientType.ALL_PARTICIPANTS);
        log.setCustomMessage(message);
        log.setRecipientCount(participants.size());

        int successCount = 0;
        List<String> recipientDetails = new ArrayList<>();

        try {
            for (User user : participants) {
                try {
                    // Create notification
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setAuction(auction);
                    notification.setType(Notification.Type.CLOSURE);
                    notification.setContent(message);
                    notificationRepository.save(notification);

                    // Send email
                    sendEmailNotification(user, "Auction Closed", message, auction);
                    
                    // Update notification's emailSent flag
                    notification.setEmailSent(true);
                    notification.setEmailSentAt(LocalDateTime.now());
                    notificationRepository.save(notification);
                    
                    successCount++;
                    recipientDetails.add(generateRecipientDetails(user));

                } catch (Exception e) {
                    recipientDetails.add(generateRecipientDetails(user) + " - FAILED: " + e.getMessage());
                }
            }

            if (successCount == participants.size()) {
                log.setStatus(NotificationLog.Status.SENT);
            } else if (successCount > 0) {
                log.setStatus(NotificationLog.Status.PARTIAL_SUCCESS);
            } else {
                log.setStatus(NotificationLog.Status.FAILED);
            }

            log.setEmailSent(successCount > 0);
            log.setEmailSentAt(LocalDateTime.now());
            log.setRecipientDetails(String.join("; ", recipientDetails));

        } catch (Exception e) {
            log.setStatus(NotificationLog.Status.FAILED);
            log.setErrorMessage(e.getMessage());
        }

        return notificationLogRepository.save(log);
    }

    /**
     * Send payment reminder to winner
     */
    public NotificationLog sendPaymentReminder(Long auctionId, Long sentByUserId, String customMessage) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        User sentBy = userRepository.findById(sentByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (auction.getWinner() == null) {
            throw new RuntimeException("No winner found for this auction");
        }

        String message = customMessage != null && !customMessage.trim().isEmpty() ? 
            customMessage : generatePaymentReminderMessage(auction);

        NotificationLog log = new NotificationLog();
        log.setAuction(auction);
        log.setSentBy(sentBy);
        log.setNotificationType(NotificationLog.NotificationType.PAYMENT_REMINDER);
        log.setRecipientType(NotificationLog.RecipientType.WINNER);
        log.setCustomMessage(message);
        log.setRecipientCount(1);

        try {
            // Create notification
            Notification notification = new Notification();
            notification.setUser(auction.getWinner());
            notification.setAuction(auction);
            notification.setType(Notification.Type.WINNER);
            notification.setContent(message);
            notificationRepository.save(notification);

            // Send email
            sendEmailNotification(auction.getWinner(), "Payment Reminder - Auction Won", message, auction);

            // Update notification's emailSent flag
            notification.setEmailSent(true);
            notification.setEmailSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.setEmailSent(true);
            log.setEmailSentAt(LocalDateTime.now());
            log.setStatus(NotificationLog.Status.SENT);
            log.setRecipientDetails(generateRecipientDetails(auction.getWinner()));

        } catch (Exception e) {
            log.setStatus(NotificationLog.Status.FAILED);
            log.setErrorMessage(e.getMessage());
        }

        return notificationLogRepository.save(log);
    }

    /**
     * Send contact seller information to winner
     */
    public NotificationLog sendContactSellerInfo(Long auctionId, Long sentByUserId, String customMessage) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        User sentBy = userRepository.findById(sentByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (auction.getWinner() == null) {
            throw new RuntimeException("No winner found for this auction");
        }

        String message = customMessage != null && !customMessage.trim().isEmpty() ? 
            customMessage : generateContactSellerMessage(auction);

        NotificationLog log = new NotificationLog();
        log.setAuction(auction);
        log.setSentBy(sentBy);
        log.setNotificationType(NotificationLog.NotificationType.CONTACT_SELLER);
        log.setRecipientType(NotificationLog.RecipientType.WINNER);
        log.setCustomMessage(message);
        log.setRecipientCount(1);

        try {
            // Create notification
            Notification notification = new Notification();
            notification.setUser(auction.getWinner());
            notification.setAuction(auction);
            notification.setType(Notification.Type.WINNER);
            notification.setContent(message);
            notificationRepository.save(notification);

            // Send email
            sendEmailNotification(auction.getWinner(), "Contact Seller Information", message, auction);

            // Update notification's emailSent flag
            notification.setEmailSent(true);
            notification.setEmailSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.setEmailSent(true);
            log.setEmailSentAt(LocalDateTime.now());
            log.setStatus(NotificationLog.Status.SENT);
            log.setRecipientDetails(generateRecipientDetails(auction.getWinner()));

        } catch (Exception e) {
            log.setStatus(NotificationLog.Status.FAILED);
            log.setErrorMessage(e.getMessage());
        }

        return notificationLogRepository.save(log);
    }

    /**
     * Send custom notification to specific recipient type
     */
    public NotificationLog sendCustomNotification(Long auctionId, Long sentByUserId, 
                                                NotificationLog.RecipientType recipientType, 
                                                String customMessage) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        User sentBy = userRepository.findById(sentByUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<User> recipients = getRecipientsByType(auction, recipientType);
        
        if (recipients.isEmpty()) {
            throw new RuntimeException("No recipients found for the specified type");
        }

        NotificationLog log = new NotificationLog();
        log.setAuction(auction);
        log.setSentBy(sentBy);
        log.setNotificationType(NotificationLog.NotificationType.CUSTOM_MESSAGE);
        log.setRecipientType(recipientType);
        log.setCustomMessage(customMessage);
        log.setRecipientCount(recipients.size());

        int successCount = 0;
        List<String> recipientDetails = new ArrayList<>();

        try {
            for (User user : recipients) {
                try {
                    // Create notification
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setAuction(auction);
                    notification.setType(Notification.Type.CUSTOM);
                    notification.setContent(customMessage);
                    notificationRepository.save(notification);

                    // Send email
                    sendEmailNotification(user, "Custom Message from Customer Service", customMessage, auction);
                    
                    // Update notification's emailSent flag
                    notification.setEmailSent(true);
                    notification.setEmailSentAt(LocalDateTime.now());
                    notificationRepository.save(notification);
                    
                    successCount++;
                    recipientDetails.add(generateRecipientDetails(user));

                } catch (Exception e) {
                    recipientDetails.add(generateRecipientDetails(user) + " - FAILED: " + e.getMessage());
                }
            }

            if (successCount == recipients.size()) {
                log.setStatus(NotificationLog.Status.SENT);
            } else if (successCount > 0) {
                log.setStatus(NotificationLog.Status.PARTIAL_SUCCESS);
            } else {
                log.setStatus(NotificationLog.Status.FAILED);
            }

            log.setEmailSent(successCount > 0);
            log.setEmailSentAt(LocalDateTime.now());
            log.setRecipientDetails(String.join("; ", recipientDetails));

        } catch (Exception e) {
            log.setStatus(NotificationLog.Status.FAILED);
            log.setErrorMessage(e.getMessage());
        }

        return notificationLogRepository.save(log);
    }

    /**
     * Get notification logs for an auction
     */
    public List<NotificationLog> getNotificationLogsForAuction(Long auctionId) {
        return notificationLogRepository.findByAuctionAuctionIdOrderBySentAtDesc(auctionId);
    }

    /**
     * Get notification logs sent by a user
     */
    public List<NotificationLog> getNotificationLogsByUser(Long userId) {
        return notificationLogRepository.findBySentByUserIdOrderBySentAtDesc(userId);
    }

    /**
     * Get all notification logs with pagination
     */
    public List<NotificationLog> getAllNotificationLogs() {
        return notificationLogRepository.findAll();
    }

    // Helper methods
    private List<User> getOutbidUsers(Auction auction) {
        if (auction.getBids() == null || auction.getBids().isEmpty()) {
            return new ArrayList<>();
        }

        return auction.getBids().stream()
                .map(Bid::getBidder)
                .filter(bidder -> !bidder.equals(auction.getWinner()))
                .distinct()
                .collect(Collectors.toList());
    }

    private List<User> getAllParticipants(Auction auction) {
        List<User> participants = new ArrayList<>();
        
        // Add seller
        participants.add(auction.getListing().getSeller());
        
        // Add winner if exists
        if (auction.getWinner() != null) {
            participants.add(auction.getWinner());
        }
        
        // Add all bidders
        if (auction.getBids() != null) {
            participants.addAll(auction.getBids().stream()
                    .map(Bid::getBidder)
                    .distinct()
                    .collect(Collectors.toList()));
        }
        
        return participants.stream().distinct().collect(Collectors.toList());
    }

    private List<User> getRecipientsByType(Auction auction, NotificationLog.RecipientType recipientType) {
        switch (recipientType) {
            case WINNER:
                return auction.getWinner() != null ? List.of(auction.getWinner()) : new ArrayList<>();
            case SELLER:
                return List.of(auction.getListing().getSeller());
            case ALL_BIDDERS:
                return auction.getBids() != null ? 
                    auction.getBids().stream()
                        .map(Bid::getBidder)
                        .distinct()
                        .collect(Collectors.toList()) : new ArrayList<>();
            case OUTBID_USERS:
                return getOutbidUsers(auction);
            case ALL_PARTICIPANTS:
                return getAllParticipants(auction);
            case SPECIFIC_USERS:
                // For specific users, return all participants as a fallback
                // In a more advanced implementation, this could accept specific user IDs
                // For now, this is treated as ALL_PARTICIPANTS
                return getAllParticipants(auction);
            default:
                return new ArrayList<>();
        }
    }

    private void sendEmailNotification(User user, String subject, String message, Auction auction) {
        String emailBody = message + 
            "\n\nAuction Details:" +
            "\nVehicle: " + auction.getListing().getMake() + " " + auction.getListing().getModel() +
            "\nAuction ID: " + auction.getAuctionId() +
            "\nFinal Bid: $" + (auction.getHighestBid() != null ? auction.getHighestBid() : "0.00") +
            "\n\nSent at: " + LocalDateTime.now();
        
        // Use appropriate decorator method based on subject/content
        if (subject.contains("Congratulations") || subject.contains("Won")) {
            emailService.sendWinnerNotification(user.getEmail(), subject, emailBody);
        } else if (subject.contains("Outbid")) {
            emailService.sendOutbidNotification(user.getEmail(), subject, emailBody);
        } else if (subject.contains("Auction Closed") || subject.contains("Closure")) {
            emailService.sendAuctionClosureNotification(user.getEmail(), subject, emailBody);
        } else {
            // Use regular send for other notification types
            emailService.send(user.getEmail(), subject, emailBody);
        }
    }

    private String generateRecipientDetails(User user) {
        return user.getUsername() + " (" + user.getEmail() + ")";
    }

    private String generateWinnerMessage(Auction auction) {
        return String.format(
            "Congratulations! You won the auction for %s %s (%d) with a bid of $%.2f.\n\n" +
            "Next Steps:\n" +
            "1. Contact the seller to arrange payment and vehicle pickup\n" +
            "2. Complete the transaction within 7 days\n" +
            "3. Contact customer service if you need assistance\n\n" +
            "Thank you for using our vehicle bidding system!",
            auction.getListing().getMake(),
            auction.getListing().getModel(),
            auction.getListing().getYear(),
            auction.getHighestBid()
        );
    }

    private String generateOutbidMessage(Auction auction) {
        return String.format(
            "You were outbid on %s %s (%d).\n\n" +
            "Final winning bid: $%.2f\n" +
            "Better luck next time!\n\n" +
            "Keep an eye on our upcoming auctions.",
            auction.getListing().getMake(),
            auction.getListing().getModel(),
            auction.getListing().getYear(),
            auction.getHighestBid()
        );
    }

    private String generateAuctionClosedMessage(Auction auction) {
        return String.format(
            "The auction for %s %s (%d) has closed.\n\n" +
            "Final bid: $%.2f\n" +
            "Winner: %s\n\n" +
            "Thank you for participating!",
            auction.getListing().getMake(),
            auction.getListing().getModel(),
            auction.getListing().getYear(),
            auction.getHighestBid(),
            auction.getWinner() != null ? auction.getWinner().getUsername() : "No winner"
        );
    }

    private String generatePaymentReminderMessage(Auction auction) {
        return String.format(
            "Payment Reminder for Auction #%d\n\n" +
            "Vehicle: %s %s (%d)\n" +
            "Winning Bid: $%.2f\n\n" +
            "Please complete your payment within 7 days to secure your purchase.\n" +
            "Contact the seller to arrange payment and vehicle pickup.\n\n" +
            "If you have any questions, please contact customer service.",
            auction.getAuctionId(),
            auction.getListing().getMake(),
            auction.getListing().getModel(),
            auction.getListing().getYear(),
            auction.getHighestBid()
        );
    }

    private String generateContactSellerMessage(Auction auction) {
        User seller = auction.getListing().getSeller();
        return String.format(
            "Contact Information for Auction #%d\n\n" +
            "Vehicle: %s %s (%d)\n" +
            "Your Winning Bid: $%.2f\n\n" +
            "Seller Contact Details:\n" +
            "Name: %s\n" +
            "Email: %s\n" +
            "Phone: %s\n\n" +
            "Please contact the seller to arrange payment and vehicle pickup.\n" +
            "Complete the transaction within 7 days.",
            auction.getAuctionId(),
            auction.getListing().getMake(),
            auction.getListing().getModel(),
            auction.getListing().getYear(),
            auction.getHighestBid(),
            seller.getUsername(),
            seller.getEmail(),
            seller.getPhone()
        );
    }
}
