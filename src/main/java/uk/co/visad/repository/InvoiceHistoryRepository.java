package uk.co.visad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.visad.entity.InvoiceHistory;
import java.util.List;

@Repository
public interface InvoiceHistoryRepository extends JpaRepository<InvoiceHistory, Long> {
    List<InvoiceHistory> findByRecordIdOrderBySentAtDesc(Long recordId);
}
