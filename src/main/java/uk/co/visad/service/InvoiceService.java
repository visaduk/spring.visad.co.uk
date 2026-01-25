package uk.co.visad.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.visad.entity.InvoiceHistory;
import uk.co.visad.repository.InvoiceHistoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceHistoryRepository invoiceHistoryRepository;

    @Transactional(readOnly = true)
    public List<InvoiceHistory> getHistory(Long travelerId) {
        return invoiceHistoryRepository.findByRecordIdOrderBySentAtDesc(travelerId);
    }
}
