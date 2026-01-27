package uk.co.visad.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import uk.co.visad.dto.EmailInvoiceRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    // Inject History repositories if needed to log

    public void sendInvoiceEmail(EmailInvoiceRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String[] to = request.getEmails().toArray(new String[0]);
            helper.setTo(to);
            helper.setSubject(request.getSubject() != null ? request.getSubject() : "Invoice from VisaD");
            helper.setFrom("info@visaway.co.uk"); // Should match config

            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                helper.setBcc(request.getBcc());
            }

            String htmlBody = buildInvoiceHtml(request);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            // Log history...
            log.info("Invoice email sent to {}", (Object) to);
        } catch (MessagingException e) {
            log.error("Failed to send email", e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    private String buildInvoiceHtml(EmailInvoiceRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif;'>");
        
        sb.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee;'>");
        
        // Header
        sb.append("<div style='text-align: center; margin-bottom: 30px;'>");
        sb.append("<h1 style='color: #333;'>").append(request.getAction().contains("t_invoice") ? "Payment Receipt" : "Invoice").append("</h1>");
        sb.append("<p style='color: #666;'>").append(request.getInvoiceNumber()).append("</p>");
        sb.append("</div>");

        // Info
        sb.append("<div style='margin-bottom: 20px;'>");
        sb.append("<p><strong>Customer:</strong> ").append(request.getCustomerName()).append("</p>");
        sb.append("</div>");

        // Items Table
        sb.append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 20px;'>");
        sb.append("<tr style='background: #f9f9f9;'><th style='padding: 10px; text-align: left; border-bottom: 2px solid #ddd;'>Item</th><th style='padding: 10px; text-align: right; border-bottom: 2px solid #ddd;'>Price</th></tr>");
        
        if (request.getInvoiceItems() != null) {
            for (EmailInvoiceRequest.EmailItemDto item : request.getInvoiceItems()) {
                sb.append("<tr>");
                sb.append("<td style='padding: 10px; border-bottom: 1px solid #eee;'>");
                sb.append("<strong>").append(item.getName()).append("</strong>");
                if (item.getPackage_() != null && !item.getPackage_().isEmpty()) {
                    sb.append(" - ").append(item.getPackage_());
                }
                sb.append("<br><span style='color:#777; font-size:12px;'>");
                if (item.getVisaType() != null) sb.append(item.getVisaType());
                if (item.getCountry() != null) sb.append(" • ").append(item.getCountry());
                sb.append("</span>");
                sb.append("</td>");
                sb.append("<td style='padding: 10px; text-align: right; border-bottom: 1px solid #eee;'>£").append(item.getPrice()).append("</td>");
                sb.append("</tr>");
            }
        }

        sb.append("</table>");

        // Totals
        sb.append("<div style='text-align: right;'>");
        sb.append("<p><strong>Subtotal:</strong> £").append(request.getSubtotal()).append("</p>");
        if (!"0.00".equals(request.getDiscountAmount())) {
            sb.append("<p style='color: green;'><strong>Discount:</strong> -£").append(request.getDiscountAmount()).append("</p>");
        }
        sb.append("<h3 style='border-top: 2px solid #333; display: inline-block; padding-top: 10px;'>Total: £").append(request.getTotal()).append("</h3>");
        sb.append("</div>");

        sb.append("<div style='margin-top: 40px; font-size: 12px; color: #999; text-align: center;'>");
        sb.append("<p>VisaD Group</p>");
        sb.append("</div>");

        sb.append("</div></body></html>");
        return sb.toString();
    }
}
