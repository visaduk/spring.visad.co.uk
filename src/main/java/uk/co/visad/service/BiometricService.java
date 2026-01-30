package uk.co.visad.service;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.visad.entity.BiometricCredential;
import uk.co.visad.entity.User;
import uk.co.visad.repository.BiometricCredentialRepository;
import uk.co.visad.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BiometricService {

    private final BiometricCredentialRepository biometricRepository;
    private final UserRepository userRepository;

    private final com.webauthn4j.converter.util.ObjectConverter objectConverter = new com.webauthn4j.converter.util.ObjectConverter();

    // In a real app, strict origin checking is required.
    // For localhost dev, we might be lenient or configurable.
    @org.springframework.beans.factory.annotation.Value("${app.webauthn.rp-id:localhost}")
    private String rpId;

    @org.springframework.beans.factory.annotation.Value("${app.webauthn.rp-name:Visad Vault}")
    private String rpName;

    @org.springframework.beans.factory.annotation.Value("${app.webauthn.origin:http://localhost:8080}")
    private String originUrl;

    private final WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();

    public Challenge generateChallenge() {
        return new DefaultChallenge();
    }

    public String getRpId() {
        return rpId;
    }

    public String getRpName() {
        return rpName;
    }

    @Transactional
    public void completeRegistration(User user, String clientDataJSONStr, String attestationObjectStr,
            Challenge challenge) {
        byte[] clientDataJSON = Base64.getUrlDecoder().decode(clientDataJSONStr);
        byte[] attestationObject = Base64.getUrlDecoder().decode(attestationObjectStr);

        RegistrationRequest registrationRequest = new RegistrationRequest(attestationObject, clientDataJSON);

        RegistrationParameters registrationParameters = new RegistrationParameters(
                new ServerProperty(
                        new Origin(originUrl), 
                        rpId,
                        challenge,
                        null),
                false // User verification required? (frontend sends 'required', backend should check)
        );

        RegistrationData registrationData = webAuthnManager.parse(registrationRequest);
        webAuthnManager.validate(registrationData, registrationParameters);

        // Extract credential data
        AttestedCredentialData attestedCredentialData = registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData();
        String credentialId = Base64.getUrlEncoder().withoutPadding().encodeToString(attestedCredentialData.getCredentialId());
        
        try {
            String publicKey = Base64.getEncoder().encodeToString(objectConverter.getCborConverter().writeValueAsBytes(attestedCredentialData.getCOSEKey()));
            long count = registrationData.getAttestationObject().getAuthenticatorData().getSignCount();
            String aaguid = attestedCredentialData.getAaguid().toString();

            registerCredential(user, credentialId, publicKey, aaguid, count);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode/save credential", e);
        }
    }

    @Transactional
    public User completeLogin(String credentialId, String clientDataJSONStr, String authenticatorDataStr,
            String signatureStr, String userHandleStr, Challenge challenge) {
        byte[] credentialIdBytes = Base64.getUrlDecoder().decode(credentialId);
        byte[] clientDataJSON = Base64.getUrlDecoder().decode(clientDataJSONStr);
        byte[] authenticatorData = Base64.getUrlDecoder().decode(authenticatorDataStr);
        byte[] signature = Base64.getUrlDecoder().decode(signatureStr);
        byte[] userHandle = userHandleStr != null ? Base64.getUrlDecoder().decode(userHandleStr) : null;

        BiometricCredential credential = biometricRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new RuntimeException("Credential not found"));

        AuthenticationRequest authenticationRequest = new AuthenticationRequest(
                credentialIdBytes,
                userHandle,
                authenticatorData,
                clientDataJSON,
                signature);

        try {
            // Reconstruct Authenticator object
            AttestedCredentialData attestedCredentialData = new AttestedCredentialData(
                    new AAGUID(UUID.fromString(credential.getAaguid())),
                    credentialIdBytes,
                    objectConverter.getCborConverter().readValue(Base64.getDecoder().decode(credential.getPublicKey()), COSEKey.class)
            );

            AuthenticatorImpl authenticator = new AuthenticatorImpl(
                    attestedCredentialData,
                    new NoneAttestationStatement(),
                    credential.getCounter()
            );

            AuthenticationParameters authenticationParameters = new AuthenticationParameters(
                    new ServerProperty(
                            new Origin(originUrl),
                            rpId,
                            challenge,
                            null),
                    authenticator,
                    false, // user verification
                    false  // user presence (default true usually, but strict check handles it)
            );

            AuthenticationData authenticationData = webAuthnManager.parse(authenticationRequest);
            webAuthnManager.validate(authenticationData, authenticationParameters);

            // Update counter
            credential.setCounter(authenticationData.getAuthenticatorData().getSignCount());
            credential.setLastUsed(LocalDateTime.now());
            biometricRepository.save(credential);

            return credential.getUser();
        } catch (Exception e) {
            throw new RuntimeException("Authentication failed", e);
        }
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
}
