package uk.co.visad.service;

import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.visad.entity.BiometricCredential;
import uk.co.visad.entity.User;
import uk.co.visad.repository.BiometricCredentialRepository;
import uk.co.visad.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BiometricService {

    private final BiometricCredentialRepository biometricRepository;
    private final UserRepository userRepository;

    // In a real app, strict origin checking is required.
    // For localhost dev, we might be lenient or configurable.
    private static final String RP_ID = "localhost";
    private static final String RP_NAME = "VISAD VAULT";

    public Challenge generateChallenge() {
        return new DefaultChallenge();
    }

    public String getRpId() {
        return RP_ID;
    }

    public String getRpName() {
        return RP_NAME;
    }

    @Transactional
    public void registerCredential(User user, String credentialId, String publicKey, String aaguid, Long counter) {
        BiometricCredential credential = BiometricCredential.builder()
                .user(user)
                .credentialId(credentialId)
                .publicKey(publicKey)
                .counter(counter)
                .aaguid(aaguid)
                .createdAt(LocalDateTime.now())
                .build();
        biometricRepository.save(credential);
    }

    public List<BiometricCredential> getUserCredentials(User user) {
        return biometricRepository.findAllByUser(user);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Additional helper methods for WebAuthn verification would go here.
    // Due to complexity, full WebAuthn4J implementation often involves DTO mapping
    // which we will handle in Controller.
}
