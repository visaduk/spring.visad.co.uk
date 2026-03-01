package uk.co.visad.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import uk.co.visad.dto.EmailInvoiceRequest;
import uk.co.visad.dto.VerificationEmailRequest;
import uk.co.visad.entity.InvoiceHistory;
import uk.co.visad.repository.InvoiceHistoryRepository;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final InvoiceHistoryRepository invoiceHistoryRepository;

    private static final String FROM_EMAIL        = "info@visad.co.uk";
    private static final String TRUSTPILOT_BCC    = "visad.co.uk+5e14bff186@invite.trustpilot.com";

    // -------------------------------------------------------------------------
    // Public entry point
    // -------------------------------------------------------------------------

    public void sendInvoiceEmail(EmailInvoiceRequest request) {
        boolean isTInvoice = "send_t_invoice".equals(request.getAction());

        byte[] pdfBytes  = generateInvoicePdf(request);
        String pdfName   = "Invoice-" + sanitize(request.getInvoiceNumber()) + ".pdf";
        String subject   = buildSubject(request, isTInvoice);

        if (isTInvoice) {
            sendTInvoice(request, pdfBytes, pdfName, subject);
        } else {
            sendRegularInvoice(request, pdfBytes, pdfName, subject);
        }
    }

    // -------------------------------------------------------------------------
    // Regular invoice — one email to all recipients
    // -------------------------------------------------------------------------

    private void sendRegularInvoice(EmailInvoiceRequest request, byte[] pdfBytes, String pdfName, String subject) {
        try {
            String toEmail = request.getEmails() != null && !request.getEmails().isEmpty()
                    ? request.getEmails().get(0) : request.getCustomerEmail();

            String[] allTo = request.getEmails() != null
                    ? request.getEmails().toArray(new String[0])
                    : new String[]{request.getCustomerEmail()};

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(allTo);
            helper.setSubject(subject);
            helper.setFrom(FROM_EMAIL);

            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                helper.setBcc(request.getBcc());
            }

            helper.setText(buildEmailHtml(request, request.getCustomerName(), false), true);
            helper.addAttachment(pdfName, new ByteArrayDataSource(pdfBytes, "application/pdf"));

            mailSender.send(message);
            log.info("Invoice email sent to {}", (Object) allTo);

            logHistory(request.getRecordId(), request.getRecordType(),
                    "invoice", request.getInvoiceNumber(), toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send invoice email", e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // T-Invoice — one personalised email per applicant
    // -------------------------------------------------------------------------

    private void sendTInvoice(EmailInvoiceRequest request, byte[] pdfBytes, String pdfName, String subject) {
        List<EmailInvoiceRequest.ApplicantDto> applicants = request.getApplicants();

        if (applicants == null || applicants.isEmpty()) {
            // Fallback: send to emails list if no applicants provided
            sendRegularInvoice(request, pdfBytes, pdfName, subject);
            return;
        }

        for (EmailInvoiceRequest.ApplicantDto applicant : applicants) {
            if (applicant.getEmail() == null || applicant.getEmail().isBlank()) continue;

            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setTo(applicant.getEmail());
                helper.setSubject(subject);
                helper.setFrom(FROM_EMAIL);
                helper.setBcc(TRUSTPILOT_BCC);

                helper.setText(buildEmailHtml(request, applicant.getName(), true), true);
                helper.addAttachment(pdfName, new ByteArrayDataSource(pdfBytes, "application/pdf"));

                mailSender.send(message);
                log.info("T-Invoice email sent to {}", applicant.getEmail());

                logHistory(request.getRecordId(), request.getRecordType(),
                        "t-invoice", request.getInvoiceNumber(), applicant.getEmail());

            } catch (MessagingException e) {
                log.error("Failed to send T-Invoice to {}", applicant.getEmail(), e);
                // Continue sending to remaining applicants
            }
        }
    }

    // -------------------------------------------------------------------------
    // Invoice history logging
    // -------------------------------------------------------------------------

    private void logHistory(Long recordId, String recordType, String invoiceType,
                            String invoiceNumber, String sentToEmail) {
        if (recordId == null) return;
        try {
            InvoiceHistory history = InvoiceHistory.builder()
                    .recordId(recordId)
                    .recordType(recordType != null ? recordType : "traveler")
                    .invoiceType(invoiceType)
                    .invoiceNumber(invoiceNumber)
                    .sentToEmail(sentToEmail)
                    .sentAt(LocalDateTime.now())
                    .build();
            invoiceHistoryRepository.save(history);
        } catch (Exception e) {
            log.error("Failed to log invoice history", e);
        }
    }

    // -------------------------------------------------------------------------
    // Verification email — HTML body is pre-built by the frontend
    // -------------------------------------------------------------------------

    public void sendVerificationEmail(VerificationEmailRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("No email address provided");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(request.getEmail());
            helper.setFrom(FROM_EMAIL);
            helper.setSubject("Your Visa Application — Document Verification");
            helper.setText(request.getEmailHtml() != null ? request.getEmailHtml() : "Please contact us.", true);
            mailSender.send(message);
            log.info("Verification email sent to {}", request.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send verification email", e);
            throw new RuntimeException("Failed to send verification email: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // PDF generation
    // -------------------------------------------------------------------------

    private byte[] generateInvoicePdf(EmailInvoiceRequest request) {
        String html = buildPdfHtml(request);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(baos);
            builder.run();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate invoice PDF", e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage());
        }
    }

    private String buildPdfHtml(EmailInvoiceRequest request) {
        boolean isTInvoice = "send_t_invoice".equals(request.getAction());
        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/>");
        h.append("<style>");
        h.append("body{font-family:Arial,sans-serif;color:#333;font-size:13px;margin:40px;}");
        h.append(".header{text-align:center;border-bottom:2px solid #1e3a5f;padding-bottom:16px;margin-bottom:24px;}");
        h.append(".logo-visa{font-size:28px;font-weight:800;color:#1e3a5f;}");
        h.append(".logo-d{font-size:28px;font-weight:800;color:#20c997;}");
        h.append(".address{font-size:11px;color:#888;margin-top:4px;}");
        h.append("table{width:100%;border-collapse:collapse;margin-bottom:16px;}");
        h.append("th{background:#f0f0f0;padding:8px;text-align:left;font-size:11px;border:1px solid #ddd;}");
        h.append("td{padding:8px;border:1px solid #ddd;font-size:12px;}");
        h.append(".totals-table{width:220px;margin-left:auto;border:none;}");
        h.append(".totals-table td{border:none;padding:4px 0;}");
        h.append(".total-row td{font-size:15px;font-weight:700;color:#1e3a5f;border-top:1px solid #ddd;padding-top:8px;}");
        h.append(".badge{display:inline-block;padding:4px 12px;border-radius:20px;font-size:11px;font-weight:600;}");
        h.append(".badge-paid{background:#d4edda;color:#155724;}");
        h.append(".badge-unpaid{background:#fff3cd;color:#856404;}");
        h.append("</style></head><body>");

        // Header
        h.append("<div class='header'>");
        h.append("<span class='logo-visa'>VISA</span><span class='logo-d'>D</span>");
        h.append("<div class='address'>iWeBron Limited, 7 Bell Yard, London WC2A 2JR</div>");
        h.append("</div>");

        // Invoice meta
        h.append("<table style='margin-bottom:24px;'><tr>");
        h.append("<td><strong>Invoice No.</strong><br/>").append(esc(request.getInvoiceNumber())).append("</td>");
        h.append("<td><strong>Date</strong><br/>").append(getCurrentDate()).append("</td>");
        h.append("<td><strong>").append(isTInvoice ? "Amount Paid" : "Amount Due").append("</strong><br/>£").append(esc(request.getTotal())).append("</td>");
        h.append("<td><strong>Status</strong><br/>");
        if (isTInvoice) {
            h.append("<span class='badge badge-paid'>&#10003; PAID</span>");
        } else {
            h.append("<span class='badge badge-unpaid'>PENDING</span>");
        }
        h.append("</td></tr></table>");

        // Bill to
        h.append("<div style='margin-bottom:20px;'>");
        h.append("<strong>Bill To:</strong><br/>");
        h.append(esc(request.getCustomerName())).append("<br/>");
        h.append(esc(request.getCustomerAddress())).append("<br/>");
        h.append(esc(request.getCustomerEmail()));
        h.append("</div>");

        // Items
        if (request.getInvoiceItems() != null && !request.getInvoiceItems().isEmpty()) {
            h.append("<table><tr>");
            h.append("<th>Description</th><th style='width:60px;text-align:center;'>Units</th>");
            h.append("<th style='width:80px;text-align:right;'>Amount</th>");
            h.append("</tr>");
            for (EmailInvoiceRequest.EmailItemDto item : request.getInvoiceItems()) {
                String desc = esc(item.getName());
                if (item.getPackage_() != null) desc += " - " + esc(item.getPackage_());
                if (item.getVisaType() != null) desc += " " + esc(item.getVisaType());
                if (item.getCountry() != null) desc += " - " + esc(item.getCountry());
                h.append("<tr><td>").append(desc).append("</td>");
                h.append("<td style='text-align:center;'>1</td>");
                h.append("<td style='text-align:right;'>£").append(esc(item.getPrice())).append("</td></tr>");
            }
            h.append("</table>");
        }

        // Totals
        h.append("<table class='totals-table'>");
        h.append("<tr><td>Subtotal</td><td style='text-align:right;'>£").append(esc(request.getSubtotal())).append("</td></tr>");
        if (request.getDiscountAmount() != null && !request.getDiscountAmount().equals("0") && !request.getDiscountAmount().equals("0.00")) {
            String discLabel = "Discount";
            if (request.getDiscountPercent() != null && !request.getDiscountPercent().equals("0")) {
                discLabel += " (" + request.getDiscountPercent() + "%)";
            }
            h.append("<tr><td style='color:#20c997;'>").append(discLabel).append("</td>");
            h.append("<td style='text-align:right;color:#20c997;'>-£").append(esc(request.getDiscountAmount())).append("</td></tr>");
        }
        h.append("<tr class='total-row'><td>Total</td><td style='text-align:right;'>£").append(esc(request.getTotal())).append("</td></tr>");
        h.append("</table>");

        h.append("</body></html>");
        return h.toString();
    }

    // -------------------------------------------------------------------------
    // Email HTML body builder
    // -------------------------------------------------------------------------

    private String buildEmailHtml(EmailInvoiceRequest request, String customerName, boolean isTInvoice) {
        String invoiceContent = buildInvoiceContent(request, isTInvoice);
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("</head>\n");
        html.append("<body style=\"margin:0; padding:0; background-color:#f5f5f5; font-family:Arial,sans-serif;\">\n");
        html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#f5f5f5;\">\n");
        html.append("<tr><td align=\"center\" style=\"padding:40px 20px;\">\n");
        html.append("<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#ffffff; border-radius:12px; box-shadow:0 2px 8px rgba(0,0,0,0.05);\">\n");

        // Header
        html.append("<tr><td style=\"padding:35px 40px 25px 40px; text-align:center; border-bottom:1px solid #eee;\">\n");
        html.append("<span style=\"font-size:32px; font-weight:800; letter-spacing:1px; color:#1e3a5f;\">VISA</span>");
        html.append("<span style=\"font-size:32px; font-weight:800; letter-spacing:1px; color:#20c997;\">D</span>\n");
        html.append("<p style=\"margin:8px 0 0 0; font-size:12px; color:#888888;\">iWeBron Limited, 7 Bell Yard, London WC2A 2JR</p>\n");
        html.append("</td></tr>\n");

        // Badge
        html.append("<tr><td style=\"padding:30px 40px 20px 40px; text-align:center;\">\n");
        if (isTInvoice) {
            html.append("<div style=\"display:inline-block; background-color:#d4edda; border:1px solid #c3e6cb; border-radius:50px; padding:12px 30px;\">");
            html.append("<span style=\"font-size:16px; font-weight:600; color:#155724;\">&#10003; Payment Confirmed</span></div>\n");
        } else {
            html.append("<div style=\"display:inline-block; background-color:#e7f3ff; border:1px solid #b8daff; border-radius:50px; padding:12px 30px;\">");
            html.append("<span style=\"font-size:16px; font-weight:600; color:#004085;\">&#128196; Invoice</span></div>\n");
        }
        html.append("</td></tr>\n");

        // Greeting
        html.append("<tr><td style=\"padding:10px 40px 25px 40px;\">\n");
        html.append("<p style=\"margin:0 0 12px 0; font-size:18px; color:#1e3a5f; font-weight:600;\">Dear ").append(esc(customerName)).append(",</p>\n");
        html.append("<p style=\"margin:0; font-size:15px; color:#555555; line-height:1.7;\">");
        html.append(isTInvoice
                ? "Thank you for choosing VISAD. Your payment has been successfully received and processed."
                : "Thank you for choosing VISAD. Please find your invoice details below.");
        html.append("</p>\n</td></tr>\n");

        // Invoice content
        html.append("<tr><td style=\"padding:0 40px 35px 40px;\">").append(invoiceContent).append("</td></tr>\n");

        // Footer
        html.append("<tr><td style=\"padding:25px 40px; background-color:#f8f9fa; border-top:1px solid #eee; text-align:center; border-radius:0 0 12px 12px;\">\n");
        html.append("<p style=\"margin:0 0 8px 0; font-size:13px; color:#666666;\">Need help? Contact us at ");
        html.append("<a href=\"mailto:support@visad.co.uk\" style=\"color:#20c997; text-decoration:none;\">support@visad.co.uk</a></p>\n");
        html.append("<p style=\"margin:0; font-size:12px; color:#999999;\">");
        html.append("<a href=\"https://www.visad.co.uk\" style=\"color:#1e3a5f; text-decoration:none; font-weight:600;\">www.visad.co.uk</a></p>\n");
        html.append("</td></tr>\n");

        html.append("</table>\n</td></tr>\n</table>\n</body>\n</html>");
        return html.toString();
    }

    private String buildInvoiceContent(EmailInvoiceRequest request, boolean isTInvoice) {
        StringBuilder c = new StringBuilder();

        // Invoice meta row
        c.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%; margin-bottom:20px; font-family:Arial,sans-serif; border:1px solid #e9ecef; border-radius:8px;\">\n<tr>\n");
        c.append(metaCell("Invoice No.", esc(request.getInvoiceNumber()), true));
        c.append(metaCell("Invoice Date", getCurrentDate(), true));
        c.append(metaCell(isTInvoice ? "Payment Date" : "Due Date", isTInvoice ? getCurrentDate() : getDueDate(), true));
        c.append(metaCell(isTInvoice ? "Amount Paid" : "Amount Due", "£" + request.getTotal(), true));
        c.append("<td style=\"width:20%; padding:14px 15px; text-align:center;\">");
        c.append("<div style=\"font-size:10px; color:#888888; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:4px;\">Status</div>");
        if (isTInvoice) {
            c.append("<div style=\"display:inline-block; padding:5px 12px; background-color:#d4edda; color:#155724; font-size:11px; font-weight:600; border-radius:20px;\">&#10003; PAID</div>");
        } else {
            c.append("<div style=\"display:inline-block; padding:5px 12px; background-color:#fff3cd; color:#856404; font-size:11px; font-weight:600; border-radius:20px;\">PENDING</div>");
        }
        c.append("</td>\n</tr>\n</table>\n\n");

        // Bill To
        c.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%; margin-bottom:20px;\">\n<tr>\n");
        c.append("<td style=\"padding:16px 18px; background-color:#f8f9fa; border-radius:8px; border-left:3px solid ").append(isTInvoice ? "#20c997" : "#1e3a5f").append(";\">\n");
        c.append("<div style=\"font-size:10px; color:#888888; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:8px;\">Bill To</div>\n");
        c.append("<div style=\"font-size:15px; color:#1e3a5f; font-weight:600; margin-bottom:5px;\">").append(esc(request.getCustomerName())).append("</div>\n");
        c.append("<div style=\"font-size:13px; color:#555555; line-height:1.6;\">").append(esc(request.getCustomerAddress())).append("</div>\n");
        c.append("<div style=\"margin-top:8px;\"><a href=\"mailto:").append(esc(request.getCustomerEmail())).append("\" style=\"color:#20c997; text-decoration:none; font-size:13px;\">").append(esc(request.getCustomerEmail())).append("</a></div>\n");
        c.append("</td>\n</tr>\n</table>\n\n");

        // Items
        if (request.getInvoiceItems() != null && !request.getInvoiceItems().isEmpty()) {
            c.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:100%; margin-bottom:20px; border:1px solid #e5e5e5; border-radius:8px; border-collapse:separate;\">\n");
            c.append("<tr>\n");
            c.append(th("Description")).append(th("Units", "center", "60px")).append(th("Price", "right", "80px")).append(th("Amount", "right", "80px"));
            c.append("</tr>\n");
            for (EmailInvoiceRequest.EmailItemDto item : request.getInvoiceItems()) {
                String desc = esc(item.getName());
                if (item.getPackage_() != null && !item.getPackage_().isEmpty()) desc += " - " + esc(item.getPackage_());
                if (item.getVisaType() != null && !item.getVisaType().isEmpty()) desc += " " + esc(item.getVisaType());
                if (item.getCountry() != null && !item.getCountry().isEmpty()) desc += " - " + esc(item.getCountry());
                c.append("<tr>");
                c.append("<td style=\"padding:14px 15px; color:#333333; font-size:13px;\">").append(desc).append("</td>");
                c.append("<td style=\"padding:14px 15px; color:#333333; font-size:13px; text-align:center;\">1</td>");
                c.append("<td style=\"padding:14px 15px; color:#333333; font-size:13px; text-align:right;\">£").append(esc(item.getPrice())).append("</td>");
                c.append("<td style=\"padding:14px 15px; color:#333333; font-size:13px; text-align:right; font-weight:600;\">£").append(esc(item.getPrice())).append("</td>");
                c.append("</tr>\n");
            }
            c.append("</table>\n\n");
        }

        // Totals
        c.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width:240px; margin:0 0 25px auto;\">\n");
        c.append("<tr><td style=\"padding:8px 0; color:#888888; font-size:13px;\">Subtotal</td>");
        c.append("<td style=\"padding:8px 0; text-align:right; color:#333333; font-size:13px;\">£").append(esc(request.getSubtotal())).append("</td></tr>\n");
        if (request.getDiscountAmount() != null && !request.getDiscountAmount().equals("0") && !request.getDiscountAmount().equals("0.00")) {
            String discLabel = "Discount";
            if (request.getDiscountPercent() != null && !request.getDiscountPercent().equals("0")) {
                discLabel += " (" + request.getDiscountPercent() + "%)";
            }
            c.append("<tr><td style=\"padding:8px 0; color:#20c997; font-size:13px;\">").append(discLabel).append("</td>");
            c.append("<td style=\"padding:8px 0; text-align:right; color:#20c997; font-size:13px; font-weight:600;\">-£").append(esc(request.getDiscountAmount())).append("</td></tr>\n");
        }
        c.append("<tr><td colspan=\"2\" style=\"padding:8px 0;\"><div style=\"border-top:1px solid #e5e5e5;\"></div></td></tr>\n");
        c.append("<tr><td style=\"padding:10px 0; color:#333333; font-size:14px; font-weight:600;\">Total</td>");
        c.append("<td style=\"padding:10px 0; text-align:right; color:#1e3a5f; font-size:20px; font-weight:700;\">£").append(esc(request.getTotal())).append("</td></tr>\n");
        c.append("</table>");

        return c.toString();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String metaCell(String label, String value, boolean border) {
        return "<td style=\"width:20%; padding:14px 15px;" + (border ? " border-right:1px solid #e9ecef;" : "") + "\">"
                + "<div style=\"font-size:10px; color:#888888; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:4px;\">" + label + "</div>"
                + "<div style=\"font-size:13px; color:#333333; font-weight:500;\">" + value + "</div>"
                + "</td>\n";
    }

    private String th(String label) {
        return "<th style=\"padding:12px 15px; text-align:left; font-weight:600; color:#333333; font-size:11px; text-transform:uppercase; background-color:#f9f9f9; border-bottom:1px solid #e5e5e5;\">" + label + "</th>\n";
    }

    private String th(String label, String align, String width) {
        return "<th style=\"padding:12px 15px; text-align:" + align + "; font-weight:600; color:#333333; font-size:11px; text-transform:uppercase; background-color:#f9f9f9; border-bottom:1px solid #e5e5e5; width:" + width + ";\">" + label + "</th>\n";
    }

    private String esc(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String buildSubject(EmailInvoiceRequest request, boolean isTInvoice) {
        if (request.getSubject() != null && !request.getSubject().isBlank()) return request.getSubject();
        return isTInvoice
                ? "Payment Confirmation - " + request.getInvoiceNumber() + " - VISAD.CO.UK"
                : "Invoice " + request.getInvoiceNumber() + " - VISAD.CO.UK";
    }

    private String sanitize(String s) {
        return s == null ? "invoice" : s.replaceAll("[^a-zA-Z0-9\\-_]", "");
    }

    private String getCurrentDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMM yyyy"));
    }

    private String getDueDate() {
        return LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ofPattern("d MMM yyyy"));
    }
}
