package com.sliit.vehiclebiddingsystem.service.decorator;

/**
 * Interface for email decorators in the Decorator Pattern
 * This allows for flexible email formatting and styling
 */
public interface EmailDecoratorInterface {
    
    /**
     * Send a plain text email
     * @param to recipient email address
     * @param subject email subject
     * @param body email body content
     */
    void send(String to, String subject, String body);
    
    /**
     * Send an HTML email
     * @param to recipient email address
     * @param subject email subject
     * @param htmlBody HTML email body content
     */
    void sendHtml(String to, String subject, String htmlBody);
    
    /**
     * Send a password reset email with specific formatting
     * @param to recipient email address
     * @param token reset token
     */
    void sendPasswordResetEmail(String to, String token);
}
