package com.sliit.vehiclebiddingsystem.service.decorator;

/**
 * Abstract decorator class that implements the Decorator Pattern
 * This provides a base for all email decorators to extend
 */
public abstract class EmailDecorator implements EmailDecoratorInterface {
    
    protected EmailDecoratorInterface emailService;
    
    /**
     * Constructor for email decorators
     * @param emailService the email service to decorate
     */
    public EmailDecorator(EmailDecoratorInterface emailService) {
        this.emailService = emailService;
    }
    
    /**
     * Default implementation delegates to the wrapped email service
     * Subclasses can override this to add their own behavior
     */
    @Override
    public void send(String to, String subject, String body) {
        emailService.send(to, subject, body);
    }
    
    /**
     * Default implementation delegates to the wrapped email service
     * Subclasses can override this to add their own behavior
     */
    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        emailService.sendHtml(to, subject, htmlBody);
    }
    
    /**
     * Default implementation delegates to the wrapped email service
     * Subclasses can override this to add their own behavior
     */
    @Override
    public void sendPasswordResetEmail(String to, String token) {
        emailService.sendPasswordResetEmail(to, token);
    }
}
