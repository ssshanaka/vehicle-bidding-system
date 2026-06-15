package com.sliit.vehiclebiddingsystem.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sliit.vehiclebiddingsystem.entity.NotificationLog;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByAuctionAuctionIdOrderBySentAtDesc(Long auctionId);

    Page<NotificationLog> findByAuctionAuctionIdOrderBySentAtDesc(Long auctionId, Pageable pageable);

    List<NotificationLog> findBySentByUserIdOrderBySentAtDesc(Long userId);

    Page<NotificationLog> findBySentByUserIdOrderBySentAtDesc(Long userId, Pageable pageable);

    List<NotificationLog> findByNotificationTypeOrderBySentAtDesc(NotificationLog.NotificationType notificationType);

    Page<NotificationLog> findByNotificationTypeOrderBySentAtDesc(NotificationLog.NotificationType notificationType, Pageable pageable);

    List<NotificationLog> findByRecipientTypeOrderBySentAtDesc(NotificationLog.RecipientType recipientType);

    Page<NotificationLog> findByRecipientTypeOrderBySentAtDesc(NotificationLog.RecipientType recipientType, Pageable pageable);

    List<NotificationLog> findByStatusOrderBySentAtDesc(NotificationLog.Status status);

    Page<NotificationLog> findByStatusOrderBySentAtDesc(NotificationLog.Status status, Pageable pageable);

    List<NotificationLog> findBySentAtAfterOrderBySentAtDesc(LocalDateTime sentAt);

    Page<NotificationLog> findBySentAtAfterOrderBySentAtDesc(LocalDateTime sentAt, Pageable pageable);

    List<NotificationLog> findBySentAtBetweenOrderBySentAtDesc(LocalDateTime startDate, LocalDateTime endDate);

    Page<NotificationLog> findBySentAtBetweenOrderBySentAtDesc(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("SELECT nl FROM NotificationLog nl WHERE nl.auction.auctionId = :auctionId AND nl.notificationType = :notificationType ORDER BY nl.sentAt DESC")
    List<NotificationLog> findByAuctionIdAndNotificationType(@Param("auctionId") Long auctionId, 
                                                             @Param("notificationType") NotificationLog.NotificationType notificationType);

    @Query("SELECT COUNT(nl) FROM NotificationLog nl WHERE nl.auction.auctionId = :auctionId AND nl.notificationType = :notificationType")
    Long countByAuctionIdAndNotificationType(@Param("auctionId") Long auctionId, 
                                           @Param("notificationType") NotificationLog.NotificationType notificationType);

    @Query("SELECT nl FROM NotificationLog nl WHERE nl.sentBy.userId = :userId AND nl.sentAt >= :startDate ORDER BY nl.sentAt DESC")
    List<NotificationLog> findBySentByAndDateRange(@Param("userId") Long userId, 
                                                   @Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(nl) FROM NotificationLog nl WHERE nl.sentBy.userId = :userId AND nl.sentAt >= :startDate")
    Long countBySentByAndDateRange(@Param("userId") Long userId, 
                                   @Param("startDate") LocalDateTime startDate);

    @Query("SELECT nl FROM NotificationLog nl WHERE nl.status = :status AND nl.sentAt >= :startDate ORDER BY nl.sentAt DESC")
    List<NotificationLog> findFailedNotifications(@Param("status") NotificationLog.Status status, 
                                                  @Param("startDate") LocalDateTime startDate);

    @Query("SELECT DISTINCT nl.auction.auctionId FROM NotificationLog nl WHERE nl.notificationType = :notificationType AND nl.sentAt >= :startDate")
    List<Long> findDistinctAuctionIdsByNotificationType(@Param("notificationType") NotificationLog.NotificationType notificationType, 
                                                       @Param("startDate") LocalDateTime startDate);

    @Query("SELECT nl FROM NotificationLog nl ORDER BY nl.sentAt DESC")
    Page<NotificationLog> findAllOrderBySentAtDesc(Pageable pageable);

    @Query("SELECT nl FROM NotificationLog nl WHERE nl.emailSent = false AND nl.sentAt >= :startDate ORDER BY nl.sentAt DESC")
    List<NotificationLog> findUnsentEmailNotifications(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(nl) FROM NotificationLog nl WHERE nl.sentAt >= :startDate")
    Long countNotificationsSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(nl) FROM NotificationLog nl WHERE nl.emailSent = true AND nl.sentAt >= :startDate")
    Long countEmailNotificationsSince(@Param("startDate") LocalDateTime startDate);
}
