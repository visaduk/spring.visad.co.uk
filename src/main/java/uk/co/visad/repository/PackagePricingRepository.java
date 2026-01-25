package uk.co.visad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.visad.entity.PackagePricing;

import java.util.Optional;

@Repository
public interface PackagePricingRepository extends JpaRepository<PackagePricing, Long> {

    Optional<PackagePricing> findByPackageName(String packageName);

    boolean existsByPackageName(String packageName);
}
