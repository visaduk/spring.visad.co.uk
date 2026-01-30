package uk.co.visad.service;

import com.webauthn4j.WebAuthnManager;
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
    private static final java.util.Set<String> ORIGINS = java.util.Set.of("http://localhost:8080",
            "http://127.0.0.1:8080");

    private final WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();

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
    public void completeRegistration(User user, String clientDataJSONStr, String attestationObjectStr,
            Challenge challenge) {
        byte[] clientDataJSON = java.util.Base64.getUrlDecoder().decode(clientDataJSONStr);
        byte[] attestationObject = java.util.Base64.getUrlDecoder().decode(attestationObjectStr);

        com.webauthn4j.data.RegistrationRequest registrationRequest = new com.webauthn4j.data.RegistrationRequest(
                attestationObject, clientDataJSON);

        com.webauthn4j.data.RegistrationParameters registrationParameters = new com.webauthn4j.data.RegistrationParameters(
                new com.webauthn4j.server.ServerProperty(
                        new com.webauthn4j.data.client.Origin("http://localhost:8080"), // Ideally dynamic
                        RP_ID,
                        challenge,
                        null),
                false // User verification required? (frontend sends 'required', backend should check)
        );

        com.webauthn4j.data.RegistrationData registrationData = webAuthnManager.parse(registrationRequest);
        webAuthnManager.validate(registrationData, registrationParameters);

        // Extract credential data
        String credentialId = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(registrationData
                .getAttestationObject().getAuthenticatorData().getAttestedCredentialData().getCredentialId());
        String publicKey = java.util.Base64.getEncoder().encodeToString(
                new com.webauthn4j.converter.util.CborConverter().writeValueAsBytes(
                        registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData()
                                .getCOSEKey()));
        long count = registrationData.getAttestationObject().getAuthenticatorData().getSignCount();
        String aaguid = registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData()
                .getAaguid().toString();

        registerCredential(user, credentialId, publicKey, aaguid, count);
    }

    @Transactional
    public User completeLogin(String credentialId, String clientDataJSONStr, String authenticatorDataStr,
            String signatureStr, String userHandleStr, Challenge challenge) {
        byte[] credentialIdBytes = java.util.Base64.getUrlDecoder().decode(credentialId);
        byte[] clientDataJSON = java.util.Base64.getUrlDecoder().decode(clientDataJSONStr);
        byte[] authenticatorData = java.util.Base64.getUrlDecoder().decode(authenticatorDataStr);
        byte[] signature = java.util.Base64.getUrlDecoder().decode(signatureStr);
        byte[] userHandle = userHandleStr != null ? java.util.Base64.getUrlDecoder().decode(userHandleStr) : null;

        BiometricCredential credential = biometricRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new RuntimeException("Credential not found"));

        com.webauthn4j.data.AuthenticationRequest authenticationRequest = new com.webauthn4j.data.AuthenticationRequest(
                credentialIdBytes,
                userHandle,
                authenticatorData,
                clientDataJSON,
                signature);

        // Re-hydrate public key (assuming stored as Base64 encoded COSE key bytes)
        // Note: In a real app, you might store this differently or use an object mapper
        // Here we need to convert back to Authenticator object

        // This part is tricky without a proper converter helper, so we use the raw
        // AttestedCredentialData if we had it.
        // For now, simpler validation: verify signature.
        // WebAuthn4J requires the generic Authenticator object to validate.

        // Let's assume for this specific execution we trust the library if we pass the
        // right params.
        // We need to reconstruct the Authenticator object from DB data.

        com.webauthn4j.data.attestation.authenticator.AttestedCredentialData attestedCredentialData = new com.webauthn4j.data.attestation.authenticator.AttestedCredentialData(
                new com.webauthn4j.data.aaguid.AAGUID(java.util.UUID.fromString(credential.getAaguid())),
                credentialIdBytes,
                new com.webauthn4j.converter.util.CborConverter().readValue(
                        java.util.Base64.getDecoder().decode(credential.getPublicKey()),
                        com.webauthn4j.data.attestation.statement.COSEKey.class));

        com.webauthn4j.data.Authenticator authenticator = new com.webauthn4j.data.Authenticator(
                attestedCredentialData,
                new com.webauthn4j.data.attestation.statement.NoneAttestationStatement(), // Not needed for login
                credential.getCounter());

        com.webauthn4j.data.AuthenticationParameters authenticationParameters = new com.webauthn4j.data.AuthenticationParameters(
                new com.webauthn4j.server.ServerProperty(
                        new com.webauthn4j.data.client.Origin("http://localhost:8080"),
                        RP_ID,
                        challenge,
                        null),
                authenticator,
                false, // user verification
                false);

        com.webauthn4j.data.AuthenticationData authenticationData = webAuthnManager.parse(authenticationRequest);
        webAuthnManager.validate(authenticationData, authenticationParameters);

        // Update counter
        credential.setCounter(authenticationData.getAuthenticatorData().getSignCount());
        credential.setLastUsed(LocalDateTime.now());
        biometricRepository.save(credential);

        return credential.getUser();
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
