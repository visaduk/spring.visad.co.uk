package uk.co.visad.dto;

import lombok.Data;

@Data
public class TestEmailRequest {
    private String toEmail;
    private String subject;
    private String message;
}
