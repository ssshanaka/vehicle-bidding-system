package com.sliit.vehiclebiddingsystem.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sliit.vehiclebiddingsystem.entity.CustomerQuery;

@Repository
public interface CustomerQueryRepository extends JpaRepository<CustomerQuery, Long> {

    List<CustomerQuery> findByUserUserIdOrderByCreatedAtDesc(Long userId);
    
    Page<CustomerQuery> findByUserUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<CustomerQuery> findByStatusOrderByCreatedAtDesc(CustomerQuery.Status status);
    
    Page<CustomerQuery> findByStatusOrderByCreatedAtDesc(CustomerQuery.Status status, Pageable pageable);
    
    List<CustomerQuery> findByPriorityOrderByCreatedAtDesc(CustomerQuery.Priority priority);
    
    Page<CustomerQuery> findByPriorityOrderByCreatedAtDesc(CustomerQuery.Priority priority, Pageable pageable);
    
    List<CustomerQuery> findByQueryTypeOrderByCreatedAtDesc(CustomerQuery.QueryType queryType);
    
    Page<CustomerQuery> findByQueryTypeOrderByCreatedAtDesc(CustomerQuery.QueryType queryType, Pageable pageable);
    
    List<CustomerQuery> findByAssignedToUserIdOrderByCreatedAtDesc(Long userId);
    
    Page<CustomerQuery> findByAssignedToUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    @Query("SELECT cq FROM CustomerQuery cq WHERE cq.status IN :statuses ORDER BY cq.createdAt DESC")
    List<CustomerQuery> findByStatusInOrderByCreatedAtDesc(@Param("statuses") List<CustomerQuery.Status> statuses);
    
    @Query("SELECT cq FROM CustomerQuery cq WHERE cq.status IN :statuses ORDER BY cq.createdAt DESC")
    Page<CustomerQuery> findByStatusInOrderByCreatedAtDesc(@Param("statuses") List<CustomerQuery.Status> statuses, Pageable pageable);
    
    @Query("SELECT cq FROM CustomerQuery cq WHERE cq.createdAt >= :since ORDER BY cq.createdAt DESC")
    List<CustomerQuery> findRecentQueries(@Param("since") LocalDateTime since);
    
    @Query("SELECT cq FROM CustomerQuery cq WHERE cq.createdAt >= :since ORDER BY cq.createdAt DESC")
    Page<CustomerQuery> findRecentQueries(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT COUNT(cq) FROM CustomerQuery cq WHERE cq.status = :status")
    long countByStatus(@Param("status") CustomerQuery.Status status);
    
    @Query("SELECT COUNT(cq) FROM CustomerQuery cq WHERE cq.priority = :priority")
    long countByPriority(@Param("priority") CustomerQuery.Priority priority);
    
    @Query("SELECT COUNT(cq) FROM CustomerQuery cq WHERE cq.createdAt >= :since")
    long countRecentQueries(@Param("since") LocalDateTime since);
    
    @Query("SELECT cq FROM CustomerQuery cq WHERE cq.subject LIKE %:keyword% OR cq.description LIKE %:keyword% ORDER BY cq.createdAt DESC")
    List<CustomerQuery> searchByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT cq FROM CustomerQuery cq WHERE cq.subject LIKE %:keyword% OR cq.description LIKE %:keyword% ORDER BY cq.createdAt DESC")
    Page<CustomerQuery> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT cq FROM CustomerQuery cq WHERE cq.relatedAuctionId = :auctionId ORDER BY cq.createdAt DESC")
    List<CustomerQuery> findByRelatedAuctionId(@Param("auctionId") Long auctionId);
    
    @Query("SELECT cq FROM CustomerQuery cq WHERE cq.relatedListingId = :listingId ORDER BY cq.createdAt DESC")
    List<CustomerQuery> findByRelatedListingId(@Param("listingId") Long listingId);
}

