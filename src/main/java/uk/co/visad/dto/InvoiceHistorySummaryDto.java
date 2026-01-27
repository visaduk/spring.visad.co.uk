package uk.co.visad.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvoiceHistorySummaryDto {
    private String lastSentInvoice;  // Formatted date string or null
    private String lastSentTInvoice; // Formatted date string or null
}
