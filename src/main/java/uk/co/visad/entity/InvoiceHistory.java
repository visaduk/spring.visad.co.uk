package uk.co.visad.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_history", indexes = {
        @Index(name = "idx_invoice_history_record", columnList = "record_id, record_type")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT")
    private Long id;

    @Column(name = "record_id", nullable = false, columnDefinition = "INT")
    private Long recordId;

    @Column(name = "record_type", nullable = false, length = 20)
    private String recordType;

    @Column(name = "invoice_type", length = 20)
    private String invoiceType;

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @Column(name = "sent_to_email", length = 150)
    private String sentToEmail;

    @CreatedDate
    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;
}
