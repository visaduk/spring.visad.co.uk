package uk.co.visad.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class VerificationEmailRequest {

    @JsonProperty("record_id")
    private Long recordId;

    @JsonProperty("record_type")
    private String recordType;

    private String email;

    @JsonProperty("email_html")
    private String emailHtml;

    @JsonProperty("first_name")
    private String firstName;
}
