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

    public void sendInvoiceEmail(EmailInvoiceRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String[] to = request.getEmails().toArray(new String[0]);
            helper.setTo(to);
            helper.setSubject(request.getSubject() != null ? request.getSubject() : "Invoice from VisaD");
            helper.setFrom("info@visad.co.uk");

            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                helper.setBcc(request.getBcc());
            }

            String htmlBody = buildInvoiceHtml(request);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Invoice email sent to {}", (Object) to);
        } catch (MessagingException e) {
            log.error("Failed to send email", e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    private String buildInvoiceHtml(EmailInvoiceRequest request) {
        boolean isTInvoice = request.getAction() != null && request.getAction().contains("t_invoice");
        String invoiceContent = buildInvoiceContent(request, isTInvoice);
        
        // Build the complete email wrapper (matching PHP format exactly)
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("</head>\n");
        html.append("<body style=\"margin:0; padding:0; background-color:#f5f5f5; font-family:Arial,sans-serif;\">\n");
        html.append("    \n");
        html.append("    <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#f5f5f5;\">\n");
        html.append("        <tr>\n");
        html.append("            <td align=\"center\" style=\"padding:40px 20px;\">\n");
        html.append("                \n");
        html.append("                <!-- Email Container -->\n");
        html.append("                <table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#ffffff; border-radius:12px; box-shadow:0 2px 8px rgba(0,0,0,0.05);\">\n");
        html.append("                    \n");
        html.append("                    <!-- Header with Logo -->\n");
        html.append("                    <tr>\n");
        html.append("                        <td style=\"padding:35px 40px 25px 40px; text-align:center; border-bottom:1px solid #eee;\">\n");
        html.append("                            <span style=\"font-size:32px; font-weight:800; letter-spacing:1px; color:#1e3a5f;\">VISA</span><span style=\"font-size:32px; font-weight:800; letter-spacing:1px; color:#20c997;\">D</span>\n");
        html.append("                            <p style=\"margin:8px 0 0 0; font-size:12px; color:#888888;\">iWeBron Limited, 7 Bell Yard, London WC2A 2JR</p>\n");
        html.append("                        </td>\n");
        html.append("                    </tr>\n");
        html.append("                    \n");
        
        // Badge section
        if (isTInvoice) {
            html.append("                    <!-- Success Badge -->\n");
            html.append("                    <tr>\n");
            html.append("                        <td style=\"padding:30px 40px 20px 40px; text-align:center;\">\n");
            html.append("                            <div style=\"display:inline-block; background-color:#d4edda; border:1px solid #c3e6cb; border-radius:50px; padding:12px 30px;\">\n");
            html.append("                                <span style=\"font-size:16px; font-weight:600; color:#155724;\">✓ Payment Confirmed</span>\n");
            html.append("                            </div>\n");
            html.append("                        </td>\n");
            html.append("                    </tr>\n");
        } else {
            html.append("                    <!-- Invoice Badge -->\n");
            html.append("                    <tr>\n");
            html.append("                        <td style=\"padding:30px 40px 20px 40px; text-align:center;\">\n");
            html.append("                            <div style=\"display:inline-block; background-color:#e7f3ff; border:1px solid #b8daff; border-radius:50px; padding:12px 30px;\">\n");
            html.append("                                <span style=\"font-size:16px; font-weight:600; color:#004085;\">📄 Invoice</span>\n");
            html.append("                            </div>\n");
            html.append("                        </td>\n");
            html.append("                    </tr>\n");
        }
        
        html.append("                    \n");
        html.append("                    <!-- Greeting -->\n");
        html.append("                    <tr>\n");
        html.append("                        <td style=\"padding:10px 40px 25px 40px;\">\n");
        html.append("                            <p style=\"margin:0 0 12px 0; font-size:18px; color:#1e3a5f; font-weight:600;\">\n");
        html.append("                                Dear ").append(escapeHtml(request.getCustomerName())).append(",\n");
        html.append("                            </p>\n");
        html.append("                            <p style=\"margin:0; font-size:15px; color:#555555; line-height:1.7;\">\n");
        if (isTInvoice) {
            html.append("                                Thank you for choosing VISAD. Your payment has been successfully received and processed.\n");
        } else {
            html.append("                                Thank you for choosing VISAD. Please find your invoice details below.\n");
        }
        html.append("                            </p>\n");
        html.append("                        </td>\n");
        html.append("                    </tr>\n");
        html.append("                    \n");
        html.append("                    <!-- Invoice Content -->\n");
        html.append("                    <tr>\n");
        html.append("                        <td style=\"padding:0 40px 35px 40px;\">\n");
        html.append("                            ").append(invoiceContent).append("\n");
        html.append("                        </td>\n");
        html.append("                    </tr>\n");
        html.append("                    \n");
        html.append("                    <!-- Footer -->\n");
        html.append("                    <tr>\n");
        html.append("                        <td style=\"padding:25px 40px; background-color:#f8f9fa; border-top:1px solid #eee; text-align:center; border-radius:0 0 12px 12px;\">\n");
        html.append("                            <p style=\"margin:0 0 8px 0; font-size:13px; color:#666666;\">Need help? Contact us at <a href=\"mailto:support@visad.co.uk\" style=\"color:#20c997; text-decoration:none;\">support@visad.co.uk</a></p>\n");
        html.append("                            <p style=\"margin:0; font-size:12px; color:#999999;\"><a href=\"https://www.visad.co.uk\" style=\"color:#1e3a5f; text-decoration:none; font-weight:600;\">www.visad.co.uk</a></p>\n");
        html.append("                        </td>\n");
        html.append("                    </tr>\n");
        html.append("                    \n");
        html.append("                </table>\n");
        html.append("                \n");
        html.append("            </td>\n");
        html.append("        </tr>\n");
        html.append("    </table>\n");
        html.append("    \n");
        html.append("</body>\n");
        html.append("</html>");
        
        return html.toString();
    }

    private String buildInvoiceContent(EmailInvoiceRequest request, boolean isTInvoice) {
        StringBuilder content = new StringBuilder();
        
        // Invoice Details Row
        content.append("<!-- Invoice Details Row -->\n");
        content.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%; margin-bottom:20px; font-family:Arial,sans-serif; border:1px solid #e9ecef; border-radius:8px;\">\n");
        content.append("<tr>\n");
        content.append("<td style=\"width:20%; padding:14px 15px; border-right:1px solid #e9ecef;\">\n");
        content.append("<div style=\"font-size:10px; color:#888888; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:4px;\">Invoice No.</div>\n");
        content.append("<div style=\"font-size:14px; color:#1e3a5f; font-weight:700;\">").append(escapeHtml(request.getInvoiceNumber())).append("</div>\n");
        content.append("</td>\n");
        content.append("<td style=\"width:20%; padding:14px 15px; border-right:1px solid #e9ecef;\">\n");
        content.append("<div style=\"font-size:10px; color:#888888; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:4px;\">Invoice Date</div>\n");
        content.append("<div style=\"font-size:13px; color:#333333; font-weight:500;\">").append(getCurrentDate()).append("</div>\n");
        content.append("</td>\n");
        content.append("<td style=\"width:20%; padding:14px 15px; border-right:1px solid #e9ecef;\">\n");
        content.append("<div style=\"font-size:10px; color:#888888; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:4px;\">").append(isTInvoice ? "Payment Date" : "Due Date").append("</div>\n");
        content.append("<div style=\"font-size:13px; color:#333333; font-weight:500;\">").append(isTInvoice ? getCurrentDate() : getDueDate()).append("</div>\n");
        content.append("</td>\n");
        content.append("<td style=\"width:20%; padding:14px 15px; border-right:1px solid #e9ecef;\">\n");
        content.append("<div style=\"font-size:10px; color:#888888; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:4px;\">").append(isTInvoice ? "Amount Paid" : "Amount Due").append("</div>\n");
        content.append("<div style=\"font-size:16px; color:#1e3a5f; font-weight:700;\">£").append(request.getTotal()).append("</div>\n");
        content.append("</td>\n");
        content.append("<td style=\"width:20%; padding:14px 15px; text-align:center;\">\n");
        content.append("<div style=\"font-size:10px; color:#888888; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:4px;\">Status</div>\n");
        if (isTInvoice) {
            content.append("<div style=\"display:inline-block; padding:5px 12px; background-color:#d4edda; color:#155724; font-size:11px; font-weight:600; border-radius:20px;\">✓ PAID</div>\n");
        } else {
            content.append("<div style=\"display:inline-block; padding:5px 12px; background-color:#fff3cd; color:#856404; font-size:11px; font-weight:600; border-radius:20px;\">PENDING</div>\n");
        }
        content.append("</td>\n");
        content.append("</tr>\n");
        content.append("</table>\n\n");
        
        // Bill To Section
        content.append("<!-- Bill To Section -->\n");
        content.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%; margin-bottom:20px; font-family:Arial,sans-serif;\">\n");
        content.append("<tr>\n");
        content.append("<td style=\"padding:16px 18px; background-color:#f8f9fa; border-radius:8px; border-left:3px solid ").append(isTInvoice ? "#20c997" : "#1e3a5f").append(";\">\n");
        content.append("<div style=\"font-size:10px; color:#888888; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:8px;\">Bill To</div>\n");
        content.append("<div style=\"font-size:15px; color:#1e3a5f; font-weight:600; margin-bottom:5px;\">").append(escapeHtml(request.getCustomerName())).append("</div>\n");
        content.append("<div style=\"font-size:13px; color:#555555; line-height:1.6;\">").append(escapeHtml(request.getCustomerAddress())).append("</div>\n");
        content.append("<div style=\"margin-top:8px;\">\n");
        content.append("<a href=\"mailto:").append(escapeHtml(request.getCustomerEmail())).append("\" style=\"color:#20c997; text-decoration:none; font-size:13px;\">").append(escapeHtml(request.getCustomerEmail())).append("</a>\n");
        content.append("</div>\n");
        content.append("</td>\n");
        content.append("</tr>\n");
        content.append("</table>\n\n");
        
        // Items Table
        if (request.getInvoiceItems() != null && !request.getInvoiceItems().isEmpty()) {
            content.append("<!-- Items Table -->\n");
            content.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%; margin-bottom:20px; font-family:Arial,sans-serif; border:1px solid #e5e5e5; border-radius:8px; border-collapse:separate;\">\n");
            content.append("<tr>\n");
            content.append("<th style=\"padding:12px 15px; text-align:left; font-weight:600; color:#333333; font-size:11px; text-transform:uppercase; letter-spacing:0.5px; background-color:#f9f9f9; border-bottom:1px solid #e5e5e5;\">Description</th>\n");
            content.append("<th style=\"padding:12px 15px; text-align:center; font-weight:600; color:#333333; font-size:11px; text-transform:uppercase; letter-spacing:0.5px; background-color:#f9f9f9; border-bottom:1px solid #e5e5e5; width:60px;\">Units</th>\n");
            content.append("<th style=\"padding:12px 15px; text-align:right; font-weight:600; color:#333333; font-size:11px; text-transform:uppercase; letter-spacing:0.5px; background-color:#f9f9f9; border-bottom:1px solid #e5e5e5; width:80px;\">Price</th>\n");
            content.append("<th style=\"padding:12px 15px; text-align:right; font-weight:600; color:#333333; font-size:11px; text-transform:uppercase; letter-spacing:0.5px; background-color:#f9f9f9; border-bottom:1px solid #e5e5e5; width:80px;\">Amount</th>\n");
            content.append("</tr>\n");
            
            for (EmailInvoiceRequest.EmailItemDto item : request.getInvoiceItems()) {
                String description = item.getName();
                if (item.getPackage_() != null && !item.getPackage_().isEmpty()) {
                    description += " - " + item.getPackage_();
                }
                if (item.getVisaType() != null && !item.getVisaType().isEmpty()) {
                    description += " " + item.getVisaType();
                }
                if (item.getCountry() != null && !item.getCountry().isEmpty()) {
                    description += " - " + item.getCountry();
                }
                
                content.append("<tr>\n");
                content.append("<td style=\"padding:14px 15px; color:#333333; font-size:13px;\">").append(escapeHtml(description)).append("</td>\n");
                content.append("<td style=\"padding:14px 15px; color:#333333; font-size:13px; text-align:center;\">1</td>\n");
                content.append("<td style=\"padding:14px 15px; color:#333333; font-size:13px; text-align:right;\">£").append(item.getPrice()).append("</td>\n");
                content.append("<td style=\"padding:14px 15px; color:#333333; font-size:13px; text-align:right; font-weight:600;\">£").append(item.getPrice()).append("</td>\n");
                content.append("</tr>\n");
            }
            
            content.append("</table>\n\n");
        }
        
        // Totals Section
        content.append("<!-- Totals -->\n");
        content.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:240px; margin:0 0 25px auto; font-family:Arial,sans-serif;\">\n");
        content.append("<tr>\n");
        content.append("<td style=\"padding:8px 0; color:#888888; font-size:13px;\">Subtotal</td>\n");
        content.append("<td style=\"padding:8px 0; text-align:right; color:#333333; font-size:13px;\">£").append(request.getSubtotal()).append("</td>\n");
        content.append("</tr>\n");
        
        // Add discount row if applicable
        if (request.getDiscountAmount() != null && !request.getDiscountAmount().equals("0") && !request.getDiscountAmount().equals("0.00")) {
            String discountLabel = "Discount";
            if (request.getDiscountPercent() != null && !request.getDiscountPercent().equals("0")) {
                discountLabel = "Discount (" + request.getDiscountPercent() + "%)";
            }
            content.append("<tr>\n");
            content.append("<td style=\"padding:8px 0; color:#20c997; font-size:13px;\">").append(discountLabel).append("</td>\n");
            content.append("<td style=\"padding:8px 0; text-align:right; color:#20c997; font-size:13px; font-weight:600;\">-£").append(request.getDiscountAmount()).append("</td>\n");
            content.append("</tr>\n");
        }
        
        content.append("<tr>\n");
        content.append("<td colspan=\"2\" style=\"padding:8px 0;\"><div style=\"border-top:1px solid #e5e5e5;\"></div></td>\n");
        content.append("</tr>\n");
        content.append("<tr>\n");
        content.append("<td style=\"padding:10px 0; color:#333333; font-size:14px; font-weight:600;\">Total</td>\n");
        content.append("<td style=\"padding:10px 0; text-align:right; color:#1e3a5f; font-size:20px; font-weight:700;\">£").append(request.getTotal()).append("</td>\n");
        content.append("</tr>\n");
        content.append("</table>");
        
        return content.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private String getCurrentDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMM yyyy"));
    }

    private String getDueDate() {
        return LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ofPattern("d MMM yyyy"));
    }
}
