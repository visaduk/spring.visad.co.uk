package uk.co.visad.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.co.visad.entity.Dependent;
import uk.co.visad.entity.Traveler;
import uk.co.visad.exception.ResourceNotFoundException;
import uk.co.visad.repository.DependentRepository;
import uk.co.visad.repository.TravelerRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final TravelerRepository travelerRepository;
    private final DependentRepository dependentRepository;

    public Object verifyToken(String token) {
        Optional<Traveler> traveler = travelerRepository.findByPublicUrlToken(token);
        if (traveler.isPresent()) {
            // In a real implementation, map to DTO to hide sensitive fields
            return traveler.get();
        }

        Optional<Dependent> dependent = dependentRepository.findByPublicUrlToken(token);
        if (dependent.isPresent()) {
            return dependent.get();
        }

        throw new ResourceNotFoundException("Invalid or expired token");
    }
}
