package uk.co.visad.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uk.co.visad.dto.ApiResponse;
import uk.co.visad.dto.AuthDto;
import uk.co.visad.service.AuthService;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {
        AuthDto.LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/check-session")
    public ResponseEntity<ApiResponse<AuthDto.SessionResponse>> checkSession() {
        AuthDto.SessionResponse response = authService.checkSession();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // JWT is stateless, so logout is handled client-side by removing the token
        return ResponseEntity.ok(ApiResponse.successMessage("Logged out successfully"));
    }

    @PostMapping("/users")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> createUser(
            @Valid @RequestBody AuthDto.CreateUserRequest request) {
        authService.createUser(request);
        return ResponseEntity.ok(ApiResponse.successMessage("User created successfully."));
    }

    @GetMapping("/users")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AuthDto.UserDto>>> readUsers() {
        List<AuthDto.UserDto> users = authService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @DeleteMapping("/users/{id}")
    // @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable("id") Long userId) {
        authService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.successMessage("User deleted successfully"));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Void>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.successMessage("System is healthy"));
    }
}
