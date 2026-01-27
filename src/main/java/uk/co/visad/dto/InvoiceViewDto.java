package uk.co.visad.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class InvoiceViewDto {
    private String invoiceNumber;
    private String invoiceDate;
    private String dueDate;
    private InvoiceCustomerDto customer;
    private List<InvoiceItemDto> items;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private String discountLabel; // e.g. "Discount (10%)"
    private BigDecimal total;
    private String status;       // "Paid" or "Unpaid" etc
    private String paymentStatus;
    private InvoiceHistorySummaryDto history;
}
