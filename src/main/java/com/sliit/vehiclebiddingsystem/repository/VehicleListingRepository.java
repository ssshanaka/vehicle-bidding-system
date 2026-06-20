package com.sliit.vehiclebiddingsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.entity.VehicleListing;
import com.sliit.vehiclebiddingsystem.entity.VehicleListing.Status;

@Repository
public interface VehicleListingRepository extends JpaRepository<VehicleListing, Long> {

    @Query("""
        SELECT v
        FROM VehicleListing v
        LEFT JOIN Auction a
        ON v.listingId = a.listing.listingId
        WHERE v.status = 'Approved'
        AND (a.auctionId IS NULL OR a.status != 'Active')
        """)
    List<VehicleListing> findAvailableForAuction();

    @Query("""
        SELECT v
        FROM VehicleListing v
        LEFT JOIN Auction a
        ON v.listingId = a.listing.listingId
        WHERE v.seller = :seller
        AND v.status = 'Approved'
        AND a.auctionId IS NULL
        """)
    List<VehicleListing> findAvailableForAuctionBySeller(@Param("seller") User seller);
    
    List<VehicleListing> findByStatus(Status status);
    Page<VehicleListing> findByStatus(Status status, Pageable pageable);
    long countByStatus(Status status);
    
    // Efficient filtering method for vehicle inspector dashboard
    @Query("""
        SELECT DISTINCT v FROM VehicleListing v 
        LEFT JOIN FETCH v.images
        LEFT JOIN FETCH v.seller
        WHERE (:status IS NULL OR v.status = :status)
        AND (:search IS NULL OR LOWER(v.make) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(v.model) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:year IS NULL OR v.year = :year)
        ORDER BY v.listingId DESC
        """)
    List<VehicleListing> findVehicleInspectorListings(
        @Param("status") Status status,
        @Param("search") String search, 
        @Param("year") Integer year
    );
    
    // Fetch single listing with images for inspection
    @Query("""
        SELECT v FROM VehicleListing v 
        LEFT JOIN FETCH v.images
        LEFT JOIN FETCH v.seller
        WHERE v.listingId = :id
        """)
    Optional<VehicleListing> findByIdWithImages(@Param("id") Long id);
}