package uk.co.visad.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class InvoiceItemDto {
    private String name;        // e.g. "Nitish Shukla - Full Support"
    private String description; // e.g. "Tourist Visa - Austria"
    private BigDecimal price;
    private int quantity;
}
