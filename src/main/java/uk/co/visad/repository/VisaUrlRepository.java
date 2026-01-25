package uk.co.visad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.co.visad.entity.VisaUrl;

import java.util.List;
import java.util.Optional;

@Repository
public interface VisaUrlRepository extends JpaRepository<VisaUrl, Long> {

    List<VisaUrl> findAllByOrderByCountryAscVisaCenterAsc();

    // Find specific match for country and visa center
    Optional<VisaUrl> findByCountryAndVisaCenter(String country, String visaCenter);

    // Find general match for country only (visa_center is null or empty)
    @Query("SELECT v FROM VisaUrl v WHERE v.country = :country AND (v.visaCenter IS NULL OR v.visaCenter = '')")
    Optional<VisaUrl> findByCountryWithNoVisaCenter(@Param("country") String country);

    // Find all URLs for a country
    List<VisaUrl> findByCountry(String country);

    // Check if combination exists
    boolean existsByCountryAndVisaCenter(String country, String visaCenter);
}
