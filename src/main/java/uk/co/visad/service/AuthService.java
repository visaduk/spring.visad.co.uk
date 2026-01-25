package uk.co.visad.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.visad.dto.AuthDto;
import uk.co.visad.entity.User;
import uk.co.visad.exception.BadRequestException;
import uk.co.visad.exception.ResourceNotFoundException;
import uk.co.visad.repository.UserRepository;
import uk.co.visad.security.JwtUtils;
import uk.co.visad.security.UserPrincipal;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return AuthDto.LoginResponse.builder()
                .token(jwt)
                .role(userPrincipal.getRole())
                .username(userPrincipal.getUsername())
                .userId(userPrincipal.getId())
                .build();
    }

    public AuthDto.SessionResponse checkSession() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            return AuthDto.SessionResponse.builder()
                    .loggedin(true)
                    .role(principal.getRole())
                    .username(principal.getUsername())
                    .userId(principal.getId())
                    .build();
        }

        return AuthDto.SessionResponse.builder()
                .loggedin(false)
                .build();
    }

    @Transactional
    public void createUser(AuthDto.CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        String role = request.getRole();
        if (role == null || (!role.equals("user") && !role.equals("admin"))) {
            role = "user";
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<AuthDto.UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> AuthDto.UserDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .role(user.getRole())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUser(Long userId) {
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            if (principal.getId().equals(userId)) {
                throw new BadRequestException("You cannot delete yourself");
            }
        }

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        userRepository.deleteById(userId);
    }
}
