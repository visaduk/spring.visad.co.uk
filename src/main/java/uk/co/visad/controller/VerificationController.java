package uk.co.visad.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.co.visad.dto.ApiResponse;
import uk.co.visad.service.VerificationService;

@RestController
@RequestMapping("/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @GetMapping("/view")
    public ResponseEntity<ApiResponse<Object>> viewVerification(@RequestParam String token) {
        Object data = verificationService.verifyToken(token);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
