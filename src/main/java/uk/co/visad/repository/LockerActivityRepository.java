package uk.co.visad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.co.visad.entity.LockerActivity;

import java.util.List;

public interface LockerActivityRepository extends JpaRepository<LockerActivity, Long> {
    List<LockerActivity> findTop50ByOrderByCreatedAtDesc();
}
