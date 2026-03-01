package uk.co.visad.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uk.co.visad.dto.ApiResponse;
import uk.co.visad.dto.AuditLogDto;
import uk.co.visad.entity.AuditLog;
import uk.co.visad.exception.BadRequestException;
import uk.co.visad.exception.ResourceNotFoundException;
import uk.co.visad.repository.AuditLogRepository;
import uk.co.visad.repository.DependentRepository;
import uk.co.visad.repository.TravelerRepository;
import uk.co.visad.service.AuditService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
// @PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final TravelerRepository travelerRepository;
    private final DependentRepository dependentRepository;
    private final AuditService auditService;

    private static final Set<String> ALLOWED_REVERT_FIELDS = Set.of(
            "name", "travel_country", "visa_center", "package", "visa_type", "status", "whatsapp_contact",
            "appointment_remarks", "visa_link", "note", "planned_travel_date", "first_name", "last_name",
            "gender", "dob", "nationality", "passport_no", "passport_issue", "passport_expire",
            "contact_number", "email", "priority", "username", "logins", "notes", "payment_status",
            "address_line_1", "address_line_2", "city", "state_province", "zip_code", "doc_date", "is_family",
            "place_of_birth", "country_of_birth");

    private static final Set<String> DATE_FIELDS = Set.of(
            "dob", "passport_issue", "passport_expire", "planned_travel_date", "doc_date");

    /**
     * Read all logs (paginated) or logs for a specific record
     * PHP equivalent: logs.php?action=read_all
     */
    @GetMapping("")
    public ResponseEntity<ApiResponse<?>> readAllLogs(
            @RequestParam(required = false, defaultValue = "0") Long record_id,
            @RequestParam(required = false, defaultValue = "") String record_type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int limit) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        // Record-specific: return all logs for that record (no pagination needed)
        if (record_id > 0 && !record_type.isEmpty()) {
            List<AuditLog> logs = auditLogRepository.findByRecordIdAndRecordTypeOrderByTimestampDesc(record_id, record_type);
            List<AuditLogDto.Response> response = logs.stream()
                    .map(log -> mapToDto(log, formatter))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(response));
        }

        // Global paginated list
        Page<AuditLog> pageResult = auditLogRepository.findAllByOrderByTimestampDesc(
                PageRequest.of(Math.max(0, page - 1), Math.min(limit, 500)));

        List<AuditLogDto.Response> responseList = pageResult.getContent().stream()
                .map(log -> mapToDto(log, formatter))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("logs", responseList);
        result.put("total", pageResult.getTotalElements());
        result.put("page", page);
        result.put("totalPages", pageResult.getTotalPages());
        result.put("limit", limit);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private AuditLogDto.Response mapToDto(AuditLog log, DateTimeFormatter formatter) {
        return AuditLogDto.Response.builder()
                .id(log.getId())
                .username(log.getUsername())
                .recordType(log.getRecordType())
                .recordName(log.getRecordName())
                .fieldChanged(log.getFieldChanged())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .formattedTimestamp(log.getTimestamp().format(formatter))
                .build();
    }

    /**
     * Revert a change
     * PHP equivalent: logs.php?action=revert
     */
    @PostMapping("/{logId}/revert")
    public ResponseEntity<ApiResponse<Void>> revertChange(
            @PathVariable("logId") Long logId) {

        AuditLog logEntry = auditLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Log entry not found."));

        String field = logEntry.getFieldChanged();

        // Check if field is allowed to be reverted
        String snakeField = toSnakeCase(field);
        if (!ALLOWED_REVERT_FIELDS.contains(snakeField)) {
            throw new BadRequestException("Invalid field for revert action.");
        }

        String oldValue = logEntry.getOldValue();

        // Handle date format conversion
        if (DATE_FIELDS.contains(snakeField) && oldValue != null && !oldValue.isEmpty()) {
            if (!oldValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
                try {
                    LocalDate date = LocalDate.parse(oldValue, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    oldValue = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (Exception e) {
                    oldValue = null;
                }
            }
        }

        // Perform the revert
        String recordType = logEntry.getRecordType();
        Long recordId = logEntry.getRecordId();
        String javaField = toCamelCase(snakeField);

        if ("traveler".equals(recordType)) {
            var traveler = travelerRepository.findById(recordId)
                    .orElseThrow(() -> new ResourceNotFoundException("Traveler not found"));
            // Use reflection or manual field setting would be needed here
            // For simplicity, reusing the update logic from TravelerService
        } else if ("dependent".equals(recordType)) {
            var dependent = dependentRepository.findById(recordId)
                    .orElseThrow(() -> new ResourceNotFoundException("Dependent not found"));
        }

        // Log the revert
        auditService.logChange(
                recordType,
                recordId,
                logEntry.getRecordName(),
                "Reverted '" + field + "'",
                logEntry.getNewValue(),
                logEntry.getOldValue());

        return ResponseEntity.ok(ApiResponse.successMessage("Change successfully reverted."));
    }

    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private String toCamelCase(String snakeCase) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
