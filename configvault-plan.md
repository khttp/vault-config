# ConfigVault: Project Plan

A centralized service to store, version and deliver configuration and secrets for multiple projects and environments, so deployments stop depending on scattered `.env` files.

> **Assumption:** I read "multi-project deployment" as: many services, each with dev/staging/prod, each needing its own config and secrets, today managed by hand. If your problem is different, tell me and I'll adjust the scope.

---

## 1. Review of the blueprint

The blueprint is a good starting sketch, but it would not hold up as built. These are the things to fix before writing code.

| # | Issue in blueprint | Why it matters | Fix |
|---|---|---|---|
| 1 | **No environment dimension.** Config is keyed only by `(project, key)`. | The core problem is the same service having different values in dev/staging/prod. | Add an `environments` table; key config by `(environment, key)`. |
| 2 | **"Cryptographically sealed" is only a comment.** No design for keys. | Encryption without key management is theater. | Envelope encryption: AES-256-GCM per value, master key from env/KMS, key id stored per row. |
| 3 | **Hardcoded DB password in compose**, DB port 5432 published to the host. | A secrets manager that leaks its own secrets is a bad demo. | `.env` file (git-ignored) + compose `env_file`; don't publish 5432 outside dev. |
| 4 | **No versioning or audit log.** | Config changes cause outages; you need history and rollback. | `config_versions` table (append-only) + `audit_log`. |
| 5 | **No authentication or authorization.** | Anyone could read every secret. | Admin login for the UI; scoped API tokens per project/environment for services. |
| 6 | **No way for a deployed service to consume config.** | The product is useless unless a service can fetch its config safely. | Read-only token endpoint + a small CLI or SDK. |
| 7 | **Java + Lombok chosen "for parity with Kotlin".** | Your real strength is Kotlin/Spring Boot, so the justification doesn't apply. | Use Kotlin. Data classes replace Lombok. |
| 8 | **Schema managed by JPA auto-DDL implied; no migrations.** | Not reproducible. | Flyway migrations. |
| 9 | `createdAt` defaults to `ZonedDateTime.now()` in the entity. | Time should come from the DB, and be UTC. | Use `Instant`, DB default, `timestamptz`. |
| 10 | Compose file has broken indentation and obsolete `version:` key. | It won't run as written. | Rewrite; add healthchecks and `depends_on: condition: service_healthy`. |

---

## 2. Scope

### MVP (must ship)
- Projects and environments (CRUD)
- Config entries per environment; secret values encrypted at rest, masked in responses
- Version history per entry, rollback to a previous version
- Audit log (who changed what, when)
- Service-facing endpoint: fetch the full config for one project and environment using a scoped token
- Admin authentication, API token management
- Docker Compose setup that runs end to end with one command
- Tests (unit + integration with Testcontainers) and CI

### Nice to have (only after MVP)
- Minimal web dashboard (React) for browsing and editing
- Diff view between environments ("what's different in prod vs staging?")
- Import/export `.env` files
- CLI: `configvault pull --project x --env prod > .env`
- Config change notifications (Slack webhook)

### Out of scope (say so in the README)
- Dynamic secrets, HSM/KMS integration, multi-tenant orgs, high-availability clustering, replacing Vault/AWS Secrets Manager

---

## 3. Domain model

```
projects        (id, name unique, description, created_at)
environments    (id, project_id -> projects, name, created_at; unique(project_id, name))
config_entries  (id, environment_id -> environments, key, is_secret,
                 current_version_id, created_at; unique(environment_id, key))
config_versions (id, entry_id -> config_entries, version_no, value_ciphertext,
                 key_id, nonce, created_by, created_at)      -- append-only
api_tokens      (id, project_id, environment_id, token_hash, scope(read), created_at, revoked_at)
audit_log       (id, actor, action, target_type, target_id, metadata jsonb, created_at)
```

Notes:
- Store only a hash of API tokens, and show the plain token once at creation.
- Non-secret values can be stored in plaintext in the same column path, or encrypted too for simplicity. Pick one and document it.
- Never return secret values in list endpoints; require an explicit "reveal" call that writes an audit entry.

