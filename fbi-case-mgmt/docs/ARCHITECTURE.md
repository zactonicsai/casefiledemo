# Architecture

## Overview

Sentinel CMS is a reference implementation of an FBI-style electronic case management system. It models the Sentinel application's core behaviors — numbered case files, serial documents, workflow approvals, full-text search, and audit trails — using modern open-source infrastructure.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            Browser (Angular 17)                         │
└────────────────────────────┬────────────────────────────────────────────┘
                             │  HTTPS / OIDC Bearer (JWT)
┌────────────────────────────▼────────────────────────────────────────────┐
│                         nginx (reverse proxy)                           │
└────────────┬──────────────────────┬─────────────────────────────────────┘
             │                      │
             ▼                      ▼
    ┌──────────────────┐   ┌───────────────────────────────────────┐
    │  Keycloak 24     │   │  Spring Boot 3.5.13 (Java 21)         │
    │  (OIDC, PKCE)    │   │  ────────────────────────────────     │
    │  realm=sentinel  │   │  Controllers                          │
    └──────────────────┘   │  ├─ CaseController                    │
                           │  ├─ DocumentController                │
                           │  ├─ SearchController                  │
                           │  ├─ DashboardController               │
                           │  ├─ AuditController                   │
                           │  └─ MeController                      │
                           │                                       │
                           │  Services                             │
                           │  ├─ CaseService (serial allocation,   │
                           │  │   lifecycle transitions)           │
                           │  ├─ DocumentService (upload -> S3 ->  │
                           │  │   SQS ingest pipeline)             │
                           │  ├─ StorageService (S3 w/ SSE + SHA)  │
                           │  ├─ OcrService (Tesseract)            │
                           │  ├─ ConversionService (LibreOffice)   │
                           │  ├─ SearchService (Elasticsearch)     │
                           │  └─ AuditService (immutable log)      │
                           │                                       │
                           │  Workflows (Temporal)                 │
                           │  ├─ CaseLifecycleWorkflow             │
                           │  └─ DocumentIngestWorkflow            │
                           └───┬───────┬─────────┬─────────┬───────┘
                               │       │         │         │
           ┌───────────────────┘       │         │         └──────────────┐
           ▼                           ▼         ▼                        ▼
   ┌────────────────┐     ┌─────────────────┐  ┌─────────────┐   ┌──────────────┐
   │ PostgreSQL 16  │     │ Elasticsearch 8 │  │ Temporal    │   │ AWS S3 / SQS │
   │ ─────────────  │     │ ──────────────  │  │ 1.24        │   │ (LocalStack) │
   │ cases          │     │ full-text idx   │  │ workers     │   │ documents/   │
   │ serial_docs    │     │ facets          │  │ histories   │   │ ingest-queue │
   │ audit_events   │     │ OCR body        │  │             │   │              │
   │ approvals      │     │                 │  │             │   │              │
   └────────────────┘     └─────────────────┘  └─────────────┘   └──────────────┘
```

## Case Number Format

FBI case numbers take the form `CCC-OFF-SERIAL`:

| Segment | Example | Meaning |
|---|---|---|
| `CCC` | `111` | 3-digit classification code (Violent Crime, Cyber, etc.) |
| `OFF` | `HQ`, `NY`, `WF` | 2–3 letter originating field office |
| `SERIAL` | `12345` | Sequence assigned by the office at case opening |

The `case_serial_sequence` table issues serials atomically per `(classification, office)` pair using `INSERT … ON CONFLICT DO UPDATE … RETURNING`.

## Case Lifecycle (Temporal workflow)

```
 ┌────────┐ submit  ┌────────────────────┐ approve  ┌────────┐
 │ DRAFT  ├────────▶│ PENDING_APPROVAL   ├─────────▶│  OPEN  │◀─┐
 └────────┘         └────────┬───────────┘          └───┬────┘  │
     ▲                       │reject                    │       │resume
     └───────────────────────┘                          │       │
                                                        │suspend│
                                                    ┌───▼────┐  │
                                                    │SUSPEND │──┘
                                                    └────────┘
                                                        │
                                                   request closure
                                                        ▼
                                            ┌───────────────────┐
                                            │ CLOSURE_REVIEW    │
                                            └─────────┬─────────┘
                                                      │ confirm
                                                      ▼
                                                 ┌────────┐
                                                 │ CLOSED │
                                                 └────────┘
