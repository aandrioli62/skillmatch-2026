# CLAUDE.md — SkillMatch Project Instructions

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
- **Databases**: PostgreSQL (single instance, 7 logical databases) + MongoDB for Notification Service. Each microservice owns its own logical DB (Database per Service pattern).
- **Containerization**: Docker (multi-stage Dockerfile per service). All images built for `linux/arm64`.
- **Orchestration**: K3s (lightweight Kubernetes, CNCF certified) on Oracle Cloud ARM VM.
- **Container Registry**: GitHub Container Registry (ghcr.io) — free.
- **CI/CD**: GitHub Actions → build → test → Docker buildx (arm64) → push to GHCR → SSH deploy to Oracle VM via kubectl.
- **Cloud**: Oracle Cloud Infrastructure Always Free — Ampere A1 VM (4 OCPU, 24 GB RAM, 200 GB storage). Permanent, no expiry.
- **Observability**: Spring Boot Actuator + Prometheus + Grafana (metrics), Loki + Promtail (logs).
- **12-Factor App**: Strictly followed. Config via env vars/ConfigMap/Secrets. Stateless processes. Logs to stdout. Flyway for DB migrations. Dev/prod parity via Docker Compose locally.

## Cloud-Agnostic Design

The architecture is fully cloud-agnostic. Docker containers and Kubernetes manifests work identically on AKS (Azure), EKS (AWS), GKE (Google), or any CNCF-compliant Kubernetes. Oracle Cloud with K3s is used because its Always Free tier provides sufficient resources (24 GB RAM). Migrating requires only changing the infrastructure layer — zero code changes.

## Microservices

| Service | Port | DB | Type |
|---------|------|----|------|
| API Gateway | 8080 | — | Spring Cloud Gateway |
| User Service | 8081 | user-db (PostgreSQL) | Spring Boot |
| Project Service | 8082 | project-db (PostgreSQL) | Spring Boot |
| Contract Service | 8083 | contract-db (PostgreSQL) | Spring Boot |
| Payment Service | 8084 | payment-db (PostgreSQL) | Spring Boot |
| Feedback Service | 8085 | feedback-db (PostgreSQL) | Spring Boot |
| Notification Service | 8086 | notification-db (MongoDB) | Spring Boot |
| Keycloak (Identity) | 8180 | identity-db (PostgreSQL) | Keycloak |
| RabbitMQ | 5672 (AMQP), 15672 (mgmt) | — | RabbitMQ |

All PostgreSQL databases are logical databases on a single PostgreSQL instance (saves RAM). Logically they are separate per the Database per Service pattern.

## Roles

- `PROFESSIONAL` — registers, completes profile (skills, portfolio, payment account), applies to projects, receives payment and feedback.
- `COMPANY` — registers, publishes projects, selects candidates, signs micro-contracts, pays via platform, receives invoice.
- `ADMIN` — validates professional registrations, configures commission rate, monitors transactions, handles reports/suspensions.

## Key Business Rules

1. **Professional validation**: Professionals must be validated by an admin before they can apply to projects.
2. **Commission**: The platform retains a configurable commission (default 8%) on each payment. The admin can change this.
3. **Invoice**: A single invoice is generated for the company, including both the professional's fee and the platform commission.
4. **Reputation levels**: Calculated from aggregated feedback ratings:
   - Junior: avg < 3.5 OR total reviews < 3
   - Affidabile: avg >= 3.5 AND total reviews >= 3
   - Top Performer: avg >= 4.5 AND total reviews >= 10
5. **Mutual feedback**: Both company and professional leave feedback (1-5 scale + optional comment) after project completion and payment.

## Event Flow (RabbitMQ)

All events go through a topic exchange `skillmatch.events`. Routing keys follow the pattern `<domain>.<action>` (e.g., `project.published`, `payment.completed`).

Key flows:
- `project.published` → Notification Service (notify matching professionals)
- `candidature.accepted` → Contract Service (create micro-contract) + Notification Service
- `project.completed` → Payment Service (enable payment) + Contract Service
- `payment.completed` → Feedback Service (enable reviews) + Notification Service
- `feedback.submitted` → User Service (recalculate reputation)

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
- Logging: SLF4J, structured JSON format. No log files — stdout only.
- JVM tuning for containers: `-Xmx256m -Xms128m` per service.

## Docker & Kubernetes

- Each service has its own `Dockerfile` inside `services/<name>/`. Multi-stage builds targeting `linux/arm64`.
- Local development: `cd infra && docker compose up` starts everything.
- Production: K3s on Oracle Cloud VM with manifests from `infra/k8s/`.
- K8s resources per service: Deployment, Service (ClusterIP), ConfigMap, Secret.
- API Gateway + frontend exposed via K3s Ingress (Traefik or Nginx).
- TLS via Let's Encrypt + cert-manager or Certbot on the VM.
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
│   ├── adr/                      # Architecture Decision Records
│   ├── use-cases/
│   └── er-diagrams/
├── .github/
│   └── workflows/                # One workflow per service, path-filtered
├── CLAUDE.md
├── CLAUDE-SERVICES.md
└── README.md
```

Each service is a standalone Spring Boot project with its own `pom.xml`, `Dockerfile`, `application.yml`. The monorepo simplifies management for a 1-2 person team without sacrificing microservice independence. GitHub Actions uses **path filters** to build/deploy only the changed service.

## CI/CD Pipeline (GitHub Actions — Monorepo)

Each service has its own workflow file (e.g., `.github/workflows/user-service.yml`) triggered only when files in `services/user-service/**` change:

1. On push/PR to `main` touching `services/<name>/**`: build with Maven, run unit tests.
2. On merge to `main`: Docker buildx (linux/arm64), push to `ghcr.io/<user>/skillmatch/<service>:<sha>`.
3. SSH into Oracle Cloud VM, `kubectl set image` for that specific service.

GitHub Actions is free and unlimited on public repos. With GitHub Student Pack: 3,000 min/month on private repos.

## Design Patterns Reference (from course theory)

Explicitly used and to be documented:
- **Microservice Architecture** — overall system decomposition
- **Client-Server** — React ↔ API Gateway
- **REST** — all inter-service communication via HTTP, stateless
- **API Gateway** — single entry point (Spring Cloud Gateway)
- **Pub/Sub** — event-driven async communication (RabbitMQ topic exchange)
- **Database per Service** — each service owns its data store
- **Circuit Breaker** — resilience via Resilience4j
- **3-Tier Architecture** — internal structure of each microservice (Presentation/Business/Data)
- **Externalized Configuration** — config via env vars, ConfigMap, Secrets (12-Factor III)

## Deliverables Checklist

- [ ] Code repository with CI/CD (GitHub + GitHub Actions)
- [ ] Technical documentation (ADR archive)
- [ ] High-Level Architecture overview (diagram + description)
- [ ] Use cases — at least 3 per stakeholder (Professional, Company, Admin)
- [ ] Service Architecture for at least 2 core services (3-layer model with UML/ER diagrams, design patterns, metrics like complexity and test coverage)
- [ ] Interactive demo
- [ ] Development process considerations (sprint backlog, burndown chart — optional)

## When Generating Code

- All service code lives under `services/<service-name>/`. Never place service code at repo root.
- Always include proper Spring Security configuration (Resource Server with JWT).
- Always add RabbitMQ listener/publisher configuration where needed.
- Always create DTOs — never return entities from controllers.
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
