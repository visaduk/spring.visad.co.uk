package uk.co.visad.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvoiceSaveRequestDto {
    private BigDecimal subtotal;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private BigDecimal total;
    private String itemsJson;
}