```

Each case has a long-running Temporal workflow (`case-<UUID>`) that waits on signals. The API translates REST actions into workflow signals; the workflow calls the `applyStatus` activity to persist transitions via `CaseService`.

This design keeps all state machine logic replayable, auditable, and durable — a crash in the API never leaves the state inconsistent because Temporal retries until the activity succeeds.

## Document Ingest Pipeline (Temporal workflow)

```
upload (HTTP)
     │
     ▼
  S3 put (AES-256, SHA-256, versioned)
     │
     ▼
  SQS message (cms-ingest-queue)
     │
     ▼  IngestSqsListener starts workflow
 ┌───────────────────────────────────────────────┐
 │ DocumentIngestWorkflow                        │
 │   virusScan                → (stub: ClamAV)   │
 │   convertToPdfIfNeeded     → LibreOffice      │
 │   extractText              → PDFBox or OCR    │
 │   indexDocument            → Elasticsearch    │
 │   markStatus INDEXED                          │
 └───────────────────────────────────────────────┘
```

Each activity is retried independently with exponential backoff and a heartbeat timeout. The `SerialDocument.processingStatus` field is updated at each boundary so the UI can show pipeline progress.

## Storage Layout

**Primary S3 key format:** `cases/{caseId}/{documentId}/{sanitized-filename}`
**Derived artifacts (converted PDFs, OCR text):** `cases/{caseId}/{documentId}/derived/{name}`

This grouping keeps every artifact for one case discoverable by prefix and makes bucket-level access policies easy to write.

## Search Model

Each `SerialDocument` is denormalized into a single Elasticsearch document that carries both the document fields and the parent case's filter facets (classification, office, status). The custom `english_case_analyzer` applies lowercasing, ASCII folding, English stop-word removal, and Porter stemming. Query execution uses `multi_match` with `best_fields` + `AUTO` fuzziness and returns highlighted snippets on the `body` and `title` fields, plus aggregations over classifications, offices, and document types.

## Audit Model

The `audit_events` table is append-only — a BEFORE UPDATE / DELETE trigger raises an exception if anyone tries to mutate a row. All writes go through `AuditService.record(...)` running in `REQUIRES_NEW`, so audit persistence is decoupled from the business transaction: even if the business operation rolls back, the audit row is committed. The schema captures actor identity (username, roles, IP), entity identifiers, the action, an outcome string, and a JSONB `details` payload for before/after diffs.

## Role Model

| Role | Key privileges |
|---|---|
| `SYSTEM_ADMIN` | Full administrative access, audit read, can impersonate roles |
| `SUPERVISOR` | Approve/reject cases, confirm closure, reassign work, suspend/resume |
| `AGENT` | Create/update cases, file serial documents, submit for approval, request closure |
| `RECORDS_CLERK` | Index/file documents into CRS, view cases |
| `ANALYST` | Read-only + full search/analytics access |
| `AUDITOR` | Read-only access to audit log, cannot see case content directly |

Roles are enforced at the HTTP boundary by `@PreAuthorize` on every controller method and carried end-to-end in the Keycloak JWT under `realm_access.roles`.

## Deployment Topology

Single-node dev uses Docker Compose. For production:

* Run Spring Boot behind an ALB with TLS termination; horizontally scale the API stateless behind it.
* Run a separate Temporal worker deployment — the API and workers can scale independently.
* Use managed services (Amazon RDS Postgres, Amazon OpenSearch, Amazon MSK or SQS, Amazon S3 + KMS-CMK) rather than container equivalents.
* Terminate Keycloak (or a managed IdP equivalent such as PingFederate) in a hardened network segment; the API only needs JWKS reachability.
