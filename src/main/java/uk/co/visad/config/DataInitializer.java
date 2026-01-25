package uk.co.visad.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uk.co.visad.entity.User;
import uk.co.visad.repository.UserRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking for admin user...");
        String adminUsername = "admin";

        userRepository.findByUsername(adminUsername).ifPresentOrElse(
                user -> {
                    boolean changed = false;
                    if (!"admin".equals(user.getRole())) {
                        log.info("Admin user found but has wrong role [{}]. Updating to 'admin'.", user.getRole());
                        user.setRole("admin");
                        changed = true;
                    }
                    // Reset password if needed (optional, keeping it simple for now to just role)
                    // If you wanted to reset password to admin123, you could do it here.

                    if (changed) {
                        userRepository.save(user);
                        log.info("Admin user updated.");
                    } else {
                        log.info("Admin user already exists with correct role.");
                    }
                },
                () -> {
                    log.info("Admin user not found. Creating default admin.");
                    User admin = User.builder()
                            .username(adminUsername)
                            .password(passwordEncoder.encode("admin123")) // Default password
                            .role("admin")
                            .build();
                    userRepository.save(admin);
                    log.info("Admin user created successfully.");
                });
    }
}
