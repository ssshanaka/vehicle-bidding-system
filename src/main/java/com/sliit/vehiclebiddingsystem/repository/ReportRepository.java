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

import com.sliit.vehiclebiddingsystem.entity.Report;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByAuctionAuctionId(Long auctionId);

    List<Report> findByAuctionListingSellerUserId(Long sellerId);

    @Query("SELECT r FROM Report r WHERE r.retentionEnd < :now AND r.status != 'EXPIRED'")
    List<Report> findByRetentionEndBeforeAndStatusNot(@Param("now") LocalDateTime now, Report.Status status);

    @Query("SELECT r FROM Report r ORDER BY r.generatedAt DESC")
    Page<Report> findAllOrderByGeneratedAtDesc(Pageable pageable);

    @Query("SELECT r FROM Report r WHERE r.auction.listing.seller.userId = :sellerId ORDER BY r.generatedAt DESC")
    Page<Report> findBySellerOrderByGeneratedAtDesc(@Param("sellerId") Long sellerId, Pageable pageable);
}
