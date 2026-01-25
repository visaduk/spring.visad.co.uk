package uk.co.visad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.visad.entity.InvoiceItem;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    List<InvoiceItem> findByInvoiceId(Long invoiceId);

    List<InvoiceItem> findByTravelerId(Long travelerId);

    void deleteByInvoiceId(Long invoiceId);
}
