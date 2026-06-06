package com.sliit.vehiclebiddingsystem.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_logs")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_by_user_id", nullable = false)
    private User sentBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false)
    private RecipientType recipientType;

    @Column(name = "custom_message", columnDefinition = "TEXT")
    private String customMessage;

    @Column(name = "recipient_count", nullable = false)
    private Integer recipientCount;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "email_sent", nullable = false)
    private Boolean emailSent = false;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.SENT;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "recipient_details", columnDefinition = "TEXT")
    private String recipientDetails; // JSON string with recipient info

    // Enums
    public enum NotificationType {
        WINNER_CONGRATULATIONS,
        OUTBID_NOTIFICATION,
        AUCTION_CLOSED,
        PAYMENT_REMINDER,
        CONTACT_SELLER,
        CUSTOM_MESSAGE
    }

    public enum RecipientType {
        WINNER,
        SELLER,
        ALL_BIDDERS,
        OUTBID_USERS,
        ALL_PARTICIPANTS,
        SPECIFIC_USERS
    }

    public enum Status {
        SENT,
        FAILED,
        PARTIAL_SUCCESS
    }

    // Constructors
    public NotificationLog() {
        this.sentAt = LocalDateTime.now();
    }

    public NotificationLog(Auction auction, User sentBy, NotificationType notificationType, 
                          RecipientType recipientType, String customMessage, Integer recipientCount) {
        this();
        this.auction = auction;
        this.sentBy = sentBy;
        this.notificationType = notificationType;
        this.recipientType = recipientType;
        this.customMessage = customMessage;
        this.recipientCount = recipientCount;
    }

    // Getters and Setters
    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Auction getAuction() {
        return auction;
    }

    public void setAuction(Auction auction) {
        this.auction = auction;
    }

    public User getSentBy() {
        return sentBy;
    }

    public void setSentBy(User sentBy) {
        this.sentBy = sentBy;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public RecipientType getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(RecipientType recipientType) {
        this.recipientType = recipientType;
    }

    public String getCustomMessage() {
        return customMessage;
    }

    public void setCustomMessage(String customMessage) {
        this.customMessage = customMessage;
    }

    public Integer getRecipientCount() {
        return recipientCount;
    }

    public void setRecipientCount(Integer recipientCount) {
        this.recipientCount = recipientCount;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public Boolean getEmailSent() {
        return emailSent;
    }

    public void setEmailSent(Boolean emailSent) {
        this.emailSent = emailSent;
    }

    public LocalDateTime getEmailSentAt() {
        return emailSentAt;
    }

    public void setEmailSentAt(LocalDateTime emailSentAt) {
        this.emailSentAt = emailSentAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRecipientDetails() {
        return recipientDetails;
    }

    public void setRecipientDetails(String recipientDetails) {
        this.recipientDetails = recipientDetails;
    }

    @Override
    public String toString() {
        return "NotificationLog{" +
                "logId=" + logId +
                ", auctionId=" + (auction != null ? auction.getAuctionId() : null) +
                ", sentBy=" + (sentBy != null ? sentBy.getUsername() : null) +
                ", notificationType=" + notificationType +
                ", recipientType=" + recipientType +
                ", recipientCount=" + recipientCount +
                ", sentAt=" + sentAt +
                ", status=" + status +
                '}';
    }
}
