package uk.co.visad.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_documents_record", columnList = "record_id, record_type, category")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT")
    private Long id;

    @Column(name = "record_id", nullable = false, columnDefinition = "INT")
    private Long recordId;

    @Column(name = "record_type", nullable = false, length = 20)
    private String recordType;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "uploaded_by", columnDefinition = "INT")
    private Long uploadedBy;

    @CreatedDate
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", columnDefinition = "INT")
    private Long deletedBy;

    // Helper to check if deleted
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
