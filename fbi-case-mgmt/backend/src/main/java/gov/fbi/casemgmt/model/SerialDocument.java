package gov.fbi.casemgmt.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A serial document filed into a case. Corresponds to FBI "serials" — numbered
 * artifacts of a case file (FD-302 interview reports, ECs, evidence logs, photos).
 * The binary lives in S3; this row holds metadata and references.
 */
@Entity
@Table(
    name = "serial_documents",
    indexes = {
        @Index(name = "idx_sdoc_case",    columnList = "case_id"),
        @Index(name = "idx_sdoc_type",    columnList = "document_type"),
        @Index(name = "idx_sdoc_status",  columnList = "processing_status")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SerialDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    @ToString.Exclude
    private Case caseFile;

    /** 1-based serial within the case. */
    @NotNull
    @Column(name = "serial_index", nullable = false)
    private Integer serialIndex;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 32)
    private DocumentType documentType;

    @NotNull
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** S3 object key — location of the original binary. */
    @NotNull
    @Column(name = "s3_key", nullable = false, length = 512)
    private String s3Key;

    /** S3 key of the converted PDF view (if applicable). */
    @Column(name = "pdf_s3_key", length = 512)
    private String pdfS3Key;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    /** SHA-256 hex digest of the original bytes. */
    @Column(name = "sha256", length = 64)
    private String sha256;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 32)
    @Builder.Default
    private ProcessingStatus processingStatus = ProcessingStatus.UPLOADED;

    /** Extracted/OCR'd plain text for indexing. */
    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    /** Number of OCR pages processed (for cost metering). */
    @Column(name = "ocr_page_count")
    private Integer ocrPageCount;

    /** Arbitrary structured metadata (EXIF, form fields, author). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @CreatedDate
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @CreatedBy
    @Column(name = "uploaded_by", updatable = false, length = 128)
    private String uploadedBy;

    @Column(name = "indexed_at")
    private Instant indexedAt;

    @Version
    private Long version;

    public enum ProcessingStatus {
        UPLOADED,
        VIRUS_SCANNING,
        CONVERTING,
        OCR_PENDING,
        OCR_COMPLETE,
        INDEXED,
        FAILED
    }
}
