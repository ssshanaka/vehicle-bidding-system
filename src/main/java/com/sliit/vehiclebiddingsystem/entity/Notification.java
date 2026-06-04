package com.sliit.vehiclebiddingsystem.entity;

import java.time.LocalDateTime;

import com.sliit.vehiclebiddingsystem.converter.NotificationTypeConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification")
@Data
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @Column(nullable = false)
    @Convert(converter = NotificationTypeConverter.class)
    private Type type;  // Enum: OUTBID, CLOSURE, WINNER

    private String content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "email_sent", nullable = false)
    private boolean emailSent = false;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    public enum Type {
        OUTBID, CLOSURE, WINNER, AUCTION_STARTED, AUCTION_ENDING, BID_PLACED, CUSTOM
    }
}