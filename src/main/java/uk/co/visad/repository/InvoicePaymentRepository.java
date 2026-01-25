package uk.co.visad.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.co.visad.entity.InvoicePayment;

import java.util.List;

@Repository
public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, Long> {

    List<InvoicePayment> findByInvoiceId(Long invoiceId);
}
