package uk.co.visad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.co.visad.entity.Dependent;

import java.util.List;
import java.util.Optional;

@Repository
public interface DependentRepository extends JpaRepository<Dependent, Long> {

    // Find all dependents for a traveler
    List<Dependent> findByTraveler_Id(Long travelerId);

    // Find by passport number
    Optional<Dependent> findByPassportNo(String passportNo);

    // Find by public URL token
    Optional<Dependent> findByPublicUrlToken(String token);

    // Find dependents with traveler questions
    @Query("SELECT d FROM Dependent d WHERE d.traveler.id = :travelerId")
    List<Dependent> findByTravelerIdWithQuestions(@Param("travelerId") Long travelerId);

    // Find dependent with all details
    @Query("SELECT d FROM Dependent d " +
            "LEFT JOIN FETCH d.traveler " +
            "WHERE d.id = :id")
    Optional<Dependent> findByIdWithAllRelations(@Param("id") Long id);

    // Delete all dependents for a traveler
    void deleteByTraveler_Id(Long travelerId);

    // Count dependents for a traveler
    // Batch fetch dependents for multiple travelers
    List<Dependent> findAllByTraveler_IdIn(List<Long> travelerIds);

    long countByTraveler_Id(Long travelerId);
}
