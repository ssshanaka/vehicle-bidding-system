package com.sliit.vehiclebiddingsystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.sliit.vehiclebiddingsystem.service.decorator.BaseEmailService;
import com.sliit.vehiclebiddingsystem.service.decorator.EmailDecoratorInterface;
import com.sliit.vehiclebiddingsystem.service.decorator.HtmlEmailDecorator;
import com.sliit.vehiclebiddingsystem.service.decorator.NotificationEmailDecorator;

import jakarta.annotation.PostConstruct;

@Service
public class EmailService {

	@Autowired(required = false)
	private JavaMailSender mailSender;

	@Value("${app.base-url:http://localhost:8010}")
	private String appBaseUrl;
	
	// Decorator pattern implementation
	private EmailDecoratorInterface decoratedEmailService;
	
	@Autowired
	public EmailService(JavaMailSender mailSender) {
		this.mailSender = mailSender;
		// Don't initialize decorators here - wait for @PostConstruct
	}
	
	/**
	 * Initialize the decorator chain for enhanced email functionality
	 * Chain: BaseEmailService -> HtmlEmailDecorator -> NotificationEmailDecorator
	 */
	@PostConstruct
	private void initializeDecorators() {
		// Create the base email service with appBaseUrl
		BaseEmailService baseService = new BaseEmailService(mailSender, appBaseUrl);
		
		// Wrap with HTML decorator for consistent styling
		HtmlEmailDecorator htmlDecorator = new HtmlEmailDecorator(baseService);
		
		// Wrap with notification decorator for notification-specific features
		NotificationEmailDecorator notificationDecorator = new NotificationEmailDecorator(htmlDecorator);
		
		// Set the final decorated service
		this.decoratedEmailService = notificationDecorator;
	}

	public void send(String to, String subject, String body) {
		// Use the decorated email service for enhanced functionality
		decoratedEmailService.send(to, subject, body);
	}

	public void sendHtml(String to, String subject, String htmlBody) {
		// Use the decorated email service for enhanced functionality
		decoratedEmailService.sendHtml(to, subject, htmlBody);
	}

	public void sendPasswordResetEmail(String to, String token) {
		// Use the decorated email service for enhanced functionality
		decoratedEmailService.sendPasswordResetEmail(to, token);
	}
	
	/**
	 * Send a winner notification email with special styling
	 * @param to recipient email address
	 * @param subject email subject
	 * @param body email body content
	 */
	public void sendWinnerNotification(String to, String subject, String body) {
		if (decoratedEmailService instanceof NotificationEmailDecorator) {
			((NotificationEmailDecorator) decoratedEmailService).sendWinnerNotification(to, subject, body);
		} else {
			// Fallback to regular send if decorator is not available
			send(to, subject, body);
		}
	}
	
	/**
	 * Send an outbid notification email with warning styling
	 * @param to recipient email address
	 * @param subject email subject
	 * @param body email body content
	 */
	public void sendOutbidNotification(String to, String subject, String body) {
		if (decoratedEmailService instanceof NotificationEmailDecorator) {
			((NotificationEmailDecorator) decoratedEmailService).sendOutbidNotification(to, subject, body);
		} else {
			// Fallback to regular send if decorator is not available
			send(to, subject, body);
		}
	}
	
	/**
	 * Send an auction closure notification email with info styling
	 * @param to recipient email address
	 * @param subject email subject
	 * @param body email body content
	 */
	public void sendAuctionClosureNotification(String to, String subject, String body) {
		if (decoratedEmailService instanceof NotificationEmailDecorator) {
			((NotificationEmailDecorator) decoratedEmailService).sendAuctionClosureNotification(to, subject, body);
		} else {
			// Fallback to regular send if decorator is not available
			send(to, subject, body);
		}
	}
}




