package com.sliit.vehiclebiddingsystem.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sliit.vehiclebiddingsystem.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserUserIdOrderBySentAtDesc(Long userId);

    Page<Notification> findByUserUserIdOrderBySentAtDesc(Long userId, Pageable pageable);

    List<Notification> findByUserUserIdAndReadFalseOrderBySentAtDesc(Long userId);

    long countByUserUserIdAndReadFalse(Long userId);

    List<Notification> findByUserUserIdAndTypeOrderBySentAtDesc(Long userId, Notification.Type type);
    Page<Notification> findByUserUserIdAndTypeOrderBySentAtDesc(Long userId, Notification.Type type, Pageable pageable);

    @Query("SELECT DISTINCT n.user FROM Notification n WHERE n.auction.auctionId = :auctionId")
    List<com.sliit.vehiclebiddingsystem.entity.User> findDistinctUsersByAuctionId(@Param("auctionId") Long auctionId);

    List<Notification> findBySentAtAfterOrderBySentAtDesc(java.time.LocalDateTime sentAt);

    Page<Notification> findAllByOrderBySentAtDesc(Pageable pageable);
}
