package gov.fbi.casemgmt.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "case_approvals")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CaseApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "approval_type", nullable = false, length = 64)
    private String approvalType;  // OPEN_CASE, CLOSE_CASE, RECLASSIFY, etc.

    @Column(name = "requested_by", nullable = false, length = 128)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "decided_by", length = 128)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    /** APPROVED / REJECTED / WITHDRAWN */
    @Column(length = 32)
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "workflow_id", length = 128)
    private String workflowId;
}
