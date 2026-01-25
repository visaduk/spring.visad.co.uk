package uk.co.visad.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.visad.dto.EmailDto;
import uk.co.visad.entity.*;
import uk.co.visad.exception.ResourceNotFoundException;
import uk.co.visad.repository.TravelerRepository;
import uk.co.visad.repository.DependentRepository;
import uk.co.visad.repository.EmailLogRepository;
import uk.co.visad.security.UserPrincipal;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TravelerRepository travelerRepository;
    private final DependentRepository dependentRepository;
    // private final InvoiceHistoryRepository invoiceHistoryRepository; // Removed:
    // table doesn't exist in production
    private final EmailLogRepository emailLogRepository;
    private final AuditService auditService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.properties.mail.from}")
    private String fromEmail;

    @Transactional
    public EmailDto.EmailResponse sendInvoiceEmail(EmailDto.SendInvoiceRequest request) {
        List<String> validEmails = getValidEmails(request);
        if (validEmails.isEmpty()) {
            return EmailDto.EmailResponse.builder()
                    .status("error")
                    .message("No valid email addresses found")
                    .build();
        }

        int sentCount = 0;
        List<String> failedEmails = new ArrayList<>();

        for (String email : validEmails) {
            try {
                sendInvoiceEmailInternal(email, request);

                // Log to invoice_history - DISABLED: table doesn't exist in production
                // InvoiceHistory history = InvoiceHistory.builder()
                // .recordId(request.getRecordId())
                // .recordType(request.getRecordType())
                // .invoiceType("invoice")
                // .invoiceNumber(request.getInvoiceNumber())
                // .sentToEmail(email)
                // .build();
                // invoiceHistoryRepository.save(history);

                sentCount++;
            } catch (Exception e) {
                log.error("Failed to send invoice email to {}: {}", email, e.getMessage());
                failedEmails.add(email);
            }
        }

        // Audit log
        if (sentCount > 0) {
            auditService.logChange(request.getRecordType(), request.getRecordId(),
                    request.getCustomerName(), "Invoice Email Sent", "",
                    String.join(", ", validEmails.subList(0, sentCount)));
        }

        return buildEmailResponse(sentCount, validEmails.size(), failedEmails);
    }

    @Transactional
    public EmailDto.EmailResponse sendVerificationEmail(EmailDto.SendVerificationRequest request) {
        try {
            Object record;
            if ("traveler".equals(request.getRecordType())) {
                record = travelerRepository.findById(request.getRecordId())
                        .orElseThrow(() -> new ResourceNotFoundException("Traveler not found"));
            } else {
                record = dependentRepository.findById(request.getRecordId())
                        .orElseThrow(() -> new ResourceNotFoundException("Dependent not found"));
            }

            String emailHtml = buildVerificationEmailHtml(record, request.getRecordType());
            String firstName = getFirstName(record);
            String subject = "Visa Appointment Waiting and Details Review - " + firstName;

            sendHtmlEmail(request.getEmail(), subject, emailHtml);

            // Log email
            Long userId = getCurrentUserId();
            EmailLog emailLog = EmailLog.builder()
                    .recordId(request.getRecordId())
                    .recordType(request.getRecordType())
                    .recipientEmail(request.getEmail())
                    .sentBy(userId)
                    .subject(subject)
                    .build();
            emailLogRepository.save(emailLog);

            return EmailDto.EmailResponse.builder()
                    .status("success")
                    .message("Email sent successfully to " + request.getEmail())
                    .sentCount(1)
                    .build();

        } catch (Exception e) {
            log.error("Failed to send verification email: {}", e.getMessage());
            return EmailDto.EmailResponse.builder()
                    .status("error")
                    .message("Failed to send email: " + e.getMessage())
                    .build();
        }
    }

    private void sendInvoiceEmailInternal(String toEmail, EmailDto.SendInvoiceRequest request)
            throws MessagingException {
        String viewLink = baseUrl + "/view_invoice.php?id=" + request.getRecordId() +
                "&type=" + request.getRecordType();

        String html = buildInvoiceEmailHtml(request.getCustomerName(), request.getInvoiceNumber(), viewLink);
        String subject = "Invoice " + request.getInvoiceNumber() + " - VISAD.CO.UK";

        sendHtmlEmail(toEmail, subject, html);
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
        log.info("Email sent successfully to: {}", to);
    }

    private List<String> getValidEmails(EmailDto.SendInvoiceRequest request) {
        List<String> emails = new ArrayList<>();

        if (request.getEmails() != null && !request.getEmails().isEmpty()) {
            emails.addAll(request.getEmails());
        } else if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            emails.add(request.getEmail());
        }

        return emails.stream()
                .filter(this::isValidEmail)
                .toList();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private EmailDto.EmailResponse buildEmailResponse(int sentCount, int totalCount, List<String> failedEmails) {
        if (sentCount == totalCount) {
            String msg = sentCount > 1
                    ? "Invoice sent successfully to " + sentCount + " recipients"
                    : "Invoice sent successfully";
            return EmailDto.EmailResponse.builder()
                    .status("success")
                    .message(msg)
                    .sentCount(sentCount)
                    .build();
        } else if (sentCount > 0) {
            return EmailDto.EmailResponse.builder()
                    .status("partial")
                    .message("Sent to " + sentCount + " of " + totalCount + " recipients")
                    .sentCount(sentCount)
                    .failedEmails(failedEmails)
                    .build();
        } else {
            return EmailDto.EmailResponse.builder()
                    .status("error")
                    .message("Failed to send email to any recipient")
                    .failedEmails(failedEmails)
                    .build();
        }
    }

    private String buildInvoiceEmailHtml(String customerName, String invoiceNumber, String viewLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
                    <style>
                        body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.7; color: #333333; margin: 0; padding: 0; background-color: #f4f4f4; }
                        .wrapper { padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        .header { background: #003366; color: white; padding: 35px 30px; text-align: center; }
                        .header h1 { margin: 0; font-size: 26px; font-weight: 600; }
                        .content { padding: 35px 30px; background-color: #ffffff; }
                        .invoice-box { background: #f8f9fa; border-left: 4px solid #003366; padding: 15px 20px; margin: 25px 0; border-radius: 0 6px 6px 0; }
                        .button-wrap { text-align: center; margin: 30px 0; }
                        .button { display: inline-block; padding: 14px 35px; background: #0066cc; color: #ffffff; text-decoration: none; border-radius: 6px; font-weight: 600; }
                        .footer { text-align: center; padding: 25px 30px; background-color: #f8f9fa; border-top: 1px solid #e9ecef; }
                    </style>
                </head>
                <body>
                    <div class='wrapper'>
                        <div class='container'>
                            <div class='header'>
                                <h1>VISAD.CO.UK</h1>
                                <p>Professional Services</p>
                            </div>
                            <div class='content'>
                                <h2>Dear %s,</h2>
                                <p>Thank you for choosing VISAD.CO.UK for your visa services.</p>
                                <div class='invoice-box'>
                                    Your invoice <strong>%s</strong> is ready for review.
                                </div>
                                <div class='button-wrap'>
                                    <a href='%s' class='button'>View & Download Invoice</a>
                                </div>
                                <p>If you have any questions, please don't hesitate to contact us.</p>
                                <p>Best regards,<br><strong>VISAD.CO.UK Team</strong></p>
                            </div>
                            <div class='footer'>
                                <p>VISAD.CO.UK | 7 Bell Yard, London WC2A 2JR</p>
                                <p>support@visad.co.uk</p>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(customerName, invoiceNumber, viewLink);
    }

    private String buildVerificationEmailHtml(Object record, String recordType) {
        // Extract data based on record type
        String firstName = "", lastName = "", email = "", phone = "", whatsapp = "";
        String nationality = "", passportNo = "", visaType = "", visaCenter = "", travelCountry = "";
        String packageType = "", fullAddress = "";
        LocalDate passportIssue = null, passportExpire = null, plannedTravelDate = null;

        if (record instanceof Traveler t) {
            firstName = t.getFirstName() != null ? t.getFirstName() : "";
            lastName = t.getLastName() != null ? t.getLastName() : "";
            email = t.getEmail() != null ? t.getEmail() : "";
            phone = t.getContactNumber() != null ? t.getContactNumber() : "";
            whatsapp = t.getWhatsappContact() != null ? t.getWhatsappContact() : "";
            nationality = t.getNationality() != null ? t.getNationality() : "";
            passportNo = t.getPassportNo() != null ? t.getPassportNo() : "";
            passportIssue = t.getPassportIssue();
            passportExpire = t.getPassportExpire();
            visaType = t.getVisaType() != null ? t.getVisaType() : "";
            packageType = t.getPackage_() != null ? t.getPackage_() : "";
            visaCenter = t.getVisaCenter() != null ? t.getVisaCenter() : "";
            travelCountry = t.getTravelCountry() != null ? t.getTravelCountry() : "";
            plannedTravelDate = t.getPlannedTravelDate();
            fullAddress = buildAddress(t.getAddressLine1(), t.getAddressLine2(), t.getCity(),
                    t.getStateProvince(), t.getZipCode(), t.getCountry());
        } else if (record instanceof Dependent d) {
            firstName = d.getFirstName() != null ? d.getFirstName() : "";
            lastName = d.getLastName() != null ? d.getLastName() : "";
            email = d.getEmail() != null ? d.getEmail() : "";
            phone = d.getContactNumber() != null ? d.getContactNumber() : "";
            whatsapp = d.getWhatsappContact() != null ? d.getWhatsappContact() : "";
            nationality = d.getNationality() != null ? d.getNationality() : "";
            passportNo = d.getPassportNo() != null ? d.getPassportNo() : "";
            passportIssue = d.getPassportIssue();
            passportExpire = d.getPassportExpire();
            visaType = d.getVisaType() != null ? d.getVisaType() : "";
            packageType = d.getPackageType() != null ? d.getPackageType() : "";
            visaCenter = d.getVisaCenter() != null ? d.getVisaCenter() : "";
            travelCountry = d.getTravelCountry() != null ? d.getTravelCountry() : "";
            plannedTravelDate = d.getPlannedTravelDate();
            fullAddress = buildAddress(d.getAddressLine1(), d.getAddressLine2(), d.getCity(),
                    d.getStateProvince(), d.getZipCode(), d.getCountry());
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String passportValidity = formatDate(passportIssue, fmt) + " – " + formatDate(passportExpire, fmt);
        String travelDateStr = formatDate(plannedTravelDate, fmt);

        return buildVerificationHtmlTemplate(firstName + " " + lastName, email, phone, whatsapp,
                nationality, passportNo, passportValidity, visaType, packageType, visaCenter,
                travelCountry, travelDateStr, fullAddress);
    }

    private String buildAddress(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(part);
            }
        }
        return sb.toString();
    }

    private String formatDate(LocalDate date, DateTimeFormatter fmt) {
        return date != null ? date.format(fmt) : "Not set";
    }

    private String getFirstName(Object record) {
        if (record instanceof Traveler t) {
            return t.getFirstName() != null ? t.getFirstName() : "Client";
        } else if (record instanceof Dependent d) {
            return d.getFirstName() != null ? d.getFirstName() : "Client";
        }
        return "Client";
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) auth.getPrincipal()).getId();
        }
        return null;
    }

    private String buildVerificationHtmlTemplate(String fullName, String email, String phone,
            String whatsapp, String nationality, String passportNo, String passportValidity,
            String visaType, String packageType, String visaCenter, String travelCountry,
            String travelDate, String address) {

        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; background: #fff; border-radius: 8px; overflow: hidden;">
                        <div style="background: #1e40af; color: white; padding: 30px; text-align: center;">
                            <h1 style="margin: 0;">VISAD.CO.UK</h1>
                            <p style="margin: 5px 0 0 0; opacity: 0.9;">Visa Support Services</p>
                        </div>
                        <div style="padding: 30px;">
                            <h2 style="color: #1e40af;">Dear %s,</h2>
                            <p>Thank you for choosing our visa services. Below are your registration details for verification:</p>

                            <table style="width: 100%%; border-collapse: collapse; margin: 20px 0;">
                                <tr><td style="padding: 8px 0; color: #666;"><strong>Email:</strong></td><td>%s</td></tr>
                                <tr><td style="padding: 8px 0; color: #666;"><strong>Phone:</strong></td><td>%s</td></tr>
                                <tr><td style="padding: 8px 0; color: #666;"><strong>WhatsApp:</strong></td><td>%s</td></tr>
                                <tr><td style="padding: 8px 0; color: #666;"><strong>Nationality:</strong></td><td>%s</td></tr>
                                <tr><td style="padding: 8px 0; color: #666;"><strong>Passport Number:</strong></td><td>%s</td></tr>
                                <tr><td style="padding: 8px 0; color: #666;"><strong>Passport Validity:</strong></td><td>%s</td></tr>
                                <tr><td style="padding: 8px 0; color: #666;"><strong>Visa Type:</strong></td><td>%s – %s</td></tr>
                                <tr><td style="padding: 8px 0; color: #666;"><strong>VAC Location:</strong></td><td>%s</td></tr>
                                <tr><td style="padding: 8px 0; color: #666;"><strong>Travel Country:</strong></td><td>%s</td></tr>
                                <tr><td style="padding: 8px 0; color: #666;"><strong>Planned Travel Date:</strong></td><td>%s</td></tr>
                                <tr><td style="padding: 8px 0; color: #666;"><strong>Address:</strong></td><td>%s</td></tr>
                            </table>

                            <h3 style="color: #1e40af;">Processing Time</h3>
                            <p>Most applicants receive their passport within <strong>15 days</strong> of appointment. We recommend a <strong>25-day gap</strong> between appointment and travel.</p>

                            <h3 style="color: #1e40af;">Important Notes</h3>
                            <ul>
                                <li>Only <strong>fully paid flight tickets</strong> are accepted</li>
                                <li>Arrive at VAC <strong>30 minutes before</strong> your appointment</li>
                                <li>Print all documents from your checklist</li>
                            </ul>

                            <p style="margin-top: 30px;">Best regards,<br><strong>VisaD - Doc Support Team</strong></p>
                        </div>
                        <div style="background: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666;">
                            <p>This is an automated email. For assistance, contact us at info@visad.co.uk</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(fullName, email, phone, whatsapp, nationality, passportNo,
                        passportValidity, visaType, packageType, visaCenter, travelCountry,
                        travelDate, address);
    }
}
