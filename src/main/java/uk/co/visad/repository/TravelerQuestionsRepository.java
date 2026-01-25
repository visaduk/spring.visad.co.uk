package uk.co.visad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.visad.entity.TravelerQuestions;

import java.util.Optional;

@Repository
public interface TravelerQuestionsRepository extends JpaRepository<TravelerQuestions, Long> {

    Optional<TravelerQuestions> findByRecordIdAndRecordType(Long recordId, String recordType);

    void deleteByRecordIdAndRecordType(Long recordId, String recordType);

    java.util.List<TravelerQuestions> findAllByRecordIdInAndRecordType(java.util.List<Long> recordIds,
            String recordType);
}
