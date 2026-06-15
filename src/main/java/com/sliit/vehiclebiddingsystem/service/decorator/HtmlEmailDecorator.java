package com.sliit.vehiclebiddingsystem.service.decorator;

/**
 * HTML Email Decorator that wraps email content in a professional HTML template
 * This decorator adds consistent styling and branding to all emails
 */
public class HtmlEmailDecorator extends EmailDecorator {
    
    public HtmlEmailDecorator(EmailDecoratorInterface emailService) {
        super(emailService);
    }
    
    /**
     * Sends a plain text email wrapped in HTML template
     */
    @Override
    public void send(String to, String subject, String body) {
        String htmlBody = wrapInHtmlTemplate(body);
        super.send(to, subject, htmlBody);
    }
    
    /**
     * Sends an HTML email with enhanced template styling
     */
    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        String enhancedHtmlBody = wrapInHtmlTemplate(htmlBody);
        super.sendHtml(to, subject, enhancedHtmlBody);
    }
    
    /**
     * Wraps email content in a professional HTML template
     * @param content the email content to wrap
     * @return HTML-formatted email content
     */
    private String wrapInHtmlTemplate(String content) {
        return String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>Vehicle Bidding System</title>" +
            "</head>" +
            "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background-color: #ffffff;'>" +
                    "<div style='background-color: #2c3e50; color: white; padding: 20px; text-align: center;'>" +
                        "<h1 style='margin: 0; font-size: 24px;'>Vehicle Bidding System</h1>" +
                        "<p style='margin: 5px 0 0 0; font-size: 14px; opacity: 0.9;'>Lanka Auto Traders (Pvt) Ltd</p>" +
                    "</div>" +
                    "<div style='padding: 30px; color: #333; line-height: 1.6;'>" +
                        "%s" +
                    "</div>" +
                    "<div style='background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;'>" +
                        "<p style='margin: 0; font-size: 12px; color: #6c757d;'>" +
                            "This email was sent from the Vehicle Bidding System. " +
                            "If you have any questions, please contact our support team." +
                        "</p>" +
                    "</div>" +
                "</div>" +
            "</body>" +
            "</html>", 
            content
        );
    }
}
