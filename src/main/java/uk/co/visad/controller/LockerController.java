package uk.co.visad.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uk.co.visad.dto.ApiResponse;
import uk.co.visad.dto.locker.LockerDtos.*;
import uk.co.visad.entity.LockerActivity;
import uk.co.visad.exception.ResourceNotFoundException;
import uk.co.visad.exception.UnauthorizedException;
import uk.co.visad.service.FileUploadService;
import uk.co.visad.service.LockerActivityService;
import uk.co.visad.service.LockerService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Locker System (Public Portal) Controller
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4545", allowCredentials = "true")
public class LockerController {

    private final LockerService lockerService;
    private final FileUploadService fileUploadService;
    private final LockerActivityService lockerActivityService;

    /**
     * Verify token and password
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<ApplicantDataDTO>> verifyToken(@Valid @RequestBody VerificationRequest request) {
        log.info("Token verification requested: {}", request.getToken());

        try {
            ApplicantDataDTO data = lockerService.verifyAndGetData(
                    request.getToken(),
                    request.getPassword());

            // Fix: generic types matching
            return ResponseEntity.ok(ApiResponse.success(data, "Verification successful"));
        } catch (UnauthorizedException e) {
            log.warn("Verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Verification error", e);
            throw e;
        }
    }

    /**
     * Update personal information
     */
    @PostMapping("/update_personal")
    public ResponseEntity<ApiResponse<Void>> updatePersonal(@Valid @RequestBody UpdatePersonalRequest request) {
        log.info("Update personal info: token={}, field={}", request.getToken(), request.getField());

        lockerService.updatePersonalField(
                request.getToken(),
                request.getField(),
                request.getValue());

        return ResponseEntity.ok(ApiResponse.successMessage("Personal info updated"));
    }

    /**
     * Update questions data
     */
    @PostMapping("/update_questions")
    public ResponseEntity<ApiResponse<Void>> updateQuestions(@Valid @RequestBody UpdateQuestionsRequest request) {
        log.info("Update questions: token={}, fields={}", request.getToken(), request.getData().keySet());

        lockerService.updateQuestionFields(
                request.getToken(),
                request.getData());

        return ResponseEntity.ok(ApiResponse.successMessage("Questions updated"));
    }

    /**
     * Upload files
     */
    @PostMapping("/upload_files")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFiles(
            @RequestParam("token") String token,
            @RequestParam(value = "input_name", required = false) String inputName,
            @RequestParam("db_field") String dbField,
            @RequestParam("files") MultipartFile[] files) {

        log.info("File upload: token={}, field={}, count={}", token, dbField, files.length);

        FileUploadResponse response = fileUploadService.uploadFiles(
                token, dbField, files);

        return ResponseEntity.ok(ApiResponse.success(response, "Files uploaded"));
    }

    /**
     * Delete file
     */
    @PostMapping("/delete_file")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteFile(@Valid @RequestBody DeleteFileRequest request) {
        log.info("Delete file: token={}, field={}, file={}", request.getToken(), request.getDbField(),
                request.getFilename());

        List<String> remainingFiles = fileUploadService.deleteFile(
                request.getToken(),
                request.getDbField(),
                request.getFilename());

        // Use Map<String, Object> to satisfy generic <Map<String, Object>>
        Map<String, Object> data = Map.of("remaining_files", remainingFiles);
        return ResponseEntity.ok(ApiResponse.success(data, "File deleted"));
    }

    /**
     * Download file
     */
    @GetMapping("/download_file")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam("token") String token,
            @RequestParam("file") String filename) {
        log.info("Download file requested: token={}, file={}", token, filename);

        Resource resource = fileUploadService.getFileAsResource(token, filename);

        // Try to determine content type
        String contentType = "application/octet-stream";
        try {
            // Basic detection based on extension
            if (filename.toLowerCase().endsWith(".pdf"))
                contentType = "application/pdf";
            else if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg"))
                contentType = "image/jpeg";
            else if (filename.toLowerCase().endsWith(".png"))
                contentType = "image/png";
        } catch (Exception e) {
            log.warn("Could not determine file type");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * Update progress
     */
    @PostMapping("/update_progress")
    public ResponseEntity<ApiResponse<Void>> updateProgress(@Valid @RequestBody UpdateProgressRequest request) {
        log.info("Update progress: token={}, percentage={}", request.getToken(), request.getPercentage());

        lockerService.updateProgress(request.getToken(), request.getPercentage());

        return ResponseEntity.ok(ApiResponse.successMessage("Progress updated"));
    }

    /**
     * Mark application as complete
     */
    @PostMapping("/mark_complete")
    public ResponseEntity<ApiResponse<Void>> markComplete(@Valid @RequestBody TokenRequest request) {
        log.info("Mark complete: token={}", request.getToken());

        lockerService.markApplicationComplete(request.getToken());

        return ResponseEntity.ok(ApiResponse.successMessage("Application submitted"));
    }

    /**
     * Get recent locker activities for admin notification history (authenticated)
     */
    @GetMapping("/recent_activities")
    public ResponseEntity<ApiResponse<java.util.List<LockerActivity>>> recentActivities() {
        return ResponseEntity.ok(ApiResponse.success(lockerActivityService.getRecent()));
    }

    /**
     * Get dependent token
     */
    @PostMapping("/get_dependent_token")
    public ResponseEntity<ApiResponse<DependentDataDTO>> getDependentToken(
            @Valid @RequestBody DependentTokenRequest request) {
        log.info("Get dependent token: dependent_id={}", request.getDependentId());

        DependentDataDTO data = lockerService.getDependentData(request.getDependentId());

        return ResponseEntity.ok(ApiResponse.success(data, "Dependent data retrieved"));
    }
}
