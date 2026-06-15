package com.sliit.vehiclebiddingsystem.service.decorator;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.internet.MimeMessage;

/**
 * Base email service implementation that provides core email functionality
 * This serves as the concrete component in the Decorator Pattern
 */
public class BaseEmailService implements EmailDecoratorInterface {

    private final JavaMailSender mailSender;
    private final String appBaseUrl;

    public BaseEmailService(JavaMailSender mailSender, String appBaseUrl) {
        this.mailSender = mailSender;
        this.appBaseUrl = appBaseUrl;
    }

    @Override
    public void send(String to, String subject, String body) {
        // Backward-compatible text fallback
        sendHtml(to, subject, body.replace("\n", "<br/>"));
    }

    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        if (mailSender == null) {
            System.out.println("[MAIL][HTML] To: " + to + ", Subject: " + subject + ", Body: " + htmlBody);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = appBaseUrl + "/reset-password?token=" + token;
        String subject = "Password Reset Request - Vehicle Bidding System";
        String html = ""
                + "<div style=\"font-family:Arial,sans-serif; color:#333;\">"
                + "<h2 style=\"color:#4a4a4a;\">Password Reset Request</h2>"
                + "<p>You requested a password reset for your Vehicle Bidding System account.</p>"
                + "<p>Click the button below to reset your password. This link expires in 2 hours.</p>"
                + "<p style=\"margin:24px 0;\"><a href=\"" + resetUrl + "\" style=\"background:#0d6efd;color:#fff;padding:12px 18px;text-decoration:none;border-radius:6px;\">Reset Password</a></p>"
                + "<p>If the button doesn't work, copy and paste this URL into your browser:</p>"
                + "<p><a href=\"" + resetUrl + "\">" + resetUrl + "</a></p>"
                + "<hr style=\"border:none;border-top:1px solid #eee;margin:24px 0;\"/>"
                + "<p style=\"font-size:12px;color:#777;\">If you didn't request this, you can ignore this email.</p>"
                + "</div>";
        sendHtml(to, subject, html);
    }
}
