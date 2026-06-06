package com.sliit.vehiclebiddingsystem.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sliit.vehiclebiddingsystem.entity.AuditLog;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.AuditLogRepository;

@Service
@Transactional
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Log an admin action
     */
    public void logAction(User admin, String actionType, Long targetId, String targetType, String reason) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAdmin(admin);
        auditLog.setActionType(AuditLog.ActionType.valueOf(actionType));
        auditLog.setTargetId(targetId);
        auditLog.setTargetType(targetType);
        auditLog.setReason(reason);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }

    /**
     * Log a system action (no admin user)
     */
    public void logSystemAction(String actionType, Long targetId, String targetType, String reason) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAdmin(null); // System action
        auditLog.setActionType(AuditLog.ActionType.valueOf(actionType));
        auditLog.setTargetId(targetId);
        auditLog.setTargetType(targetType);
        auditLog.setReason(reason);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }
}
