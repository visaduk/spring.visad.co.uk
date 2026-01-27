package uk.co.visad.controller;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import uk.co.visad.dto.ApiResponse;
import uk.co.visad.dto.TestEmailRequest;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class TestEmailController {

    private final JavaMailSender mailSender;

    @PostMapping("/send-email")
    public ResponseEntity<ApiResponse<String>> sendTestEmail(@RequestBody TestEmailRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(request.getToEmail());
            helper.setSubject(request.getSubject() != null ? request.getSubject() : "Test Email from VisaD");
            helper.setFrom("info@visad.co.uk");

            String htmlBody = buildTestEmailHtml(request.getMessage());
            helper.setText(htmlBody, true);

            mailSender.send(message);
            
            log.info("Test email sent successfully to {}", request.getToEmail());
            return ResponseEntity.ok(ApiResponse.success("Email sent successfully to " + request.getToEmail()));
            
        } catch (MessagingException e) {
            log.error("Failed to send test email", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to send email: " + e.getMessage()));
        }
    }

    private String buildTestEmailHtml(String customMessage) {
        String message = customMessage != null ? customMessage : "This is a test email from the VisaD Spring Boot application.";
        
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "</head>" +
                "<body style=\"margin:0; padding:0; background-color:#f5f5f5; font-family:Arial,sans-serif;\">" +
                "    <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#f5f5f5;\">" +
                "        <tr>" +
                "            <td align=\"center\" style=\"padding:40px 20px;\">" +
                "                <table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#ffffff; border-radius:12px; box-shadow:0 2px 8px rgba(0,0,0,0.05);\">" +
                "                    <tr>" +
                "                        <td style=\"padding:35px 40px 25px 40px; text-align:center; border-bottom:1px solid #eee;\">" +
                "                            <span style=\"font-size:32px; font-weight:800; letter-spacing:1px; color:#1e3a5f;\">VISA</span>" +
                "                            <span style=\"font-size:32px; font-weight:800; letter-spacing:1px; color:#20c997;\">D</span>" +
                "                            <p style=\"margin:8px 0 0 0; font-size:12px; color:#888888;\">Email System Test</p>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding:30px 40px 20px 40px; text-align:center;\">" +
                "                            <div style=\"display:inline-block; background-color:#d4edda; border:1px solid #c3e6cb; border-radius:50px; padding:12px 30px;\">" +
                "                                <span style=\"font-size:16px; font-weight:600; color:#155724;\">✓ Email System Working</span>" +
                "                            </div>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding:10px 40px 35px 40px;\">" +
                "                            <p style=\"margin:0 0 12px 0; font-size:18px; color:#1e3a5f; font-weight:600;\">Test Email</p>" +
                "                            <p style=\"margin:0; font-size:15px; color:#555555; line-height:1.7;\">" + message + "</p>" +
                "                            <div style=\"margin-top:25px; padding:20px; background-color:#f8f9fa; border-radius:8px; border-left:3px solid #20c997;\">" +
                "                                <p style=\"margin:0; font-size:13px; color:#666666;\"><strong>Server:</strong> mail.visad.co.uk</p>" +
                "                                <p style=\"margin:5px 0 0 0; font-size:13px; color:#666666;\"><strong>From:</strong> info@visad.co.uk</p>" +
                "                                <p style=\"margin:5px 0 0 0; font-size:13px; color:#666666;\"><strong>Time:</strong> " + java.time.LocalDateTime.now() + "</p>" +
                "                            </div>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <tr>" +
                "                        <td style=\"padding:25px 40px; background-color:#f8f9fa; border-top:1px solid #eee; text-align:center; border-radius:0 0 12px 12px;\">" +
                "                            <p style=\"margin:0 0 8px 0; font-size:13px; color:#666666;\">This is an automated test email from the VisaD Spring Boot application.</p>" +
                "                            <p style=\"margin:0; font-size:12px; color:#999999;\"><a href=\"https://www.visad.co.uk\" style=\"color:#1e3a5f; text-decoration:none; font-weight:600;\">www.visad.co.uk</a></p>" +
                "                        </td>" +
                "                    </tr>" +
                "                </table>" +
                "            </td>" +
                "        </tr>" +
                "    </table>" +
                "</body>" +
                "</html>";
    }
}
