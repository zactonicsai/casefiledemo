# Data Dictionary

## `cases`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PK | Internal identifier |
| case_number | VARCHAR(32) | UNIQUE, `^\d{3}-[A-Z]{2,3}-\d+$` | FBI-style number (e.g. `111-HQ-12345`) |
| classification_code | VARCHAR(4) | NOT NULL | 3-digit classification (`111`, `245`, …) |
| originating_office | VARCHAR(4) | NOT NULL | 2–3 letter office code |
| serial_number | BIGINT | NOT NULL | Sequence within office |
| title | VARCHAR(255) | NOT NULL | Short title |
| synopsis | TEXT |  | Long-form case summary |
| status | VARCHAR(32) | NOT NULL | Lifecycle state (see `CaseStatus`) |
| assigned_agent_id | UUID |  | Primary investigator |
| supervisor_id | UUID |  | Reviewing supervisor |
| metadata | JSONB | NOT NULL, default `{}` | Flexible structured data (suspects, vehicles, locations) |
| workflow_id | VARCHAR(128) |  | Temporal workflow ID for this case |
| created_at, updated_at | TIMESTAMPTZ | NOT NULL | Audit timestamps |
| created_by, updated_by | VARCHAR(128) |  | User who performed the action |
| opened_at, closed_at | TIMESTAMPTZ |  | Lifecycle markers |
| closure_reason | VARCHAR(500) |  | Why the case was closed |
| version | BIGINT | NOT NULL | Optimistic locking |

## `serial_documents`

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PK | Document identifier |
| case_id | UUID | FK → cases(id) CASCADE | Parent case |
| serial_index | INTEGER | NOT NULL, UNIQUE (case_id, serial_index) | 1-based serial within case |
| document_type | VARCHAR(32) | NOT NULL | `FD_302`, `EC`, `LHM`, `EVIDENCE_LOG`, `PHOTO`, … |
| title, description | VARCHAR(255) / TEXT |  | Human-entered |
| s3_key | VARCHAR(512) | NOT NULL | Location of original binary |
| pdf_s3_key | VARCHAR(512) |  | Converted PDF (if generated) |
| original_filename, content_type | VARCHAR(255) / VARCHAR(128) |  | Upload metadata |
| size_bytes | BIGINT |  | Original file size |
| sha256 | VARCHAR(64) |  | Hex digest for chain-of-custody |
| processing_status | VARCHAR(32) | NOT NULL | Pipeline state: UPLOADED → … → INDEXED |
| extracted_text | TEXT |  | OCR / PDF text for indexing |
| ocr_page_count | INTEGER |  | Pages processed by OCR |
| metadata | JSONB | NOT NULL, default `{}` | EXIF, form fields, custom metadata |
| uploaded_at, indexed_at | TIMESTAMPTZ |  | Pipeline timestamps |
| uploaded_by | VARCHAR(128) |  | Uploader username |
| version | BIGINT | NOT NULL | Optimistic locking |

## `audit_events` (append-only)

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PK | Event identifier |
| occurred_at | TIMESTAMPTZ | NOT NULL | When the action happened |
| actor_username | VARCHAR(128) | NOT NULL | JWT `preferred_username` |
| actor_roles | VARCHAR(512) |  | Comma-separated roles at time of action |
| actor_ip | VARCHAR(45) |  | Remote address (IPv4/IPv6) |
| action | VARCHAR(64) | NOT NULL | e.g. `CREATE_CASE`, `UPLOAD_DOCUMENT`, `APPROVE_CASE` |
| entity_type, entity_id | VARCHAR(64) / VARCHAR(128) |  | Target of the action |
| case_number | VARCHAR(32) |  | Denormalized for quick case-scoped queries |
| outcome | VARCHAR(500) |  | Short human-readable result |
| details | JSONB |  | Before/after diff, request params |
| request_id | VARCHAR(64) |  | X-Request-Id correlation |

**Trigger `audit_events_no_update`:** raises exception on any UPDATE or DELETE.

## `case_approvals`

| Column | Type | Description |
|---|---|---|
| id | UUID | Approval identifier |
| case_id | UUID | Parent case |
| approval_type | VARCHAR(64) | `OPEN_CASE`, `CLOSE_CASE`, `RECLASSIFY`, … |
| requested_by, requested_at | — | Who/when requested |
| decided_by, decided_at, decision | — | Who/when decided, outcome (`APPROVED` / `REJECTED` / `WITHDRAWN`) |
| comments | TEXT | Reviewer notes |
| workflow_id | VARCHAR(128) | Temporal workflow handling the approval |

## `case_serial_sequence`

| Column | Type | Description |
|---|---|---|
| classification_code | VARCHAR(4) | Part of composite PK |
| originating_office | VARCHAR(4) | Part of composite PK |
| next_value | BIGINT | Next serial to issue |

Used by `CaseSerialSequenceRepository.nextSerial()` to atomically allocate the next `SERIAL` for a given `(classification, office)` pair via UPSERT + RETURNING.

## `case_tags`

Simple element-collection table: `(case_id UUID, tag VARCHAR(128))` with PK `(case_id, tag)`.
