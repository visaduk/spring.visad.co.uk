package uk.co.visad.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uk.co.visad.dto.ApiResponse;
import uk.co.visad.dto.VisaUrlDto;
import uk.co.visad.service.VisaUrlService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/urls")
@RequiredArgsConstructor
// @PreAuthorize("hasRole('ADMIN')")
public class VisaUrlController {

    private final VisaUrlService visaUrlService;

    /**
     * Get all visa URLs
     * PHP equivalent: urls.php?action=read_all
     */
    @GetMapping("")
    public ResponseEntity<ApiResponse<List<VisaUrlDto>>> readAllUrls() {
        List<VisaUrlDto> urls = visaUrlService.getAllUrls();
        return ResponseEntity.ok(ApiResponse.success(urls));
    }

    /**
     * Create a new visa URL
     * PHP equivalent: urls.php?action=create
     */
    @PostMapping("")
    public ResponseEntity<ApiResponse<Void>> createUrl(
            @RequestParam String country,
            @RequestParam(required = false) String visa_center,
            @RequestParam String url,
            @RequestParam(required = false) String application_form_url,
            @RequestParam(required = false) MultipartFile application_form_file) throws IOException {

        VisaUrlDto.CreateRequest request = new VisaUrlDto.CreateRequest(
                country, visa_center, url, application_form_url);
        visaUrlService.createUrl(request, application_form_file);
        return ResponseEntity.ok(ApiResponse.successMessage("URL created successfully"));
    }

    /**
     * Update a visa URL
     * PHP equivalent: urls.php?action=update
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateUrl(
            @PathVariable Long id,
            @RequestParam String country,
            @RequestParam(required = false) String visa_center,
            @RequestParam String url,
            @RequestParam(required = false) String application_form_url,
            @RequestParam(required = false) MultipartFile application_form_file) throws IOException {

        VisaUrlDto.UpdateRequest request = new VisaUrlDto.UpdateRequest(
                id, country, visa_center, url, application_form_url);
        visaUrlService.updateUrl(request, application_form_file);
        return ResponseEntity.ok(ApiResponse.successMessage("URL updated successfully"));
    }

    /**
     * Delete a visa URL
     * PHP equivalent: urls.php?action=delete
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUrl(@PathVariable Long id) {
        visaUrlService.deleteUrl(id);
        return ResponseEntity.ok(ApiResponse.successMessage("URL deleted successfully"));
    }
}
