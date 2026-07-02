package com.foodexpress.order.service;

import com.foodexpress.order.model.Order;
import com.foodexpress.order.model.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;


import jakarta.mail.internet.MimeMessage;

/**
 * Sends Gmail email notifications to customers for order events.
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${notification.from-email}")
    private String fromEmail;

    @Value("${notification.enabled:true}")
    private boolean enabled;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send a notification email when an order is successfully created.
     */
    public void sendOrderCreatedEmail(String toEmail, Order order) {
        if (!enabled || toEmail == null || toEmail.isBlank()) return;

        String subject = "✅ Order Confirmed — #" + order.getId().toString().substring(0, 8).toUpperCase();

        String body = """
                <html>
                <body style="font-family: Arial, sans-serif; background: #f4f4f4; padding: 20px;">
                  <div style="max-width: 600px; margin: auto; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <div style="background: #4CAF50; padding: 24px; text-align: center;">
                      <h1 style="color: white; margin: 0;">🍔 FoodExpress</h1>
                      <p style="color: #e8f5e9; margin: 4px 0 0;">Your order has been placed!</p>
                    </div>
                    <div style="padding: 28px;">
                      <h2 style="color: #333;">Order Received ✅</h2>
                      <p style="color: #555;">Hi there! We've received your order and it's being processed.</p>
                      <table style="width: 100%%; border-collapse: collapse; margin: 20px 0;">
                        <tr style="background: #f9f9f9;">
                          <td style="padding: 10px; border: 1px solid #ddd; font-weight: bold;">Order ID</td>
                          <td style="padding: 10px; border: 1px solid #ddd; font-family: monospace;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding: 10px; border: 1px solid #ddd; font-weight: bold;">Status</td>
                          <td style="padding: 10px; border: 1px solid #ddd;">
                            <span style="background: #4CAF50; color: white; padding: 3px 10px; border-radius: 12px;">%s</span>
                          </td>
                        </tr>
                        <tr style="background: #f9f9f9;">
                          <td style="padding: 10px; border: 1px solid #ddd; font-weight: bold;">Total</td>
                          <td style="padding: 10px; border: 1px solid #ddd; font-weight: bold; color: #4CAF50;">%s %s</td>
                        </tr>
                        <tr>
                          <td style="padding: 10px; border: 1px solid #ddd; font-weight: bold;">Items</td>
                          <td style="padding: 10px; border: 1px solid #ddd;">%d item(s)</td>
                        </tr>
                      </table>
                      <p style="color: #888; font-size: 13px;">We'll notify you as your order progresses. Thank you for choosing FoodExpress!</p>
                    </div>
                    <div style="background: #f4f4f4; padding: 16px; text-align: center;">
                      <p style="color: #aaa; font-size: 12px; margin: 0;">© 2025 FoodExpress. All rights reserved.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                order.getId().toString(),
                order.getStatus().name(),
                order.getTotalPrice() != null ? order.getTotalPrice().toPlainString() : "0.00",
                order.getCurrency() != null ? order.getCurrency() : "USD",
                order.getItems().size()
        );

        sendHtmlEmail(toEmail, subject, body);
    }

    /**
     * Send a notification email when order status changes.
     */
    public void sendOrderStatusChangedEmail(String toEmail, Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        if (!enabled || toEmail == null || toEmail.isBlank()) return;

        String emoji = getStatusEmoji(newStatus);
        String subject = emoji + " Order Update — Status: " + newStatus.name();

        String statusColor = getStatusColor(newStatus);

        String body = """
                <html>
                <body style="font-family: Arial, sans-serif; background: #f4f4f4; padding: 20px;">
                  <div style="max-width: 600px; margin: auto; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                    <div style="background: %s; padding: 24px; text-align: center;">
                      <h1 style="color: white; margin: 0;">🍔 FoodExpress</h1>
                      <p style="color: rgba(255,255,255,0.85); margin: 4px 0 0;">Order status update</p>
                    </div>
                    <div style="padding: 28px;">
                      <h2 style="color: #333;">%s Your order is now <em>%s</em></h2>
                      <table style="width: 100%%; border-collapse: collapse; margin: 20px 0;">
                        <tr style="background: #f9f9f9;">
                          <td style="padding: 10px; border: 1px solid #ddd; font-weight: bold;">Order ID</td>
                          <td style="padding: 10px; border: 1px solid #ddd; font-family: monospace;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding: 10px; border: 1px solid #ddd; font-weight: bold;">Previous Status</td>
                          <td style="padding: 10px; border: 1px solid #ddd;">
                            <span style="background: #9e9e9e; color: white; padding: 3px 10px; border-radius: 12px;">%s</span>
                          </td>
                        </tr>
                        <tr style="background: #f9f9f9;">
                          <td style="padding: 10px; border: 1px solid #ddd; font-weight: bold;">New Status</td>
                          <td style="padding: 10px; border: 1px solid #ddd;">
                            <span style="background: %s; color: white; padding: 3px 10px; border-radius: 12px;">%s</span>
                          </td>
                        </tr>
                      </table>
                      <p style="color: #888; font-size: 13px;">%s</p>
                    </div>
                    <div style="background: #f4f4f4; padding: 16px; text-align: center;">
                      <p style="color: #aaa; font-size: 12px; margin: 0;">© 2025 FoodExpress. All rights reserved.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                statusColor,
                emoji,
                newStatus.name(),
                order.getId().toString(),
                oldStatus.name(),
                statusColor,
                newStatus.name(),
                getStatusMessage(newStatus)
        );

        sendHtmlEmail(toEmail, subject, body);
    }

    // ── Internal helpers ──

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email notification sent to {} — subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String getStatusEmoji(OrderStatus status) {
        return switch (status) {
            case CONFIRMED  -> "✅";
            case PREPARING  -> "👨‍🍳";
            case READY      -> "📦";
            case DELIVERING -> "🚴";
            case DELIVERED  -> "🎉";
            case CANCELLED  -> "❌";
            default         -> "🔔";
        };
    }

    private String getStatusColor(OrderStatus status) {
        return switch (status) {
            case CONFIRMED  -> "#4CAF50";
            case PREPARING  -> "#FF9800";
            case READY      -> "#2196F3";
            case DELIVERING -> "#9C27B0";
            case DELIVERED  -> "#4CAF50";
            case CANCELLED  -> "#F44336";
            default         -> "#607D8B";
        };
    }

    private String getStatusMessage(OrderStatus status) {
        return switch (status) {
            case CONFIRMED  -> "Great news! Your order has been confirmed by the restaurant.";
            case PREPARING  -> "The restaurant is preparing your food. Hang tight!";
            case READY      -> "Your order is packed and ready for pickup!";
            case DELIVERING -> "Your order is on its way! 🛵";
            case DELIVERED  -> "Your order has been delivered. Enjoy your meal! 🎉";
            case CANCELLED  -> "Your order has been cancelled. If this was unexpected, please contact support.";
            default         -> "Your order status has been updated.";
        };
    }
}
