package uk.co.visad.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.co.visad.dto.ApiResponse;
import uk.co.visad.dto.DependentDto;
import uk.co.visad.service.DependentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dependents")
@RequiredArgsConstructor
public class DependentController {

    private final DependentService dependentService;

    /**
     * Create a new dependent (co-traveler)
     * PHP equivalent: dependents.php?action=create
     */
    @PostMapping("")
    public ResponseEntity<ApiResponse<Map<String, Long>>> createDependent(
            @RequestParam("traveler_id") Long travelerId) {
        Long id = dependentService.createDependent(travelerId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("id", id)));
    }

    /**
     * Delete a dependent
     * PHP equivalent: dependents.php?action=delete
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDependent(@PathVariable Long id) {
        dependentService.deleteDependent(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Co-traveler deleted successfully"));
    }

    /**
     * Update a single field
     * PHP equivalent: dependents.php?action=update_field
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateField(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String field = payload.get("field");
        String value = payload.getOrDefault("value", "");
        dependentService.updateField(id, field, value);
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
        dependentService.updateFields(id, updates);
        return ResponseEntity.ok(ApiResponse.successMessage("Fields updated successfully"));
    }

    /**
     * Get form data for dependent
     * PHP equivalent: dependents.php?action=get_form_data
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DependentDto>> getDependent(@PathVariable Long id) {
        DependentDto dependent = dependentService.getDependentById(id);
        return ResponseEntity.ok(ApiResponse.success(dependent));
    }

    /**
     * Set lock status
     * PHP equivalent: dependents.php?action=set_lock_status
     */
    @PostMapping("/{id}/lock-status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> setLockStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> payload) {
        int locked = payload.getOrDefault("locked", 0);
        dependentService.setLockStatus(id, locked == 1);
        return ResponseEntity.ok(ApiResponse.success(Map.of("locked", locked == 1)));
    }

    /**
     * Get full dependent data
     * PHP equivalent: dependents.php?action=get_full_data
     */

    /**
     * Get all dependents for a traveler
     * PHP equivalent: dependents.php?action=get_all_for_traveler
     */
    @GetMapping("")
    public ResponseEntity<ApiResponse<List<DependentDto>>> getAllForTraveler(
            @RequestParam("traveler_id") Long travelerId) {
        List<DependentDto> dependents = dependentService.getDependentsForTraveler(travelerId);
        return ResponseEntity.ok(ApiResponse.<List<DependentDto>>builder()
                .status("success")
                .data(dependents)
                .build());
    }

    @PatchMapping("/{id}/questions")
    public ResponseEntity<ApiResponse<Void>> updateQuestionField(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String field = payload.get("field");
        String value = payload.getOrDefault("value", "");
        dependentService.updateQuestionField(id, field, value);
        return ResponseEntity.ok(ApiResponse.successMessage("Question field updated successfully"));
    }
}
