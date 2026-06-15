package com.sliit.vehiclebiddingsystem.service.decorator;

/**
 * Notification Email Decorator that adds specific styling for notification emails
 * This decorator enhances notification emails with appropriate visual indicators
 */
public class NotificationEmailDecorator extends EmailDecorator {
    
    public NotificationEmailDecorator(EmailDecoratorInterface emailService) {
        super(emailService);
    }
    
    /**
     * Sends a notification email with enhanced styling
     */
    @Override
    public void send(String to, String subject, String body) {
        String notificationBody = applyNotificationStyling(body, "info");
        super.send(to, subject, notificationBody);
    }
    
    /**
     * Sends an HTML notification email with enhanced styling
     */
    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        String notificationBody = applyNotificationStyling(htmlBody, "info");
        super.sendHtml(to, subject, notificationBody);
    }
    
    /**
     * Sends a winner notification with special styling
     */
    public void sendWinnerNotification(String to, String subject, String body) {
        String winnerBody = applyNotificationStyling(body, "success");
        super.send(to, subject, winnerBody);
    }
    
    /**
     * Sends an outbid notification with warning styling
     */
    public void sendOutbidNotification(String to, String subject, String body) {
        String outbidBody = applyNotificationStyling(body, "warning");
        super.send(to, subject, outbidBody);
    }
    
    /**
     * Sends an auction closure notification with info styling
     */
    public void sendAuctionClosureNotification(String to, String subject, String body) {
        String closureBody = applyNotificationStyling(body, "info");
        super.send(to, subject, closureBody);
    }
    
    /**
     * Applies notification-specific styling based on notification type
     * @param content the email content
     * @param type the notification type (success, warning, info, error)
     * @return styled notification content
     */
    private String applyNotificationStyling(String content, String type) {
        String icon = getIconForType(type);
        String color = getColorForType(type);
        String backgroundColor = getBackgroundColorForType(type);
        
        return String.format(
            "<div style='background: %s; border-left: 4px solid %s; padding: 20px; margin: 20px 0; border-radius: 4px;'>" +
                "<div style='display: flex; align-items: center; margin-bottom: 15px;'>" +
                    "<span style='font-size: 24px; margin-right: 10px;'>%s</span>" +
                    "<h3 style='margin: 0; color: %s; font-size: 18px;'>Notification</h3>" +
                "</div>" +
                "<div style='color: #333; line-height: 1.6;'>%s</div>" +
            "</div>",
            backgroundColor, color, icon, color, content
        );
    }
    
    /**
     * Gets the appropriate icon for the notification type
     */
    private String getIconForType(String type) {
        switch (type.toLowerCase()) {
            case "success":
                return "🎉"; // Party emoji for winner notifications
            case "warning":
                return "⚠️"; // Warning emoji for outbid notifications
            case "error":
                return "❌"; // Error emoji for error notifications
            case "info":
            default:
                return "ℹ️"; // Info emoji for general notifications
        }
    }
    
    /**
     * Gets the appropriate color for the notification type
     */
    private String getColorForType(String type) {
        switch (type.toLowerCase()) {
            case "success":
                return "#28a745"; // Green for success
            case "warning":
                return "#ffc107"; // Yellow for warning
            case "error":
                return "#dc3545"; // Red for error
            case "info":
            default:
                return "#17a2b8"; // Blue for info
        }
    }
    
    /**
     * Gets the appropriate background color for the notification type
     */
    private String getBackgroundColorForType(String type) {
        switch (type.toLowerCase()) {
            case "success":
                return "#d4edda"; // Light green background
            case "warning":
                return "#fff3cd"; // Light yellow background
            case "error":
                return "#f8d7da"; // Light red background
            case "info":
            default:
                return "#d1ecf1"; // Light blue background
        }
    }
}
