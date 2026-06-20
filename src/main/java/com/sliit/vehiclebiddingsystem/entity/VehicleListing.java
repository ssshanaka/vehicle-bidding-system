package com.sliit.vehiclebiddingsystem.entity;

import java.util.List;

import com.sliit.vehiclebiddingsystem.converter.ConditionConverter;
import com.sliit.vehiclebiddingsystem.converter.VehicleListingStatusConverter;

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
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vehicle_listing")
@Data
@NoArgsConstructor
public class VehicleListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "listing_id")
    private Long listingId;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private String make;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer mileage;

    @Column(nullable = false)
    @Convert(converter = ConditionConverter.class)
    private Condition condition;  // Enum: EXCELLENT, GOOD, FAIR, POOR

    @Column(name = "fuel_type")
    private String fuelType;

    @Column(nullable = false)
    private String transmission;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    @Convert(converter = VehicleListingStatusConverter.class)
    private Status status = Status.PENDING;  // Enum: PENDING, APPROVED, REJECTED, CLOSED

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "inspector_notes")
    private String inspectorNotes;

    // Relationships
    @OneToOne(mappedBy = "listing", cascade = CascadeType.ALL)
    private Auction auction;

    @OneToMany(mappedBy = "listing", orphanRemoval = true)
    private List<VehicleImage> images;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL)
    private List<Bid> bids;

    public enum Condition {
        EXCELLENT, GOOD, FAIR, POOR
    }

    public enum Status {
        PENDING, APPROVED, REJECTED, CLOSED
    }
}