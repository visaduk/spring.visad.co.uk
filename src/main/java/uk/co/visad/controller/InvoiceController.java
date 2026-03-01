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

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<InvoiceHistory>>> getHistory(
            @PathVariable Long id,
            @RequestParam("type") String recordType) {
        List<InvoiceHistory> history = invoiceService.getHistory(id);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/get_history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHistoryByTravelerId(
            @RequestParam("traveler_id") Long travelerId) {
        List<InvoiceHistory> history = invoiceService.getHistory(travelerId);
        String lastInv = history.stream()
                .filter(h -> "invoice".equals(h.getInvoiceType()))
                .findFirst()
                .map(h -> h.getSentAt() != null ? h.getSentAt().toString() : null)
                .orElse(null);
        String lastTInv = history.stream()
                .filter(h -> "t-invoice".equals(h.getInvoiceType()))
                .findFirst()
                .map(h -> h.getSentAt() != null ? h.getSentAt().toString() : null)
                .orElse(null);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("last_sent_invoice", lastInv);
        result.put("last_sent_t_invoice", lastTInv);
        return ResponseEntity.ok(ApiResponse.success(result));
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
