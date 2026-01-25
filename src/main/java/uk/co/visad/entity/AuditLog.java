package uk.co.visad.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_record", columnList = "record_id, record_type"),
        @Index(name = "idx_audit_logs_timestamp", columnList = "timestamp")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT")
    private Long id;

    @Column(name = "user_id", columnDefinition = "INT")
    private Long userId;

    @Column(length = 50)
    private String username;

    @Column(name = "record_type", nullable = false, length = 20)
    private String recordType;

    @Column(name = "record_id", nullable = false, columnDefinition = "INT")
    private Long recordId;

    @Column(name = "record_name", length = 255)
    private String recordName;

    @Column(name = "field_changed", length = 100)
    private String fieldChanged;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @CreatedDate
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // Factory method for creating audit logs
    public static AuditLog create(Long userId, String username, String recordType,
            Long recordId, String recordName,
            String fieldChanged, String oldValue, String newValue) {
        return AuditLog.builder()
                .userId(userId)
                .username(username)
                .recordType(recordType)
                .recordId(recordId)
                .recordName(recordName)
                .fieldChanged(fieldChanged)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
    }
}
