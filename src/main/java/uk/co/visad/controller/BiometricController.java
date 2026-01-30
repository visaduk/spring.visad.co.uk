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
    private final uk.co.visad.security.JwtUtils jwtUtils;

    // --- Registration ---

    @PostMapping("/register/start")
    public ResponseEntity<?> startRegistration(@RequestBody Map<String, String> request,
            jakarta.servlet.http.HttpSession session) {
        String username = request.get("username");
        User user = biometricService.getUserByUsername(username);

        Challenge challenge = biometricService.generateChallenge();
        session.setAttribute("REG_CHALLENGE", challenge);
        session.setAttribute("REG_USERNAME", username);

        Map<String, Object> response = new HashMap<>();
        response.put("challenge", Base64.getUrlEncoder().withoutPadding().encodeToString(challenge.getValue()));
        response.put("userId",
                Base64.getUrlEncoder().withoutPadding().encodeToString(String.valueOf(user.getId()).getBytes()));
        response.put("rpId", biometricService.getRpId());
        response.put("rpName", biometricService.getRpName());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/finish")
    public ResponseEntity<?> finishRegistration(@RequestBody Map<String, Object> request,
            jakarta.servlet.http.HttpSession session) {
        try {
            Challenge challenge = (Challenge) session.getAttribute("REG_CHALLENGE");
            String username = (String) session.getAttribute("REG_USERNAME");

            if (challenge == null || username == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "error", "message", "Registration session expired"));
            }

            User user = biometricService.getUserByUsername(username);

            // Extract data from the nested JSON structure sent by frontend
            // { id, rawId, type, response: { attestationObject, clientDataJSON } }
            @SuppressWarnings("unchecked")
            Map<String, String> responseData = (Map<String, String>) request.get("response");
            String attestationObject = responseData.get("attestationObject");
            String clientDataJSON = responseData.get("clientDataJSON");

            biometricService.completeRegistration(user, clientDataJSON, attestationObject, challenge);

            session.removeAttribute("REG_CHALLENGE");
            session.removeAttribute("REG_USERNAME");

            return ResponseEntity.ok(Map.of("status", "success", "message", "Biometric registered"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Verification failed: " + e.getMessage()));
        }
    }

    // --- Authentication ---

    @PostMapping("/login/start")
    public ResponseEntity<?> startLogin(@RequestBody Map<String, String> request,
            jakarta.servlet.http.HttpSession session) {
        String username = request.get("username");
        biometricService.getUserByUsername(username); // Check user exists

        Challenge challenge = biometricService.generateChallenge();
        session.setAttribute("AUTH_CHALLENGE", challenge);
        session.setAttribute("AUTH_USERNAME", username);

        Map<String, Object> response = new HashMap<>();
        response.put("challenge", Base64.getUrlEncoder().withoutPadding().encodeToString(challenge.getValue()));
        response.put("rpId", biometricService.getRpId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/finish")
    public ResponseEntity<?> finishLogin(@RequestBody Map<String, Object> request,
            jakarta.servlet.http.HttpSession session) {
        try {
            Challenge challenge = (Challenge) session.getAttribute("AUTH_CHALLENGE");

            if (challenge == null) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Login session expired"));
            }

            String credentialId = (String) request.get("id");
            @SuppressWarnings("unchecked")
            Map<String, String> responseData = (Map<String, String>) request.get("response");
            String authenticatorData = responseData.get("authenticatorData");
            String clientDataJSON = responseData.get("clientDataJSON");
            String signature = responseData.get("signature");
            String userHandle = responseData.get("userHandle");

            User user = biometricService.completeLogin(credentialId, clientDataJSON, authenticatorData, signature,
                    userHandle, challenge);

            session.removeAttribute("AUTH_CHALLENGE");
            session.removeAttribute("AUTH_USERNAME");

            // Generate real JWT token
            String token = jwtUtils.generateTokenFromUsername(user.getUsername(), user.getId(), user.getRole());

            return ResponseEntity.ok(Map.of("status", "success", "token", token, "username", user.getUsername(), "role",
                    user.getRole()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(401)
                    .body(Map.of("status", "error", "message", "Authentication failed: " + e.getMessage()));
        }
    }
}
