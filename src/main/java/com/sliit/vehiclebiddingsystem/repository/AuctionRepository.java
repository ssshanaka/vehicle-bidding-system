package com.sliit.vehiclebiddingsystem.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.entity.Auction.Status;
import com.sliit.vehiclebiddingsystem.entity.User;

/**
 * Repository for Auction entities with custom query for active auctions.
 */
@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE'")
    List<Auction> findActiveAuctions();

    List<Auction> findByStatus(Status status);
    Page<Auction> findByStatus(Status status, Pageable pageable);

    @Query("SELECT a FROM Auction a WHERE a.listing.seller = :seller")
    List<Auction> findBySeller(@Param("seller") User seller);
    
    long countByStatus(Status status);
    
    Optional<Auction> findByListingListingId(Long listingId);
    
    List<Auction> findByStatusAndStartTimeBetween(Status status, LocalDateTime start, LocalDateTime end);
    List<Auction> findByStatusAndEndTimeBetween(Status status, LocalDateTime start, LocalDateTime end);
    
    // Get top 3 pending auctions ordered by start time (earliest first)
