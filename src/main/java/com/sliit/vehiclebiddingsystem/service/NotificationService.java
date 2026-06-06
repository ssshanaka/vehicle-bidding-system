package com.sliit.vehiclebiddingsystem.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.entity.Notification;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.AuctionRepository;
import com.sliit.vehiclebiddingsystem.repository.BidRepository;
import com.sliit.vehiclebiddingsystem.repository.NotificationRepository;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private EmailService emailService;

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserUserIdOrderBySentAtDesc(userId);
    }

    public Page<Notification> getUserNotificationsPaginated(Long userId, Pageable pageable) {
        return notificationRepository.findByUserUserIdOrderBySentAtDesc(userId, pageable);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserUserIdAndReadFalseOrderBySentAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserUserIdAndReadFalse(userId);
    }

    public Notification markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to notification");
        }

        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = getUnreadNotifications(userId);
        LocalDateTime now = LocalDateTime.now();
        
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
            notification.setReadAt(now);
        }
        
        notificationRepository.saveAll(unreadNotifications);
    }

    public void createOutbidNotification(Long auctionId, Long outbidUserId, Double newHighestBid) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        User user = userRepository.findById(outbidUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String content = String.format("You have been outbid on %s %s. New highest bid: $%.2f",
                auction.getListing().getMake(),
                auction.getListing().getModel(),
                newHighestBid);

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setAuction(auction);
        notification.setType(Notification.Type.OUTBID);
        notification.setContent(content);

        Notification saved = notificationRepository.save(notification);
        sendEmailNotification(saved);
    }

    public void createAuctionClosureNotification(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        // Get the highest bid amount from the database
        Double highestBidAmount = bidRepository.findHighestBidByAuctionId(auctionId);
        if (highestBidAmount == null) {
            highestBidAmount = 0.0;
        }

        // Notify all bidders
        List<User> bidders = bidRepository.findDistinctBiddersByAuctionId(auctionId);
        
        for (User bidder : bidders) {
            String content = String.format("Auction for %s %s has ended. Final bid: $%.2f",
                    auction.getListing().getMake(),
                    auction.getListing().getModel(),
                    highestBidAmount);

            Notification notification = new Notification();
            notification.setUser(bidder);
            notification.setAuction(auction);
            notification.setType(Notification.Type.CLOSURE);
            notification.setContent(content);

            Notification saved = notificationRepository.save(notification);
            sendEmailNotification(saved);
        }

        // Notify seller
        User seller = auction.getListing().getSeller();
        String sellerContent = String.format("Your auction for %s %s has ended. Winner: %s, Final bid: $%.2f",
                auction.getListing().getMake(),
                auction.getListing().getModel(),
                auction.getWinner() != null ? auction.getWinner().getUsername() : "No winner",
                highestBidAmount);

        Notification sellerNotification = new Notification();
        sellerNotification.setUser(seller);
        sellerNotification.setAuction(auction);
        sellerNotification.setType(Notification.Type.CLOSURE);
        sellerNotification.setContent(sellerContent);

        Notification savedSellerNotification = notificationRepository.save(sellerNotification);
        sendEmailNotification(savedSellerNotification);
    }

    public void createWinnerNotification(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        if (auction.getWinner() != null) {
            // Get the highest bid amount from the database
            Double highestBidAmount = bidRepository.findHighestBidByAuctionId(auctionId);
            if (highestBidAmount == null) {
                highestBidAmount = 0.0;
            }
                    
            String content = String.format("Congratulations! You won the auction for %s %s with a bid of $%.2f",
                    auction.getListing().getMake(),
                    auction.getListing().getModel(),
                    highestBidAmount);

            Notification notification = new Notification();
            notification.setUser(auction.getWinner());
            notification.setAuction(auction);
            notification.setType(Notification.Type.WINNER);
            notification.setContent(content);

            Notification saved = notificationRepository.save(notification);
            sendEmailNotification(saved);
        }
    }

    public void createAuctionStartedNotification(Long auctionId) {
        // Get the highest bid amount from the database
        Double highestBidAmount = bidRepository.findHighestBidByAuctionId(auctionId);
        if (highestBidAmount == null) {
            highestBidAmount = 0.0;
        }

        // For now, we'll create a general notification
        // In a real system, you might want to notify users based on their interests
        // This method can be enhanced to notify interested users
        // TODO: Implement auction started notifications when user preferences are available
    }

    public void createBidPlacedNotification(Long auctionId, Long bidderId, Double bidAmount) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        User bidder = userRepository.findById(bidderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String content = String.format("You placed a bid of $%.2f on %s %s",
                bidAmount,
                auction.getListing().getMake(),
                auction.getListing().getModel());

        Notification notification = new Notification();
        notification.setUser(bidder);
        notification.setAuction(auction);
        notification.setType(Notification.Type.BID_PLACED);
        notification.setContent(content);

        notificationRepository.save(notification);
    }

    private void sendEmailNotification(Notification notification) {
        try {
            String subject = "Vehicle Bidding System - " + notification.getType().name();
            String body = notification.getContent() + 
                         "\n\nAuction ID: " + notification.getAuction().getAuctionId() +
                         "\nSent at: " + notification.getSentAt();

            // Use appropriate decorator method based on notification type
            switch (notification.getType()) {
                case WINNER:
                    emailService.sendWinnerNotification(notification.getUser().getEmail(), subject, body);
                    break;
                case OUTBID:
                    emailService.sendOutbidNotification(notification.getUser().getEmail(), subject, body);
                    break;
                case CLOSURE:
                    emailService.sendAuctionClosureNotification(notification.getUser().getEmail(), subject, body);
                    break;
                default:
                    // Use regular send for other notification types
                    emailService.send(notification.getUser().getEmail(), subject, body);
                    break;
            }
            
            notification.setEmailSent(true);
            notification.setEmailSentAt(LocalDateTime.now());
            notificationRepository.save(notification);
        } catch (Exception e) {
            // Log error but don't fail the notification creation
            System.err.println("Failed to send email notification: " + e.getMessage());
            // Don't set emailSent to true if sending failed
        }
    }

    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to notification");
        }

        notificationRepository.delete(notification);
    }

    public List<Notification> getNotificationsByType(Long userId, Notification.Type type) {
        return notificationRepository.findByUserUserIdAndTypeOrderBySentAtDesc(userId, type);
    }

    public Page<Notification> getUserNotificationsByTypePaginated(Long userId, Notification.Type type, Pageable pageable) {
        return notificationRepository.findByUserUserIdAndTypeOrderBySentAtDesc(userId, type, pageable);
    }

    public Page<Notification> getAllNotificationsPaginated(Pageable pageable) {
        return notificationRepository.findAllByOrderBySentAtDesc(pageable);
    }

    public Notification getNotificationById(Long notificationId) {
        return notificationRepository.findById(notificationId).orElse(null);
    }

    public Notification saveNotification(Notification notification) {
        return notificationRepository.save(notification);
    }
}
