package uk.co.visad.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisaUrlDto {

    private Long id;

    @NotBlank(message = "Country is required")
    private String country;

    private String visaCenter;

    @NotBlank(message = "URL is required")
    private String url;

    private String applicationFormUrl;

    private Boolean isUploadedFile;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "Country is required")
        private String country;
        private String visaCenter;
        @NotBlank(message = "URL is required")
        private String url;
        private String applicationFormUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private Long id;
        @NotBlank(message = "Country is required")
        private String country;
        private String visaCenter;
        @NotBlank(message = "URL is required")
        private String url;
        private String applicationFormUrl;
    }
}
