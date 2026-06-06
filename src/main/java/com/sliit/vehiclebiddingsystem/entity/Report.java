package com.sliit.vehiclebiddingsystem.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "report")
@Data
@NoArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @OneToOne
    @JoinColumn(name = "auction_id", nullable = false, unique = true)
    private Auction auction;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    @Column(name = "retention_end")
    private LocalDateTime retentionEnd;

    @Column(name = "bid_summary")
    private String bidSummary;

    @Column(name = "participant_list")
    private String participantList;

    @Column(name = "vehicle_details")
    private String vehicleDetails;

    private String timeline;

    @Column(name = "contact_details")
    private String contactDetails;

    @Column(name = "total_bids")
    private Integer totalBids = 0;

    @Column(name = "highest_bid_amount")
    private Double highestBidAmount = 0.0;

    @Column(name = "winner_username")
    private String winnerUsername;

    @Column(name = "seller_username")
    private String sellerUsername;

    @Column(name = "auction_duration_minutes")
    private Long auctionDurationMinutes;

    @Column(name = "report_status")
    @Enumerated(EnumType.STRING)
    private Status status = Status.GENERATED;

    // Enhanced fields for detailed reporting
    @Column(name = "bid_history", columnDefinition = "TEXT")
    private String bidHistory;

    @Column(name = "vehicle_condition")
    private String vehicleCondition;

    @Column(name = "vehicle_fuel_type")
    private String vehicleFuelType;

    @Column(name = "vehicle_transmission")
    private String vehicleTransmission;

    @Column(name = "vehicle_mileage")
    private Integer vehicleMileage;

    @Column(name = "winner_email")
    private String winnerEmail;

    @Column(name = "winner_phone")
    private String winnerPhone;

    @Column(name = "seller_email")
    private String sellerEmail;

    @Column(name = "seller_phone")
    private String sellerPhone;

    @Column(name = "cse_notes", columnDefinition = "TEXT")
    private String cseNotes;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "is_approved")
    private Boolean isApproved = false;

    public enum Status {
        GENERATED, UNDER_REVIEW, APPROVED, DOWNLOADED, EXPIRED
    }
}