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
    @Query("SELECT a FROM Auction a WHERE a.status = :status ORDER BY a.startTime ASC")
    List<Auction> findTop3ByStatusOrderByStartTimeAsc(@Param("status") Status status, Pageable pageable);
    
    // Get top 3 recently closed auctions ordered by end time (latest first)
    @Query("SELECT a FROM Auction a WHERE a.status = :status ORDER BY a.endTime DESC")
    List<Auction> findTop3ByStatusOrderByEndTimeDesc(@Param("status") Status status, Pageable pageable);
    
    // Get top 5 active auctions ordered by end time (earliest first)
    @Query("SELECT a FROM Auction a WHERE a.status = :status ORDER BY a.endTime ASC")
    List<Auction> findTop5ByStatusOrderByEndTimeAsc(@Param("status") Status status, Pageable pageable);
    
    // Enhanced queries for auction listing with joins and filtering
    @Query("SELECT a FROM Auction a JOIN FETCH a.listing l JOIN FETCH l.seller WHERE a.status = :status ORDER BY a.endTime ASC")
    Page<Auction> findByStatusWithListingAndSeller(@Param("status") Status status, Pageable pageable);
    
    @Query("SELECT a FROM Auction a JOIN FETCH a.listing l JOIN FETCH l.seller WHERE a.status = :status AND " +
           "(LOWER(l.make) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.model) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY a.endTime ASC")
    Page<Auction> findByStatusAndSearchWithListingAndSeller(@Param("status") Status status, @Param("search") String search, Pageable pageable);
    
    @Query("SELECT a FROM Auction a JOIN FETCH a.listing l JOIN FETCH l.seller WHERE " +
           "(LOWER(l.make) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.model) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY a.endTime ASC")
    Page<Auction> findBySearchWithListingAndSeller(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT a FROM Auction a JOIN FETCH a.listing l JOIN FETCH l.seller ORDER BY a.endTime ASC")
    Page<Auction> findAllWithListingAndSeller(Pageable pageable);
    
    // Sorting queries
    @Query("SELECT a FROM Auction a JOIN FETCH a.listing l JOIN FETCH l.seller WHERE a.status = :status ORDER BY a.endTime ASC")
    Page<Auction> findByStatusOrderByEndTimeAsc(@Param("status") Status status, Pageable pageable);
    
    @Query("SELECT a FROM Auction a JOIN FETCH a.listing l JOIN FETCH l.seller WHERE a.status = :status ORDER BY a.endTime DESC")
    Page<Auction> findByStatusOrderByEndTimeDesc(@Param("status") Status status, Pageable pageable);
    
    @Query("SELECT a FROM Auction a JOIN FETCH a.listing l JOIN FETCH l.seller WHERE a.status = :status ORDER BY a.startTime DESC")
    Page<Auction> findByStatusOrderByStartTimeDesc(@Param("status") Status status, Pageable pageable);
    
    // Count queries for statistics
    @Query("SELECT COUNT(a) FROM Auction a WHERE a.status = :status")
    long countByStatusWithJoin(@Param("status") Status status);
    
    // Get auctions with bid counts
    @Query("SELECT a, COUNT(b) as bidCount FROM Auction a LEFT JOIN a.bids b WHERE a.status = :status GROUP BY a ORDER BY a.endTime ASC")
    Page<Object[]> findAuctionsWithBidCounts(@Param("status") Status status, Pageable pageable);
}