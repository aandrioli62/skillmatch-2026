# CLAUDE.md: SkillMatch Project Instructions

## Project Overview

SkillMatch is a microservice-based web application for an exam in "Progettazione di Architetture di Servizi" (University of Salento, Prof. Luca Mainetti, A.Y. 2025/26). It connects professionals with companies for short-term micro-projects (training, consulting, prototyping).

**Deadline**: August 2026.

## Architecture Summary

- **Pattern**: Microservice Architecture with a mix of Client-Server, REST, Pub/Sub, API Gateway, Database per Service, Circuit Breaker, 3-Tier per microservice.
- **Backend**: Java 21 + Spring Boot 3.x (each microservice is a separate Spring Boot app).
- **Frontend**: React (SPA, responsive).
- **Auth**: Keycloak (OAuth 2.0 / OIDC). Realm `skillmatch`. Flows: Authorization Code + PKCE (SPA), Client Credentials (service-to-service).
- **API Gateway**: Spring Cloud Gateway (port 8080). Validates JWT, routes requests, implements Circuit Breaker (Resilience4j).
- **Messaging**: RabbitMQ with topic exchange `skillmatch.events`.
- **Databases**: PostgreSQL (single instance, 6 logical databases: user, project, contract, payment, feedback, identity) + MongoDB for Notification Service. Each microservice owns its own logical DB (Database per Service pattern).
- **Containerization**: Docker (multi-stage Dockerfile per service, frontend included). All images built for `linux/arm64`.
- **Orchestration**: K3s (lightweight Kubernetes, CNCF certified) on Oracle Cloud ARM VM.
- **Container Registry**: GitHub Container Registry (ghcr.io), free.
- **CI/CD**: GitHub Actions, build, test, Docker buildx (arm64), push to GHCR, SSH deploy to Oracle VM via kubectl.
- **Cloud**: Oracle Cloud Infrastructure Always Free, Ampere A1 VM (4 OCPU, 24 GB RAM, 200 GB storage). Permanent, no expiry.
- **Observability**: Spring Boot Actuator + Prometheus + Grafana (metrics), Loki + Promtail (logs).
- **12-Factor App**: Strictly followed. Config via env vars/ConfigMap/Secrets. Stateless processes. Logs to stdout. Flyway for DB migrations. Dev/prod parity via Docker Compose locally.
- **Self-service registration**: `POST /api/v1/auth/register` on User Service provisions the Keycloak identity (via Keycloak's Admin REST API), the local user record and the minimal role-specific profile in one call. See `docs/adr/ADR-007-self-service-registration.md`.
- **Quality gates**: User Service and Project Service (the two core services chosen for the Service Architecture deliverable) enforce JaCoCo (minimum 90% line coverage) and PMD (cyclomatic complexity ≤ 10 per method) at Maven's `verify` phase.
- **Transactional email**: Notification Service sends real emails (not just in-app notifications) for a deliberately small set of events (`user.validated`, `candidature.accepted`, `payment.completed`) via a Brevo SMTP relay, using a dedicated Keycloak Client Credentials client (`notification-service`) to resolve the recipient's email from User Service outside any HTTP request context.

## Cloud-Agnostic Design

The architecture is fully cloud-agnostic. Docker containers and Kubernetes manifests work identically on AKS (Azure), EKS (AWS), GKE (Google), or any CNCF-compliant Kubernetes. Oracle Cloud with K3s is used because its Always Free tier provides sufficient resources (24 GB RAM). Migrating requires only changing the infrastructure layer: zero code changes.

## Microservices

| Service | Port | DB | Type |
|---------|------|----|------|
| API Gateway | 8080 | n/d | Spring Cloud Gateway |
| User Service | 8081 | user-db (PostgreSQL) | Spring Boot |
| Project Service | 8082 | project-db (PostgreSQL) | Spring Boot |
| Contract Service | 8083 | contract-db (PostgreSQL) | Spring Boot |
| Payment Service | 8084 | payment-db (PostgreSQL) | Spring Boot |
| Feedback Service | 8085 | feedback-db (PostgreSQL) | Spring Boot |
| Notification Service | 8086 | notification-db (MongoDB) | Spring Boot |
| Keycloak (Identity) | 8180 | identity-db (PostgreSQL) | Keycloak |
| RabbitMQ | 5672 (AMQP), 15672 (mgmt) | n/d | RabbitMQ |

All PostgreSQL databases are logical databases on a single PostgreSQL instance (saves RAM). Logically they are separate per the Database per Service pattern.

## Roles

- `PROFESSIONAL`: self-registers (`POST /api/v1/auth/register`), completes profile (skills from a shared catalog, portfolio, payment account), applies to projects, receives payment and feedback, can file a report against a company.
- `COMPANY`: self-registers, publishes projects, selects candidates (or rejects individual candidatures), signs micro-contracts, pays via platform, receives invoice, can file a report against a professional.
- `ADMIN`: validates professional and company registrations, configures commission rate, monitors transactions (with filters and aggregate totals), reviews and closes reports, handles suspensions (reversible) and permanent deactivation (irreversible, also disables the Keycloak identity).

## Key Business Rules

1. **Account validation**: Both professionals and companies must be validated by an admin before they can act: a professional before applying to projects, a company before creating (publishing) a project. The same admin endpoint (`POST /api/v1/admin/users/{userId}/validate`) validates either role.
2. **Commission**: The platform retains a configurable commission (default 8%) on each payment. The admin can change this.
3. **Invoice**: A single invoice is generated for the company, including both the professional's fee and the platform commission.
4. **Reputation levels**: Calculated from aggregated feedback ratings, professionals only (companies have no reputation score):
   - Junior: avg < 3.5 OR total reviews < 3
   - Affidabile: avg >= 3.5 AND total reviews >= 3
   - Top Performer: avg >= 4.5 AND total reviews >= 10
5. **Mutual feedback**: Both company and professional leave feedback (1-5 scale + optional comment) after project completion and payment.
6. **Candidature selection**: Accepting a candidature automatically rejects every other still-`PENDING` candidature on the same project and moves the project to `ASSIGNED`. A company can also reject a single candidature explicitly without accepting anyone yet; this does not affect the project's status or the other candidatures.
7. **Reports**: Either party of a collaboration can file a report against the counterparty. Filing or closing a report is purely informational bookkeeping; it never suspends anyone automatically, suspension remains the separate admin action on the user. Any authenticated professional or company can view the reports filed against a given user id (`GET /api/v1/users/{userId}/reports`), not just admins; there is no restriction to counterparties of an actual shared contract.
8. **Suspension vs. deactivation**: `SUSPENDED` is a reversible, temporary hold; the Keycloak identity stays enabled (no code path re-enables it via the API, but nothing at the identity layer blocks it either). `DEACTIVATED` (`POST /api/v1/admin/users/{userId}/deactivate`) is permanent: it also disables the Keycloak account, so the user can never log in again. The user row itself is kept so contracts/payments/feedback tied to that id stay resolvable. Deactivated accounts are excluded from `GET /api/v1/admin/users`.
9. **Live commission rate for non-admins**: `GET /api/v1/commission-config/current` is open to any authenticated caller (companies and professionals), unlike `GET /api/v1/admin/commission-config`. It exists so a contract estimate shown before payment always matches the rate that will actually be charged at payment time.

## Event Flow (RabbitMQ)

All events go through a topic exchange `skillmatch.events`. Routing keys follow the pattern `<domain>.<action>` (e.g., `project.published`, `payment.completed`).

Key flows:
- `project.published` → Notification Service (confirms publication to the company; professionals discover open projects by browsing, not via a push notification)
- `candidature.submitted` → Notification Service (notify the owning company)
- `candidature.accepted` → Contract Service (create micro-contract) + Notification Service
- `project.completed` → Notification Service only. No service reacts automatically: completing the contract (`PUT /api/v1/contracts/{id}/complete`) and initiating the payment (`POST /api/v1/payments`) remain explicit REST actions taken by the company, not event-driven.
- `payment.completed` → Feedback Service (enable reviews) + Notification Service
- `feedback.aggregated` → User Service (recalculate reputation) + Notification Service

The Notification Service actually binds its queue to `#` (every routing key on the exchange), not to each event individually; the table above lists which events currently have a dedicated template/consumer, not literal per-event bindings. See `docs/events.md` for the full, code-verified map (routing keys, queues, payloads).

## Internal Microservice Structure (3-Tier)

Every microservice follows the 3-Tier architecture:

```
controller/  → REST Controllers (@RestController), DTOs, input validation
service/     → Business logic (@Service), event publishing, domain rules
repository/  → Data access (@Repository, JPA), entities, Flyway migrations
```

Additional packages: `config/`, `dto/`, `model/`, `event/`, `exception/`.

## Coding Conventions

- Java 21, Spring Boot 3.x.
- Package naming: `com.skillmatch.<service-name>`.
- REST endpoints versioned: `/api/v1/...`.
- DTOs for request/response (never expose JPA entities directly).
- Validation with `@Valid` and Jakarta Bean Validation annotations.
- Global exception handling with `@RestControllerAdvice`.
- OpenAPI/Swagger documentation via `springdoc-openapi`.
- Database migrations with Flyway (SQL scripts in `src/main/resources/db/migration/`).
- Tests: JUnit 5 + Mockito (unit), @SpringBootTest + Testcontainers (integration).
- Logging: SLF4J, structured JSON format. No log files: stdout only.
- JVM tuning for containers: `-Xmx256m -Xms128m` per service.

## Docker & Kubernetes

- Each service has its own `Dockerfile` inside `services/<name>/` (the frontend too, `frontend/Dockerfile`, static build served by Nginx). Multi-stage builds targeting `linux/arm64`.
- Local development: `cd infra && docker compose up` starts everything.
- Production: K3s on Oracle Cloud VM with manifests from `infra/k8s/`, applied via `infra/scripts/deploy-k8s.sh`.
- K8s resources per service: Deployment, Service (ClusterIP), ConfigMap, Secret. Java services also carry a `startupProbe` (Postgres + JPA startup takes ~80s, longer than a plain `livenessProbe` tolerates).
- API Gateway and frontend exposed via K3s Ingress **Traefik** (K3s's built-in default, nothing extra installed). Two separate hosts on a shared `nip.io` domain: the app (`/` → frontend, `/api` → API Gateway) and a dedicated `keycloak.<ip>.nip.io` host for Keycloak itself, each with its own Let's Encrypt certificate.
- TLS via Let's Encrypt + cert-manager. Not optional in production: the Authorization Code + PKCE flow needs the browser's Web Crypto API, only available in a secure context.
- Service discovery via Kubernetes DNS (no Eureka needed).
- Oracle Cloud Security List: only ports 80, 443, 22 open.
- Docker Compose `build` context paths reference `../services/<name>/` from `infra/`.

## Repository Structure (Monorepo)

Single repository `skillmatch` with folder-per-service:

```
skillmatch/
├── services/
│   ├── api-gateway/              # Spring Cloud Gateway (port 8080)
│   ├── user-service/             # Spring Boot (port 8081)
│   ├── project-service/          # Spring Boot (port 8082)
│   ├── contract-service/         # Spring Boot (port 8083)
│   ├── payment-service/          # Spring Boot (port 8084)
│   ├── feedback-service/         # Spring Boot (port 8085)
│   └── notification-service/     # Spring Boot (port 8086)
├── frontend/                     # React SPA
├── infra/
│   ├── docker-compose.yml
│   ├── init-databases.sql
│   ├── keycloak/                 # Realm export JSON
│   ├── k8s/                      # Kubernetes manifests for K3s
│   └── scripts/                  # VM setup, deploy helpers
├── docs/
│   ├── architecture.md
│   ├── events.md                 # RabbitMQ event map (routing keys, queues)
│   ├── deployment.md             # K3s deployment diagram
│   ├── development-process.md    # Actual development process, AI-assisted methodology
│   ├── adr/                      # Architecture Decision Records
│   ├── use-cases/
│   ├── er-diagrams/
│   ├── sequence-diagrams/
│   └── three-layer/              # 3-layer diagrams + quality metrics, core services only
├── .github/
│   └── workflows/                # One workflow per service, path-filtered
├── CLAUDE.md
├── CLAUDE-SERVICES.md
└── README.md
```

Each service is a standalone Spring Boot project with its own `pom.xml`, `Dockerfile`, `application.yml`. The monorepo simplifies management for a solo developer without sacrificing microservice independence. GitHub Actions uses **path filters** to build/deploy only the changed service.

## CI/CD Pipeline (GitHub Actions: Monorepo)

Each service has its own workflow file (e.g., `.github/workflows/user-service.yml`) triggered only when files in `services/user-service/**` change:

1. On push/PR to `main` touching `services/<name>/**`: build with Maven, run unit tests.
2. On merge to `main`: Docker buildx (linux/arm64), push to `ghcr.io/<user>/skillmatch/<service>:<sha>`.
3. SSH into Oracle Cloud VM, `kubectl set image` for that specific service.

GitHub Actions is free and unlimited on public repos. With GitHub Student Pack: 3,000 min/month on private repos.

## Design Patterns Reference (from course theory)

Explicitly used and to be documented:
- **Microservice Architecture**: overall system decomposition
- **Client-Server**: React ↔ API Gateway
- **REST**: all inter-service communication via HTTP, stateless
- **API Gateway**: single entry point (Spring Cloud Gateway)
- **Pub/Sub**: event-driven async communication (RabbitMQ topic exchange)
- **Database per Service**: each service owns its data store
- **Circuit Breaker**: resilience via Resilience4j
- **3-Tier Architecture**: internal structure of each microservice (Presentation/Business/Data)
- **Externalized Configuration**: config via env vars, ConfigMap, Secrets (12-Factor III)

## Deliverables Checklist

- [x] Code repository with CI/CD (GitHub + GitHub Actions), deployed and reachable over HTTPS on Oracle Cloud
- [x] Technical documentation (ADR archive, `docs/adr/`, 7 ADRs)
- [x] High-Level Architecture overview (`docs/architecture.md`, `docs/events.md`, `docs/deployment.md`)
- [x] Use cases: at least 3 per stakeholder (`docs/use-cases/`, Professional, Company, Admin)
- [x] Service Architecture for User Service and Project Service (`docs/three-layer/`, `docs/er-diagrams/`, JaCoCo coverage + PMD complexity gates as metrics)
- [ ] Interactive demo
- [ ] Development process considerations (sprint backlog, burndown chart: optional)

When new features land, keep this file (and `docs/`) in sync in the same session, or flag the drift explicitly: recent examples of drift this file has had to catch up on include self-service registration, candidature rejection, reports, real transactional email, and the JaCoCo/PMD quality gates.

## How to Collaborate

- **Stop before looping**: If you encounter a problem you cannot solve immediately, do NOT enter a try→fail→retry cycle. Stop, give a brief recap of what was tried and why it failed, list the proposed next steps, and wait for confirmation before proceeding. This lets the user catch derailments early.
- **Never write generated files from memory**: Some files are meant to be produced by official tools (e.g., `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` by `mvn wrapper:wrapper`; `package-lock.json` by `npm install`). Never hand-craft these from memory: they must be generated by the actual tool. Only write a file manually when no official generator exists for it.

## When Generating Code

- All service code lives under `services/<service-name>/`. Never place service code at repo root.
- Always include proper Spring Security configuration (Resource Server with JWT).
- Always add RabbitMQ listener/publisher configuration where needed.
- Always create DTOs: never return entities from controllers.
- Always add Flyway migration scripts for any schema change.
- Always add `@Valid` on request bodies.
- Always add health check endpoints (Spring Boot Actuator is enough).
- Always use `application.yml` with profiles (default, dev, prod).
- Use `${ENV_VAR:default}` syntax for 12-Factor config externalization.
- When writing Dockerfiles, use multi-stage builds targeting `linux/arm64`. Dockerfile lives in `services/<n>/`.
- When writing K8s manifests, place them in `infra/k8s/`. Include resource limits, health probes, ConfigMaps.
- JVM flags: always set `-Xmx256m -Xms128m` in ENTRYPOINT for container efficiency.
- PostgreSQL: all services connect to the same PostgreSQL host but different logical databases.
- Docker Compose is in `infra/docker-compose.yml`. Build contexts point to `../services/<n>/`.
