# Security Model

> **Disclaimer:** This is an UNCLASSIFIED reference implementation. It is NOT a CJIS-authorized system and does NOT meet the policy, personnel, physical, or cryptographic requirements to handle Criminal Justice Information. The controls below illustrate the *design approach* a compliant system would take, but operating such a system requires formal accreditation (ATO), FIPS-validated cryptographic modules, physical facility controls, NCIC background checks, and active CJIS Security Policy auditing.

## Authentication

* **Identity Provider:** Keycloak 24 configured with PKCE-only Authorization Code flow for the SPA.
* **Token Format:** JWT access tokens signed by the realm; Spring Boot validates signatures against the realm's JWKS endpoint.
* **Token Lifetime:** 15-minute access tokens with silent refresh; 8-hour max SSO session.
* **Brute-force protection:** Realm-level lockout after 5 failures, 15-minute wait.
* **MFA:** Not enabled in the dev realm but easily added via Keycloak's built-in OTP / WebAuthn providers — a CJIS-compliant deployment MUST enable advanced authentication per CJIS §5.6.

## Authorization

* **RBAC:** Six roles (`SYSTEM_ADMIN`, `SUPERVISOR`, `AGENT`, `ANALYST`, `RECORDS_CLERK`, `AUDITOR`), carried in the JWT under `realm_access.roles` and mapped to Spring authorities.
* **Enforcement:** `@PreAuthorize` on every controller method. No endpoint is unprotected except `/actuator/health`, `/actuator/info`, and the OpenAPI docs.
* **Method-level granularity:** Transitions such as approve/reject require `SUPERVISOR` or `SYSTEM_ADMIN`; case creation requires `AGENT` or higher; audit log access requires `AUDITOR` or `SYSTEM_ADMIN`.

## Data Protection

| Data at… | Protection |
|---|---|
| **Rest — S3** | Server-side encryption (AES-256) on every object. Versioning enabled. Public access blocked at bucket level. Production should use SSE-KMS with a CMK. |
| **Rest — Postgres** | Encryption delegated to the storage layer (RDS at-rest encryption / LUKS on self-managed). Sensitive columns (not modeled here) should use pgcrypto or application-level envelope encryption. |
| **Rest — Elasticsearch** | In dev, `xpack.security.enabled=false`. Production MUST enable the security plugin, TLS between nodes, and encrypted snapshots. |
| **In transit** | TLS everywhere in production (terminated at ingress). Temporal supports mTLS on the gRPC frontend. |
| **In processing** | SHA-256 digest is calculated on every uploaded binary at the moment of storage and preserved for chain-of-custody verification. |

## Audit Trail

The `audit_events` table is append-only:

* A Postgres trigger (`reject_audit_mutation`) raises an exception on any UPDATE or DELETE.
* Every write runs in a `REQUIRES_NEW` transaction so audit rows are not lost if the business transaction rolls back.
* Every row records: actor username + roles + IP, action name, entity type/id, case number, outcome, a JSONB details payload, the X-Request-Id correlation identifier, and a high-precision timestamp.
* Default retention: 7 years (`app.audit.retention-days=2555`), aligned with typical federal records schedules. A production deployment would additionally ship events to a WORM (write-once) sink such as S3 Object Lock or a hardware-attested log service.

## Input Validation & Injection Controls

* Bean-validation annotations on all request DTOs (`@NotBlank`, `@Size`, `@Pattern`).
* JPA queries are parameterized via Spring Data; no dynamic string concatenation.
* Elasticsearch queries use the typed Java API (`BoolQuery.Builder`) — user input never concatenates into a query string.
* File uploads cap at 250 MB by default (`spring.servlet.multipart.max-file-size`) and filenames are sanitized before being used as S3 keys or on-disk paths.

## HTTP Hardening

Configured in `SecurityConfig`:

* `Content-Security-Policy: default-src 'self'; frame-ancestors 'none'; object-src 'none'` — prevents clickjacking and most XSS vectors.
* `Referrer-Policy: no-referrer`
* `Strict-Transport-Security: max-age=31536000; includeSubDomains`
* `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff` (set by the nginx layer).
* Stateless session policy — no cookies, so there is no CSRF surface on the API.

## Secret Management

The dev stack ships secrets as environment variables in `docker-compose.yml`. Any non-dev deployment should source these from Secrets Manager / Vault / SSM Parameter Store and inject them at runtime. Keycloak admin credentials, the database password, and AWS keys are the obvious candidates.

## Dependencies & Supply Chain

* The backend pins Spring Boot 3.5.13 (Java 21 LTS target) and AWS SDK v2.
* No runtime `SNAPSHOT` dependencies.
* Production builds should run a dependency vulnerability scanner (OWASP Dependency-Check, Snyk, or Grype) as part of CI.

## What This Reference Does NOT Provide

These are explicitly out-of-scope for this reference and would require significant additional work:

* FIPS 140-2/140-3 validated cryptographic modules
* DoD PKI integration (CAC/PIV)
* FedRAMP / CJIS personnel screening workflows
* Physical facility controls (locked rooms, escort procedures)
* CUI/SBU data marking and dissemination controls
* Cross-domain solutions for classified networks
* Tamper-evident hardware-backed audit logs
* ATO (Authority to Operate) documentation
