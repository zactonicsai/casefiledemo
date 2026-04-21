# Demo Only-Style Case Management System

A reference implementation inspired by the FBI's Demo Only system for numbered case files (e.g., `111-HQ-12345`) containing serial documents such as FD-302 interviews, evidence logs, photographs, and investigative reports. Digital from inception through closure with full audit trail, RBAC, OCR, full-text search, and workflow approvals.

> **Disclaimer:** This is an unclassified reference architecture for demonstration and training purposes. It is NOT a CJIS-authorized system and does NOT connect to any FBI system. Deploying a real CJIS-compliant system requires an ATO (Authority to Operate), FIPS 140-2/140-3 validated modules, physical facility controls, personnel screening, and formal accreditation.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  Angular 17 SPA  ──►  NGINX  ──►  Spring Boot 3.5 (Java 21)     │
│       │                                   │                     │
│       └── Keycloak OIDC ──────────────────┤                     │
│                                           ▼                     │
│  ┌────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │ PostgreSQL │  │ Elasticsearch│  │  Temporal    │             │
│  │ (case DB)  │  │ (full-text)  │  │  (workflow)  │             │
│  └────────────┘  └──────────────┘  └──────────────┘             │
│                                                                 │
│  ┌────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │  LocalStack│  │   Tesseract  │  │  LibreOffice │             │
│  │  (S3/SQS)  │  │    (OCR)     │  │  (convert)   │             │
│  └────────────┘  └──────────────┘  └──────────────┘             │
└─────────────────────────────────────────────────────────────────┘
```

## Technology Stack

| Layer | Technology |
|---|---|
| Frontend | Angular 17, Angular Material, RxJS, NgRx |
| API | Java 21, Spring Boot 3.5.x, Spring Security 6, Spring Data JPA |
| AuthN/AuthZ | Keycloak 24 (OIDC), JWT bearer tokens, role hierarchy |
| Primary DB | PostgreSQL 16 (case metadata, audit log, users) |
| Search | Elasticsearch 8.x (document content, case full-text index) |
| Object Storage | AWS S3 (LocalStack for dev) — document binaries |
| Messaging | AWS SQS (LocalStack for dev) — ingestion pipeline events |
| Workflow | Temporal.io — case lifecycle, approvals, review chains |
| OCR | Tesseract 5 (via tess4j) |
| Conversion | LibreOffice headless (.doc/.docx/.xls → PDF), pdfbox |
| Orchestration | Docker Compose (single-node dev profile) |

## Case File Model

A case file uses FBI-style numbering: `<CLASSIFICATION>-<OFFICE>-<SERIAL>` (e.g., `111-HQ-12345`).

- **Classification** — 3-digit case classification code (e.g., `111` = Violent Crime)
- **Office** — originating field office (`HQ`, `NY`, `LA`, `WF`, etc.)
- **Serial** — sequential number assigned by the originating office

Each case contains **serial documents** (numbered 1, 2, 3…) of types:
- `FD-302` — interview report
- `EC` — electronic communication
- `LHM` — letterhead memorandum
- `EVIDENCE_LOG` — chain-of-custody entry
- `PHOTO` — photographic evidence
- `NOTE` — investigator notes
- `OTHER` — arbitrary attachment

## Quick Start

```bash
# 1. Build and start everything
docker compose up -d --build

# 2. Wait for health checks (~90s on first run)
docker compose ps

# 3. Seed Keycloak realm + demo users
./infra/init-scripts/seed-keycloak.sh

# 4. Open the application
open http://localhost:4200
```

**Default credentials (dev only):**
- `sa.admin / Password1!` — System administrator
- `agent.smith / Password1!` — Special Agent
- `supervisor.jones / Password1!` — Supervisory Special Agent
- `analyst.doe / Password1!` — Intelligence Analyst

## Directory Layout

```
.
├── backend/              Spring Boot 3.5 API (Java 21)
├── frontend/             Angular 17 SPA
├── docker/               Per-service Dockerfiles & configs
├── infra/                Infrastructure bootstrapping (LocalStack, Keycloak realm)
├── docs/                 Architecture, security model, data dictionary
└── docker-compose.yml    Single-command local stack
```

See `docs/ARCHITECTURE.md` for deeper details.
