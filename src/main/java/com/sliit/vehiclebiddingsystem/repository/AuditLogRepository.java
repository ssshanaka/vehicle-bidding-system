package com.sliit.vehiclebiddingsystem.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.sliit.vehiclebiddingsystem.entity.AuditLog;
import com.sliit.vehiclebiddingsystem.entity.AuditLog.ActionType;
import com.sliit.vehiclebiddingsystem.entity.User;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    List<AuditLog> findByActionType(ActionType actionType);
    
    List<AuditLog> findByAdmin(User admin);
    
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate")
    List<AuditLog> findByTimestampBetween(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT a FROM AuditLog a WHERE a.actionType = :actionType AND a.timestamp BETWEEN :startDate AND :endDate")
    List<AuditLog> findByActionTypeAndTimestampBetween(@Param("actionType") ActionType actionType,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT a FROM AuditLog a WHERE a.targetType = :targetType AND a.targetId = :targetId")
    List<AuditLog> findByTargetTypeAndTargetId(@Param("targetType") String targetType, 
                                              @Param("targetId") Long targetId);
    
    // findAll method is inherited from JpaRepository
    
    @Query("SELECT a FROM AuditLog a ORDER BY a.timestamp DESC")
    Page<AuditLog> findAllOrderByTimestampDesc(@NonNull Pageable pageable);
    
    // IT Consultant specific security queries
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.actionType = 'UNAUTHORIZED' AND a.timestamp >= :since")
    long countUnauthorizedAttemptsSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT a FROM AuditLog a WHERE a.actionType = 'UNAUTHORIZED' AND a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<AuditLog> findUnauthorizedAttemptsSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentSecurityEvents(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.timestamp >= :since")
    long countSecurityEventsSince(@Param("since") LocalDateTime since);
}
