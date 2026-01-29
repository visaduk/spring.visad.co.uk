package uk.co.visad.controller;

import com.webauthn4j.data.client.challenge.Challenge;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.co.visad.service.BiometricService;
import uk.co.visad.entity.User;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth/webauthn")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Adjust for production
public class BiometricController {

    private final BiometricService biometricService;

    // --- Registration ---

    @PostMapping("/register/start")
    public ResponseEntity<?> startRegistration(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        User user = biometricService.getUserByUsername(username);

        Challenge challenge = biometricService.generateChallenge();
        // Store challenge in session or cache (key=username) - simplified for demo:
        // returning directly
        // In prod, use Redis or HttpSession

        Map<String, Object> response = new HashMap<>();
        response.put("challenge", Base64.getUrlEncoder().withoutPadding().encodeToString(challenge.getValue()));
        response.put("userId",
                Base64.getUrlEncoder().withoutPadding().encodeToString(String.valueOf(user.getId()).getBytes()));
        response.put("rpId", biometricService.getRpId());
        response.put("rpName", biometricService.getRpName());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/finish")
    public ResponseEntity<?> finishRegistration(@RequestBody Map<String, Object> request) {
        // Parse client response
        // Verify attestation using biometricService
        // Save credential

        // This is a placeholder as full verification requires creating
        // RegistrationContext
        // and parsing the complex JSON structure from the browser.
        // For the purpose of this task, we assume success to unblock the frontend work
        // until the full parser is implemented.

        return ResponseEntity.ok(Map.of("status", "success", "message", "Biometric registered"));
    }

    // --- Authentication ---

    @PostMapping("/login/start")
    public ResponseEntity<?> startLogin(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        biometricService.getUserByUsername(username); // Check user exists

        Challenge challenge = biometricService.generateChallenge();

        Map<String, Object> response = new HashMap<>();
        response.put("challenge", Base64.getUrlEncoder().withoutPadding().encodeToString(challenge.getValue()));
        response.put("rpId", biometricService.getRpId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/finish")
    public ResponseEntity<?> finishLogin(@RequestBody Map<String, Object> request) {
        // Parse assertion
        // Verify signature
        // Return JWT token

        // Placeholder returning success
        return ResponseEntity.ok(Map.of("status", "success", "token", "dummy-jwt-token-for-biometric"));
    }
}
