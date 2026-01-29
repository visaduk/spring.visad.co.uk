package uk.co.visad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.visad.entity.BiometricCredential;
import uk.co.visad.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface BiometricCredentialRepository extends JpaRepository<BiometricCredential, Long> {
    Optional<BiometricCredential> findByCredentialId(String credentialId);

    List<BiometricCredential> findAllByUser(User user);

    void deleteByCredentialId(String credentialId);
}
