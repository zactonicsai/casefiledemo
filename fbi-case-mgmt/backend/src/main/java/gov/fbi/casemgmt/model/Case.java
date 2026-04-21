package gov.fbi.casemgmt.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.*;

/**
 * FBI-style case file entity.
 * <p>
 * Numbered as {@code <CLASSIFICATION>-<OFFICE>-<SERIAL>}, e.g. {@code 111-HQ-12345}.
 * The case is the root aggregate; {@link SerialDocument} entries are filed under it.
 */
@Entity
@Table(
    name = "cases",
    indexes = {
        @Index(name = "idx_case_number", columnList = "case_number", unique = true),
        @Index(name = "idx_case_status", columnList = "status"),
        @Index(name = "idx_case_assigned", columnList = "assigned_agent_id"),
        @Index(name = "idx_case_office",   columnList = "originating_office")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Full case number, e.g. "111-HQ-12345". */
    @NotNull
    @Pattern(regexp = "^[0-9]{3}-[A-Z]{2,3}-[0-9]{1,10}$",
             message = "Case number must match pattern CCC-OFF-SERIAL (e.g. 111-HQ-12345)")
    @Column(name = "case_number", unique = true, nullable = false, length = 32)
    private String caseNumber;

    /** 3-digit classification code (e.g. "111"). */
    @NotNull
    @Column(name = "classification_code", nullable = false, length = 4)
    private String classificationCode;

    /** Originating office (e.g. "HQ", "NY", "WF"). */
    @NotNull
    @Size(min = 2, max = 3)
    @Column(name = "originating_office", nullable = false, length = 4)
    private String originatingOffice;

    /** Sequential serial assigned by the office. */
    @NotNull
    @Column(name = "serial_number", nullable = false)
    private Long serialNumber;

    @NotNull
    @Size(min = 3, max = 255)
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CaseStatus status;

    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;

    @Column(name = "supervisor_id")
    private UUID supervisorId;

    /** Free-form tags for the case (suspects, subjects, locations). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "case_tags", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "tag")
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    /** Structured metadata: suspects, vehicles, addresses, etc. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /** Temporal workflow ID currently orchestrating this case. */
    @Column(name = "workflow_id", length = 128)
    private String workflowId;

    @OneToMany(mappedBy = "caseFile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("serialIndex ASC")
    @Builder.Default
    private List<SerialDocument> documents = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 128)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", length = 128)
    private String updatedBy;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closure_reason", length = 500)
    private String closureReason;

    @Version
    private Long version;

    /** Appends a new serial document, automatically numbering it. */
    public SerialDocument addDocument(SerialDocument doc) {
        doc.setCaseFile(this);
        doc.setSerialIndex(documents.size() + 1);
        documents.add(doc);
        return doc;
    }
}
