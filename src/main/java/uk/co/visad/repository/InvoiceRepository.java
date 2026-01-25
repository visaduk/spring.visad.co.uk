package uk.co.visad.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.co.visad.entity.Invoice;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByTravelerId(Long travelerId);

    @Query("SELECT i FROM Invoice i WHERE i.paymentStatus = 'Unpaid' AND i.createdAt < CURRENT_DATE")
    List<Invoice> findOverdueInvoices();

    @Query("SELECT i FROM Invoice i WHERE " +
            "LOWER(i.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "i.invoiceNumber LIKE CONCAT('%', :search, '%')")
    Page<Invoice> searchInvoices(@Param("search") String search, Pageable pageable);
}
