package uk.co.visad.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.co.visad.entity.Traveler;

import java.util.List;
import java.util.Optional;

@Repository
public interface TravelerRepository extends JpaRepository<Traveler, Long> {

       // Find by passport number
       Optional<Traveler> findByPassportNo(String passportNo);

       // Find by public URL token
       Optional<Traveler> findByPublicUrlToken(String token);

       // Find by email
       List<Traveler> findByEmail(String email);

       // Paginated query with traveler questions joined
       @Query("SELECT t FROM Traveler t LEFT JOIN FETCH t.travelerQuestions ORDER BY t.id DESC")
       Page<Traveler> findAllWithQuestions(Pageable pageable);

       // Find traveler with dependents
       @Query("SELECT t FROM Traveler t LEFT JOIN FETCH t.dependents WHERE t.id = :id")
       Optional<Traveler> findByIdWithDependents(@Param("id") Long id);

       // Find traveler with all relations
       @Query("SELECT t FROM Traveler t " +
                     "LEFT JOIN FETCH t.dependents " +
                     "LEFT JOIN FETCH t.travelerQuestions " +
                     "WHERE t.id = :id")
       Optional<Traveler> findByIdWithAllRelations(@Param("id") Long id);

       // Search by name or passport
       @Query("SELECT t FROM Traveler t WHERE " +
                     "LOWER(t.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "LOWER(t.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "t.passportNo LIKE CONCAT('%', :search, '%')")
       List<Traveler> searchByNameOrPassport(@Param("search") String search);

       // Find travelers without generated invoice
       // DISABLED: invoiceGenerated field doesn't exist in production DB
       // @Query("SELECT t FROM Traveler t WHERE t.invoiceGenerated IS NULL OR
       // t.invoiceGenerated = false")
       // List<Traveler> findWithoutInvoice();

       // Count travelers by status
       long countByStatus(String status);

       // Optimized projection for dashboard
       @Query("SELECT t FROM Traveler t ORDER BY t.id DESC")
       Page<TravelerSummaryProjection> findAllProjectedBy(Pageable pageable);
}
