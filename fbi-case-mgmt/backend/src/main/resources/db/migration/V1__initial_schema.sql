-- =============================================================================
-- V1: Initial schema for Demo Only case management.
-- =============================================================================

CREATE TABLE cases (
    id                   UUID PRIMARY KEY,
    case_number          VARCHAR(32)  NOT NULL UNIQUE,
    classification_code  VARCHAR(4)   NOT NULL,
    originating_office   VARCHAR(4)   NOT NULL,
    serial_number        BIGINT       NOT NULL,
    title                VARCHAR(255) NOT NULL,
    synopsis             TEXT,
    status               VARCHAR(32)  NOT NULL,
    assigned_agent_id    UUID,
    supervisor_id        UUID,
    metadata             JSONB        NOT NULL DEFAULT '{}'::jsonb,
    workflow_id          VARCHAR(128),
    created_at           TIMESTAMPTZ  NOT NULL,
    created_by           VARCHAR(128),
    updated_at           TIMESTAMPTZ,
    updated_by           VARCHAR(128),
    opened_at            TIMESTAMPTZ,
    closed_at            TIMESTAMPTZ,
    closure_reason       VARCHAR(500),
    version              BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT case_number_format
        CHECK (case_number ~ '^[0-9]{3}-[A-Z]{2,3}-[0-9]+$')
);

CREATE INDEX idx_case_status   ON cases (status);
CREATE INDEX idx_case_assigned ON cases (assigned_agent_id);
CREATE INDEX idx_case_office   ON cases (originating_office);
CREATE INDEX idx_case_metadata ON cases USING GIN (metadata);

CREATE TABLE case_tags (
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    tag     VARCHAR(128) NOT NULL,
    PRIMARY KEY (case_id, tag)
);
CREATE INDEX idx_case_tag ON case_tags (tag);

CREATE TABLE serial_documents (
    id                  UUID PRIMARY KEY,
    case_id             UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    serial_index        INTEGER NOT NULL,
    document_type       VARCHAR(32)  NOT NULL,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    s3_key              VARCHAR(512) NOT NULL,
    pdf_s3_key          VARCHAR(512),
    original_filename   VARCHAR(255),
    content_type        VARCHAR(128),
    size_bytes          BIGINT,
    sha256              VARCHAR(64),
    processing_status   VARCHAR(32) NOT NULL,
    extracted_text      TEXT,
    ocr_page_count      INTEGER,
    metadata            JSONB NOT NULL DEFAULT '{}'::jsonb,
    uploaded_at         TIMESTAMPTZ NOT NULL,
    uploaded_by         VARCHAR(128),
    indexed_at          TIMESTAMPTZ,
    version             BIGINT NOT NULL DEFAULT 0,
    UNIQUE (case_id, serial_index)
);
CREATE INDEX idx_sdoc_type   ON serial_documents (document_type);
CREATE INDEX idx_sdoc_status ON serial_documents (processing_status);

-- Audit events are append-only.
CREATE TABLE audit_events (
    id              UUID PRIMARY KEY,
    occurred_at     TIMESTAMPTZ NOT NULL,
    actor_username  VARCHAR(128) NOT NULL,
    actor_roles     VARCHAR(512),
    actor_ip        VARCHAR(45),
    action          VARCHAR(64) NOT NULL,
    entity_type     VARCHAR(64),
    entity_id       VARCHAR(128),
    case_number     VARCHAR(32),
    outcome         VARCHAR(500),
    details         JSONB,
    request_id      VARCHAR(64)
);
CREATE INDEX idx_audit_actor  ON audit_events (actor_username);
CREATE INDEX idx_audit_entity ON audit_events (entity_type, entity_id);
CREATE INDEX idx_audit_action ON audit_events (action);
CREATE INDEX idx_audit_time   ON audit_events (occurred_at);
CREATE INDEX idx_audit_case   ON audit_events (case_number);

-- Enforce immutability on audit_events.
CREATE OR REPLACE FUNCTION reject_audit_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_events rows are immutable (% by %)', TG_OP, current_user;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_events_no_update
BEFORE UPDATE OR DELETE ON audit_events
FOR EACH ROW EXECUTE FUNCTION reject_audit_mutation();

-- Counter for generating serials per (classification, office).
CREATE TABLE case_serial_sequence (
    classification_code VARCHAR(4) NOT NULL,
    originating_office  VARCHAR(4) NOT NULL,
    next_value          BIGINT     NOT NULL DEFAULT 1,
    PRIMARY KEY (classification_code, originating_office)
);

-- Approvals for workflow-driven transitions.
CREATE TABLE case_approvals (
    id               UUID PRIMARY KEY,
    case_id          UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    approval_type    VARCHAR(64) NOT NULL,
    requested_by     VARCHAR(128) NOT NULL,
    requested_at     TIMESTAMPTZ  NOT NULL,
    decided_by       VARCHAR(128),
    decided_at       TIMESTAMPTZ,
    decision         VARCHAR(32),
    comments         TEXT,
    workflow_id      VARCHAR(128)
);
CREATE INDEX idx_approval_case   ON case_approvals (case_id);
CREATE INDEX idx_approval_status ON case_approvals (decision);
