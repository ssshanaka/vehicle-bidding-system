package com.sliit.vehiclebiddingsystem.entity;

import java.time.LocalDateTime;

import com.sliit.vehiclebiddingsystem.converter.AuditLogActionTypeConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_log")
@Data
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = true)
    private User admin;

    @Column(name = "action_type", nullable = false)
    @Convert(converter = AuditLogActionTypeConverter.class)
    private ActionType actionType;  // Enum: APPROVE, REJECT, BAN, WARNING, UNAUTHORIZED

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    private String reason;

    public enum ActionType {
        APPROVE, REJECT, BAN, WARNING, UNAUTHORIZED, START_AUCTION, EXTEND_AUCTION, END_AUCTION, CREATE_AUCTION, FLAG_REPORTING, AUTO_EXTEND_AUCTION, MANUAL_EXTEND_AUCTION, CLOSE_AUCTION
    }
}