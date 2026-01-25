package uk.co.visad.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.co.visad.dto.ApiResponse;
import uk.co.visad.entity.InvoiceHistory;
import uk.co.visad.service.InvoiceService;

import java.util.List;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/get_history")
    public ResponseEntity<ApiResponse<List<InvoiceHistory>>> getHistory(
            @RequestParam("traveler_id") Long travelerId) {
        List<InvoiceHistory> history = invoiceService.getHistory(travelerId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
