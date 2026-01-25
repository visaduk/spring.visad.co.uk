package uk.co.visad.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.co.visad.dto.ApiResponse;
import uk.co.visad.dto.EmailDto;
import uk.co.visad.service.EmailService;

@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    /**
     * Send invoice email
     * PHP equivalent: email_handler.php?action=send_invoice
     */
    @PostMapping("/send_invoice")
    public ResponseEntity<ApiResponse<EmailDto.EmailResponse>> sendInvoice(
            @Valid @RequestBody EmailDto.SendInvoiceRequest request) {
        EmailDto.EmailResponse response = emailService.sendInvoiceEmail(request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    /**
     * Send T-Invoice email (with Trustpilot BCC)
     * PHP equivalent: email_handler.php?action=send_t_invoice
     */
    @PostMapping("/send_t_invoice")
    public ResponseEntity<ApiResponse<EmailDto.EmailResponse>> sendTInvoice(
            @Valid @RequestBody EmailDto.SendInvoiceRequest request) {
        // For now, same as regular invoice - can add Trustpilot BCC logic
        EmailDto.EmailResponse response = emailService.sendInvoiceEmail(request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    /**
     * Send verification email
     * PHP equivalent: send_verification_email.php
     */
    @PostMapping("/send_verification")
    public ResponseEntity<ApiResponse<EmailDto.EmailResponse>> sendVerification(
            @Valid @RequestBody EmailDto.SendVerificationRequest request) {
        EmailDto.EmailResponse response = emailService.sendVerificationEmail(request);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }
}
