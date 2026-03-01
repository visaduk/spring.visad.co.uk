package uk.co.visad.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.co.visad.dto.ApiResponse;
import uk.co.visad.util.FileEncryptionUtil;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Admin-only endpoint to encrypt all existing plaintext files in vault_uploads.
 *
 * Skips:
 *  - Files already encrypted (VISADENC header)
 *  - The forms/ subdirectory (served as static resources, must remain plaintext)
 *
 * POST /api/admin/encrypt-files
 * Requires a valid JWT (admin login).
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class FileEncryptionMigrationController {

    @Value("${app.upload.root:/home/VisaD/visad.co.uk/vault_uploads}")
    private String uploadRoot;

    @Autowired(required = false)
    private FileEncryptionUtil encryptionUtil;

    @PostMapping("/encrypt-files")
    public ResponseEntity<ApiResponse<EncryptionResult>> encryptAll() {
        if (encryptionUtil == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Encryption is not configured. Set APP_ENCRYPTION_ENABLED=true and APP_ENCRYPTION_KEY."));
        }

        EncryptionResult result = new EncryptionResult();
        Path root = Paths.get(uploadRoot).normalize();
        Path formsDir = root.resolve("forms");

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    // Skip the forms/ directory — served as static resources
                    if (file.startsWith(formsDir)) {
                        return FileVisitResult.CONTINUE;
                    }

                    try {
                        byte[] bytes = Files.readAllBytes(file);
                        if (encryptionUtil.isEncrypted(bytes)) {
                            result.alreadyEncrypted++;
                            return FileVisitResult.CONTINUE;
                        }

                        byte[] encrypted = encryptionUtil.encrypt(bytes);

                        // Atomic write: temp file + rename
                        Path tmp = file.resolveSibling(file.getFileName() + ".enc_tmp");
                        Files.write(tmp, encrypted);
                        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

                        result.encrypted++;
                        log.info("Encrypted: {}", root.relativize(file));
                    } catch (GeneralSecurityException | IOException e) {
                        result.failures.add(root.relativize(file).toString() + ": " + e.getMessage());
                        log.error("Failed to encrypt: {}", file, e);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    result.failures.add(file.toString() + ": " + exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Migration failed: " + e.getMessage()));
        }

        log.info("Encryption migration complete. encrypted={}, skipped={}, failures={}",
                result.encrypted, result.alreadyEncrypted, result.failures.size());

        return ResponseEntity.ok(ApiResponse.success(result,
                "Migration complete. Encrypted=" + result.encrypted
                + ", AlreadyEncrypted=" + result.alreadyEncrypted
                + ", Failures=" + result.failures.size()));
    }

    public static class EncryptionResult {
        public int encrypted = 0;
        public int alreadyEncrypted = 0;
        public List<String> failures = new ArrayList<>();
    }
}
