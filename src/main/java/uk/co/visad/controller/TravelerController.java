package uk.co.visad.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.co.visad.dto.ApiResponse;
import uk.co.visad.dto.TravelerDto;
import uk.co.visad.service.TravelerService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/travelers")
@RequiredArgsConstructor
public class TravelerController {

    private final TravelerService travelerService;

    /**
     * Create a new traveler
     * PHP equivalent: travelers.php?action=create
     */
    @PostMapping("")
    public ResponseEntity<ApiResponse<Map<String, Long>>> createTraveler() {
        Long id = travelerService.createTraveler();
        return ResponseEntity.ok(ApiResponse.success(Map.of("id", id)));
    }

    /**
     * Get all travelers with pagination
     * PHP equivalent: travelers.php?action=read_all
     */
    @GetMapping("")
    public ResponseEntity<ApiResponse<List<TravelerDto>>> readAllTravelers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "false") boolean summary) {
        return ResponseEntity.ok(travelerService.getAllTravelers(page, limit, summary));
    }

    /**
     * Get a single traveler by ID
     * PHP equivalent: travelers.php?action=read_one
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TravelerDto>> readOneTraveler(@PathVariable Long id) {
        TravelerDto traveler = travelerService.getTravelerById(id);
        return ResponseEntity.ok(ApiResponse.success(traveler));
    }

    /**
     * Update a single field
     * PHP equivalent: travelers.php?action=update_field
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateField(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String field = payload.get("field");
        String value = payload.getOrDefault("value", "");
        travelerService.updateField(id, field, value);
        return ResponseEntity.ok(ApiResponse.successMessage("Field updated successfully"));
    }

    /**
     * Update multiple fields at once
     * Request body: { "updates": { "field1": "value1", "field2": "value2" } }
     */
    @PatchMapping("/{id}/bulk")
    public ResponseEntity<ApiResponse<Void>> updateFields(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> updates = (Map<String, Object>) payload.get("updates");
        travelerService.updateFields(id, updates);
        return ResponseEntity.ok(ApiResponse.successMessage("Fields updated successfully"));
    }

    /**
     * Delete a traveler
     * PHP equivalent: travelers.php?action=delete
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTraveler(@PathVariable Long id) {
        travelerService.deleteTraveler(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Traveler deleted successfully"));
    }

    /**
     * Find by passport number
     * PHP equivalent: travelers.php?action=find_by_passport
     */
    @GetMapping("/find-by-passport")
    public ResponseEntity<ApiResponse<TravelerDto>> findByPassport(
            @RequestParam("passport_no") String passportNo) {
        TravelerDto traveler = travelerService.findByPassport(passportNo);
        if (traveler != null) {
            return ResponseEntity.ok(ApiResponse.success(traveler));
        }
        return ResponseEntity.ok(ApiResponse.<TravelerDto>builder()
                .status("not_found")
                .build());
    }

    /**
     * Get form data for traveler
     * PHP equivalent: travelers.php?action=get_form_data
     */
    @GetMapping("/get_form_data")
    public ResponseEntity<ApiResponse<TravelerDto>> getFormData(@RequestParam Long id) {
        TravelerDto traveler = travelerService.getTravelerById(id);
        return ResponseEntity.ok(ApiResponse.success(traveler));
    }

    /**
     * Set lock status
     * PHP equivalent: travelers.php?action=set_lock_status
     */
    @PostMapping("/{id}/lock-status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> setLockStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> payload) {
        int locked = payload.getOrDefault("locked", 0);
        travelerService.setLockStatus(id, locked == 1);
        return ResponseEntity.ok(ApiResponse.success(Map.of("locked", locked == 1)));
    }

    /**
     * Get full traveler data
     * PHP equivalent: travelers.php?action=get_full_data
     */
    @GetMapping("/get_full_data")
    public ResponseEntity<ApiResponse<TravelerDto>> getFullData(@RequestParam Long id) {
        TravelerDto traveler = travelerService.getTravelerById(id);
        return ResponseEntity.ok(ApiResponse.success(traveler));
    }

    /**
     * Save invoice
     * PHP equivalent: travelers.php?action=save_invoice
     */
    @PostMapping("/save_invoice")
    public ResponseEntity<ApiResponse<TravelerDto.InvoiceDto>> saveInvoice(
            @RequestBody TravelerDto.SaveInvoiceRequest request) {
        TravelerDto.InvoiceDto invoice = travelerService.saveInvoice(request);
        return ResponseEntity.ok(ApiResponse.success(invoice, "Invoice saved"));
    }

    /**
     * Get invoice
     * PHP equivalent: travelers.php?action=get_invoice
     */
    @GetMapping("/get_invoice")
    public ResponseEntity<ApiResponse<TravelerDto.InvoiceDto>> getInvoice(
            @RequestParam("traveler_id") Long travelerId) {
        TravelerDto.InvoiceDto invoice = travelerService.getInvoice(travelerId);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @PatchMapping("/{id}/questions")
    public ResponseEntity<ApiResponse<Void>> updateQuestionField(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String field = payload.get("field");
        String value = payload.getOrDefault("value", "");
        travelerService.updateQuestionField(id, field, value);
        return ResponseEntity.ok(ApiResponse.successMessage("Question field updated successfully"));
    }

    @DeleteMapping("/{id}/files")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable Long id,
            @RequestParam String field) {
        travelerService.deleteQuestionFile(id, field);
        return ResponseEntity.ok(ApiResponse.successMessage("File deleted successfully"));
    }
}
