package com.sliit.vehiclebiddingsystem.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sliit.vehiclebiddingsystem.entity.Notification;
import com.sliit.vehiclebiddingsystem.entity.User;
import com.sliit.vehiclebiddingsystem.repository.UserRepository;
import com.sliit.vehiclebiddingsystem.service.NotificationService;

@Controller
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/notifications")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('SALES_MANAGER') or hasRole('CUSTOMER_SERVICE') or hasRole('VEHICLE_INSPECTOR')")
    public String getNotifications(Model model, 
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationService.getUserNotificationsPaginated(user.getUserId(), pageable);
        
        long unreadCount = notificationService.getUnreadCount(user.getUserId());

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notifications.getTotalPages());

        return "notifications-inbox";
    }

    @GetMapping("/api/notifications/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);

        long unreadCount = notificationService.getUnreadCount(user.getUserId());

        Map<String, Object> response = new HashMap<>();
        response.put("unreadCount", unreadCount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notifications/{id}/mark-read")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('SALES_MANAGER') or hasRole('CUSTOMER_SERVICE') or hasRole('VEHICLE_INSPECTOR')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);

            Notification notification = notificationService.markAsRead(id, user.getUserId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("notificationId", notification.getNotificationId());
            response.put("isRead", notification.isRead());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/notifications/mark-all-read")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('SALES_MANAGER') or hasRole('CUSTOMER_SERVICE') or hasRole('VEHICLE_INSPECTOR')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);

            notificationService.markAllAsRead(user.getUserId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All notifications marked as read");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/notifications/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('SALES_MANAGER') or hasRole('CUSTOMER_SERVICE') or hasRole('VEHICLE_INSPECTOR')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userRepository.findByUsername(username);

            notificationService.deleteNotification(id, user.getUserId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification deleted");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/notifications/type/{type}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN_OFFICER') or hasRole('IT_CONSULTANT') or hasRole('SALES_MANAGER') or hasRole('CUSTOMER_SERVICE') or hasRole('VEHICLE_INSPECTOR')")
    public String getNotificationsByType(@PathVariable String type, Model model,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username);

        try {
            Notification.Type notificationType = Notification.Type.valueOf(type.toUpperCase());
            Pageable pageable = PageRequest.of(page, size);
            Page<Notification> notifications = notificationService.getUserNotificationsByTypePaginated(user.getUserId(), notificationType, pageable);
            
            model.addAttribute("notifications", notifications);
            model.addAttribute("filterType", type.toUpperCase());
            model.addAttribute("unreadCount", notificationService.getUnreadCount(user.getUserId()));
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", notifications.getTotalPages());

            return "notifications-inbox";
        } catch (IllegalArgumentException e) {
            return "redirect:/notifications";
        }
    }
}