---

## 4. Architecture

- **Backend:** Kotlin + Spring Boot (Web, Security, Data JPA), PostgreSQL 16, Flyway
- **Crypto:** AES-256-GCM, random nonce per value, master key loaded from an environment variable (documented upgrade path to a KMS)
- **Auth:** admin login (JWT or session) + hashed, scoped API tokens for services
- **API docs:** OpenAPI (springdoc)
- **Frontend (optional, later):** React + Vite, served by nginx
- **Ops:** Docker multi-stage build for the backend, Compose with healthchecks, GitHub Actions CI

### API sketch
```
POST   /api/projects
POST   /api/projects/{p}/environments
PUT    /api/environments/{e}/config/{key}        # creates a new version
GET    /api/environments/{e}/config              # masked for secrets
POST   /api/environments/{e}/config/{key}/reveal # audited
POST   /api/environments/{e}/config/{key}/rollback?toVersion=3
GET    /api/environments/{e}/audit
POST   /api/tokens                               # returns plain token once
GET    /v1/config   (Authorization: Bearer <token>)   # full config for that token's env
```

---

## 5. Key design decisions to write down (short ADRs)

1. **Envelope encryption vs. a single key.** Start with one master key + per-row nonce; record `key_id` so rotation is possible later.
2. **Append-only versions.** Updates never overwrite; the "current" pointer moves. This makes rollback and audit trivial.
3. **Token scope.** One token = one project + one environment, read-only.
4. **Masking by default.** Secrets are never in logs, list responses, or error messages.
5. **Why not just use Vault/AWS Secrets Manager?** Be honest in the README: this is a learning/portfolio project, and it targets small teams that want a simple self-hosted option.

---

## 6. Build plan (about 4 weeks, ~8-10 hrs/week alongside your job search)

### Week 1: Foundation
- Repo, Kotlin + Spring Boot skeleton, Docker Compose with Postgres (secrets via `.env`)
- Flyway migrations for the full schema
- CRUD for projects and environments, with tests
- CI pipeline (build + tests)

### Week 2: Core value
- Config entries + versions, encryption service (AES-GCM) with unit tests
- Masking, reveal endpoint, rollback
- Audit logging on every write and reveal

### Week 3: Security and consumption
- Admin auth, API tokens (hashed, scoped), `/v1/config` endpoint
- Rate limiting on token endpoints, input validation, error handling
- Integration tests with Testcontainers (including "token for env A cannot read env B")

### Week 4: Polish and show-off
- OpenAPI docs, README with architecture diagram and threat model notes
- Small CLI or `curl` demo script; optional dashboard
- Demo: two sample services pulling different configs per environment
- Record a 2-minute demo GIF/video

**Definition of done for the MVP:** `docker compose up` starts everything; a service can fetch its prod config with a token; a secret is unreadable in the DB dump; a change can be rolled back; the audit log shows every action; CI is green.

---

## 7. Testing and quality
- Unit: encryption round-trips, wrong key/nonce fails, token hashing
- Integration: Testcontainers Postgres, full API flows, authorization boundaries
- Security checks: no secrets in logs or responses, SQL injection and IDOR attempts (token for another project)
- Keep a short `SECURITY.md` listing limitations (master key handling, no HA)

---

## 8. How this helps your job search
- It shows the exact topics mid-level interviews ask about: schema design, auth, encryption, idempotent updates, Docker, testing, CI.
- Put a clear README at the top: problem, architecture diagram, how to run, design trade-offs, what you'd do next.
- Pin the repo on GitHub and link it on your resume under Projects.
- Prepare a 2-minute explanation: "I built X to solve Y. The hardest part was Z, and I chose A over B because..."

---

## 9. Risks
| Risk | Mitigation |
|---|---|
| Scope creep (dashboard, CLI, notifications) | Freeze MVP scope; do extras only after the definition of done |
| Time lost on the UI | Ship the API first; the UI is optional |
| Crypto done wrong | Use the JDK's standard AES-GCM API, never invent your own scheme, unit-test it |
| Never finishing | Deploy a demo after week 2, even if rough |
