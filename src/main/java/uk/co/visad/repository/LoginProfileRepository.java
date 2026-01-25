package uk.co.visad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.visad.entity.LoginProfile;

import java.util.List;

@Repository
public interface LoginProfileRepository extends JpaRepository<LoginProfile, Long> {

    List<LoginProfile> findByProfileNameContainingIgnoreCase(String name);

    List<LoginProfile> findByIsPinnedTrue();
}
