package gov.fbi.casemgmt.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable audit event. Records every material action taken on a case
 * or document so the system satisfies audit and chain-of-custody needs.
 *
 * <p>Rows are append-only — never updated or deleted (enforced by DB trigger
 * and application logic). Retention: 7 years minimum.
 */
@Entity
@Table(
    name = "audit_events",
    indexes = {
        @Index(name = "idx_audit_actor",      columnList = "actor_username"),
        @Index(name = "idx_audit_entity",     columnList = "entity_type,entity_id"),
        @Index(name = "idx_audit_action",     columnList = "action"),
        @Index(name = "idx_audit_time",       columnList = "occurred_at"),
        @Index(name = "idx_audit_case",       columnList = "case_number")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_username", nullable = false, length = 128, updatable = false)
    private String actorUsername;

    @Column(name = "actor_roles", length = 512, updatable = false)
    private String actorRoles;

    @Column(name = "actor_ip", length = 45, updatable = false)
    private String actorIp;

    /** CREATE_CASE, VIEW_CASE, UPLOAD_DOCUMENT, DOWNLOAD_DOCUMENT, APPROVE, ... */
    @Column(nullable = false, length = 64, updatable = false)
    private String action;

    @Column(name = "entity_type", length = 64, updatable = false)
    private String entityType;

    @Column(name = "entity_id", length = 128, updatable = false)
    private String entityId;

    @Column(name = "case_number", length = 32, updatable = false)
    private String caseNumber;

    /** Short human-readable outcome. */
    @Column(length = 500, updatable = false)
    private String outcome;

    /** Arbitrary structured payload: before/after diffs, request params. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> details;

    @Column(name = "request_id", length = 64, updatable = false)
    private String requestId;
}
