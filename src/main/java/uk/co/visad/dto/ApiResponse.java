package uk.co.visad.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private String status;
    private String message;
    private T data;
    private PaginationInfo pagination;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status("success")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .status("success")
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, PaginationInfo pagination) {
        return ApiResponse.<T>builder()
                .status("success")
                .data(data)
                .pagination(pagination)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .status("error")
                .message(message)
                .build();
    }

    public static ApiResponse<Void> successMessage(String message) {
        return ApiResponse.<Void>builder()
                .status("success")
                .message(message)
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaginationInfo {
        private int page;
        private int limit;
        private long totalRecords;
        private int totalPages;
        private boolean hasMore;
    }
}
