package com.sliit.vehiclebiddingsystem.controller;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sliit.vehiclebiddingsystem.entity.AuditLog;
import com.sliit.vehiclebiddingsystem.entity.AuditLog.ActionType;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.AuditLogRepository;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;

@Controller
public class BanController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuditLogRepository auditLogRepository;

    @GetMapping("/banned/temporary")
    public String temporaryBanPage(Model model) {
        User user = getCurrentUser();
        if (user == null || !user.isBanned() || !"temp".equals(user.getBanType())) {
            return "redirect:/";
        }

        // Get ban reason from audit log
        String banReason = getBanReason(user.getUserId());
        LocalDateTime banStartTime = getBanStartTime(user.getUserId());

        model.addAttribute("user", user);
        model.addAttribute("banReason", banReason);
        model.addAttribute("banStartTime", banStartTime);
        
        return "banned-temporary";
    }

    @GetMapping("/banned/permanent")
    public String permanentBanPage(Model model) {
        User user = getCurrentUser();
        if (user == null || !user.isBanned() || !"perm".equals(user.getBanType())) {
            return "redirect:/";
        }

        // Get ban reason from audit log
        String banReason = getBanReason(user.getUserId());
        LocalDateTime banStartTime = getBanStartTime(user.getUserId());

        model.addAttribute("user", user);
        model.addAttribute("banReason", banReason);
        model.addAttribute("banStartTime", banStartTime);
        
        return "banned-permanent";
    }

    @PostMapping("/appeal")
    public String submitAppeal(@RequestParam String message) {
        User user = getCurrentUser();
        if (user == null || !user.isBanned()) {
            return "redirect:/";
        }

        // Log the appeal
        logAppeal(user, message);
        
        return "redirect:/banned/temporary?appeal=submitted";
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            return userRepository.findByUsername(username);
        }
        return null;
    }

    private String getBanReason(Long userId) {
        Optional<AuditLog> banLog = auditLogRepository.findByTargetTypeAndTargetId("USER", userId)
            .stream()
            .filter(log -> log.getActionType() == ActionType.BAN)
            .findFirst();
        
        return banLog.map(AuditLog::getReason).orElse("Policy violation");
    }

    private LocalDateTime getBanStartTime(Long userId) {
        Optional<AuditLog> banLog = auditLogRepository.findByTargetTypeAndTargetId("USER", userId)
            .stream()
            .filter(log -> log.getActionType() == ActionType.BAN)
            .findFirst();
        
        return banLog.map(AuditLog::getTimestamp).orElse(LocalDateTime.now());
    }

    private void logAppeal(User user, String message) {
        AuditLog appealLog = new AuditLog();
        appealLog.setAdmin(user); // User appealing their own ban
        appealLog.setActionType(ActionType.WARNING); // Using WARNING as appeal type
        appealLog.setTargetId(user.getUserId());
        appealLog.setTargetType("USER_APPEAL");
        appealLog.setReason("Ban appeal: " + message);
        appealLog.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(appealLog);
    }
}
