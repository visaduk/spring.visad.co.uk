package uk.co.visad.dto;

import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class EmailInvoiceRequest {
    private String action;
    
    @JsonProperty("record_id")
    private Long recordId;
    
    @JsonProperty("record_type")
    private String recordType;
    
    private List<String> emails;
    
    // For T-Invoice applicants
    // Frontend sends JSON string for applicants in PHP version, we will change to Object list
    private List<ApplicantDto> applicants; 
    
    @JsonProperty("invoice_number")
    private String invoiceNumber;
    
    @JsonProperty("customer_name")
    private String customerName;
    
    @JsonProperty("customer_email")
    private String customerEmail;
    
    @JsonProperty("customer_address")
    private String customerAddress;
    
    // Changed from String to List
    @JsonProperty("invoice_items")
    private List<EmailItemDto> invoiceItems; 
    
    private String subtotal;
    
    @JsonProperty("discount_amount")
    private String discountAmount;
    
    @JsonProperty("discount_percent")
    private String discountPercent;
    
    private String total;
    private String bcc;
    private String subject;
    
    @Data
    public static class ApplicantDto {
        private String name;
        private String email;
        private String type;
    }

    @Data
    public static class EmailItemDto {
        private String name;
        private String packageName; // mapped from 'package' ? need custom mapping if key is 'package'
        
        @JsonProperty("package")
        private String package_; // handle reserved keyword
        
        @JsonProperty("visa_type")
        private String visaType;
        
        private String country;
        private String price;
        private String type;
    }
}
