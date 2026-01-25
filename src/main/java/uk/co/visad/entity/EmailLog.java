package uk.co.visad.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_log", indexes = {
        @Index(name = "idx_email_log_record", columnList = "record_id, record_type")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT")
    private Long id;

    @Column(name = "record_id", nullable = false, columnDefinition = "INT")
    private Long recordId;

    @Column(name = "record_type", nullable = false, length = 20)
    private String recordType;

    @Column(name = "recipient_email", nullable = false, length = 150)
    private String recipientEmail;

    @Column(name = "sent_by", columnDefinition = "INT")
    private Long sentBy;

    @CreatedDate
    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;

    @Column(length = 255)
    private String subject;

    @Column(length = 20)
    @Builder.Default
    private String status = "sent";
}
