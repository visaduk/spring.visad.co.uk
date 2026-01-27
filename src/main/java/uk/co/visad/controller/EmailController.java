package uk.co.visad.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.co.visad.dto.ApiResponse;
import uk.co.visad.dto.EmailInvoiceRequest;
import uk.co.visad.service.EmailService;

@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/send-invoice")
    public ResponseEntity<ApiResponse<Void>> sendInvoice(@RequestBody EmailInvoiceRequest request) {
        emailService.sendInvoiceEmail(request);
        return ResponseEntity.ok(ApiResponse.successMessage("Email sent successfully"));
    }
}
