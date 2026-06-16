package com.sliit.vehiclebiddingsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sliit.vehiclebiddingsystem.entity.Auction;
import com.sliit.vehiclebiddingsystem.entity.Bid;
import com.sliit.vehiclebiddingsystem.entity.User;

/**
 * Repository for Bid entity.
 */
@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    /**
     * Find the highest bid amount for a given auction.
     */
    @Query("SELECT MAX(b.amount) FROM Bid b WHERE b.auction.auctionId = ?1")
    Double findHighestBidByAuctionId(Long auctionId);

    /**
     * Find the highest bidder for a given auction.
     */
    @Query("SELECT b.bidder FROM Bid b WHERE b.auction.auctionId = ?1 ORDER BY b.amount DESC LIMIT 1")
    User findHighestBidderByAuctionId(Long auctionId);

    /**
     * Get all bids for an auction, sorted by amount descending.
     */
    @Query("SELECT b FROM Bid b WHERE b.auction.auctionId = :auctionId ORDER BY b.amount DESC")
    List<Bid> findByAuctionAuctionIdOrderByAmountDesc(@Param("auctionId") Long auctionId);

    /**
     * Find distinct bidders for a given auction.
     */
    @Query("SELECT DISTINCT b.bidder FROM Bid b WHERE b.auction.auctionId = :auctionId")
    List<User> findDistinctBiddersByAuctionId(@Param("auctionId") Long auctionId);

    /**
     * Count bids between two timestamps.
     */
    long countByTimestampBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    /**
     * Count bids for a specific auction.
     */
    long countByAuctionAuctionId(Long auctionId);
    
    /**
     * Find all bids by a specific user (bidder).
     */
    @Query("SELECT b FROM Bid b WHERE b.bidder.userId = :userId ORDER BY b.timestamp DESC")
    List<Bid> findByBidderUserIdOrderByTimestampDesc(@Param("userId") Long userId);
    
    /**
     * Find all bids by a specific user with pagination.
     */
    @Query("SELECT b FROM Bid b JOIN FETCH b.auction a JOIN FETCH a.listing l JOIN FETCH l.seller WHERE b.bidder.userId = :userId ORDER BY b.timestamp DESC")
    List<Bid> findByBidderUserIdWithAuctionAndListing(@Param("userId") Long userId);
    
    /**
     * Find distinct auctions where a user has placed bids.
     */
    @Query("SELECT DISTINCT b.auction FROM Bid b WHERE b.bidder.userId = :userId ORDER BY b.auction.startTime DESC")
    List<Auction> findDistinctAuctionsByBidderUserId(@Param("userId") Long userId);
    
    /**
     * Find user's highest bid for each auction they participated in.
     */
    @Query("SELECT b FROM Bid b WHERE b.bidder.userId = :userId AND b.amount = " +
           "(SELECT MAX(b2.amount) FROM Bid b2 WHERE b2.auction.auctionId = b.auction.auctionId AND b2.bidder.userId = :userId) " +
           "ORDER BY b.timestamp DESC")
    List<Bid> findHighestBidsByUserForEachAuction(@Param("userId") Long userId);
}