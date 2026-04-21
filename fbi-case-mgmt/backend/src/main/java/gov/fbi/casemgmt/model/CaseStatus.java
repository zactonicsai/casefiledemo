package gov.fbi.casemgmt.model;

/**
 * Case lifecycle states from inception to closure.
 * State transitions are orchestrated by Temporal workflows.
 */
public enum CaseStatus {
    /** Initial draft; case is being created, not yet submitted. */
    DRAFT,
    /** Submitted for supervisor approval to open. */
    PENDING_APPROVAL,
    /** Approved and actively being worked. */
    OPEN,
    /** Work paused — awaiting external action (e.g., court proceedings). */
    SUSPENDED,
    /** Under supervisor review prior to closure. */
    CLOSURE_REVIEW,
    /** Formally closed. Archived but still searchable. */
    CLOSED,
    /** Moved to long-term archive (records retention). */
    ARCHIVED
}
