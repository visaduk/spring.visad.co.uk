package uk.co.visad.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class AuditLogDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String username;
        private String recordType;
        private String recordName;
        private String fieldChanged;
        private String oldValue;
        private String newValue;

        @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
        private String formattedTimestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevertRequest {
        private Long logId;
    }
}
