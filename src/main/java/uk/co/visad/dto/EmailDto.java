package uk.co.visad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class EmailDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SendInvoiceRequest {
        @NotNull(message = "Record ID is required")
        private Long recordId;

        private String recordType = "traveler";

        private String invoiceNumber;

        private String customerName;

        // Single email (backward compatibility)
        @Email(message = "Invalid email format")
        private String email;

        // Multiple emails
        private List<String> emails;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SendVerificationRequest {
        @NotNull(message = "Record ID is required")
        private Long recordId;

        @NotBlank(message = "Record type is required")
        private String recordType;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmailResponse {
        private String status;
        private String message;
        private int sentCount;
        private List<String> failedEmails;
    }
}
