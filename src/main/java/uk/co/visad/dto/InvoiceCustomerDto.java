package uk.co.visad.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class InvoiceCustomerDto {
    private String name;
    private List<String> addressLines;
    private String email;
}
