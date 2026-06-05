package com.sliit.vehiclebiddingsystem.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.sliit.vehiclebiddingsystem.converter.AuctionStatusConverter;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auction")
@Data
@NoArgsConstructor
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auction_id")
    private Long auctionId;

    @OneToOne
    @JoinColumn(name = "listing_id", nullable = false, unique = true)
    private VehicleListing listing;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "current_end_time")
    private LocalDateTime currentEndTime;

    @Column(name = "extension_duration")
    private Integer extensionDuration = 30;

    @Column(nullable = false)
    @Convert(converter = AuctionStatusConverter.class)
    private Status status = Status.PENDING;  // Enum: PENDING, ACTIVE, CLOSED

    @Transient
    private Double highestBid;
    
    @Transient
    private Long bidCount;
    
    @Transient
    private Long bidderCount;

    // Relationships
    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL)
    private List<Bid> bids;

    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL)
    private List<Notification> notifications;

    @OneToOne(mappedBy = "auction", cascade = CascadeType.ALL)
    private Report report;

    public enum Status {
        PENDING, ACTIVE, CLOSED
    }
}