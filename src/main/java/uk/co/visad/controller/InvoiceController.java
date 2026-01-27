package uk.co.visad.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.co.visad.dto.ApiResponse;
import uk.co.visad.entity.InvoiceHistory;
import uk.co.visad.service.InvoiceService;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/{recordType}/{id}")
    public ResponseEntity<ApiResponse<uk.co.visad.dto.InvoiceViewDto>> getInvoiceView(
            @PathVariable String recordType,
            @PathVariable Long id) {
        uk.co.visad.dto.InvoiceViewDto dto = invoiceService.generateInvoiceView(recordType, id);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
    
    @PostMapping("/{id}/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveInvoice(
            @PathVariable Long id,
            @RequestBody uk.co.visad.dto.InvoiceSaveRequestDto request) {
        invoiceService.saveInvoice(id, request);
        
        // Return simulated PHP response structure if needed by frontend
        // Frontend expects: { status: success, data: { invoice_id, invoice_number } }
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "invoice_id", id,
            "invoice_number", "INV-" + String.format("%04d", id)
        )));
    }
}
