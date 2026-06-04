# SkillMatch 2026

> **Progettazione di Architetture di Servizi** — Università del Salento  
> Prof. Luca Mainetti — A.Y. 2025/26 — Deadline: August 2026

Piattaforma microservizi che mette in contatto **professionisti** e **aziende** per micro-progetti a breve termine (formazione, consulenza, prototipazione).

---

## Architettura

| Layer | Tecnologia |
|---|---|
| Frontend | React SPA (responsive) |
| API Gateway | Spring Cloud Gateway — JWT, routing, circuit breaker |
| Microservizi | Java 21 + Spring Boot 3.x (7 servizi) |
| Auth | Keycloak (OAuth 2.0 / OIDC) — Realm `skillmatch` |
| Messaggistica | RabbitMQ — topic exchange `skillmatch.events` |
| Database relazionale | PostgreSQL (istanza singola, 6 DB logici) |
| Database documentale | MongoDB (Notification Service) |
| Containerizzazione | Docker multi-stage — `linux/arm64` |
| Orchestrazione | K3s (Kubernetes CNCF) su Oracle Cloud ARM VM |
| Container Registry | GitHub Container Registry (ghcr.io) |
| CI/CD | GitHub Actions — path-filtered per servizio |
| Observability | Spring Actuator + Prometheus + Grafana + Loki |

---

## Microservizi

| Servizio | Porta | Database | Descrizione |
|---|---|---|---|
| **api-gateway** | 8080 | — | Ingresso unico, JWT, circuit breaker |
| **user-service** | 8081 | user-db (PG) | Professionisti, aziende, skill, portfolio, reputazione |
| **project-service** | 8082 | project-db (PG) | Micro-progetti, candidature |
| **contract-service** | 8083 | contract-db (PG) | Micro-contratti |
| **payment-service** | 8084 | payment-db (PG) | Transazioni, commissioni, fatture |
| **feedback-service** | 8085 | feedback-db (PG) | Feedback reciproci, trigger reputazione |
| **notification-service** | 8086 | notification-db (Mongo) | Notifiche in-app event-driven |

---

## Struttura Repository

```
skillmatch-2026/
├── services/
│   ├── api-gateway/          # Spring Cloud Gateway (:8080)
│   ├── user-service/         # Spring Boot (:8081)
│   ├── project-service/      # Spring Boot (:8082)
│   ├── contract-service/     # Spring Boot (:8083)
│   ├── payment-service/      # Spring Boot (:8084)
│   ├── feedback-service/     # Spring Boot (:8085)
│   └── notification-service/ # Spring Boot (:8086)
├── frontend/                 # React SPA
├── infra/
│   ├── docker-compose.yml    # Ambiente locale completo
│   ├── init-databases.sql    # Inizializzazione DB logici
│   ├── keycloak/             # Realm export JSON
│   ├── k8s/                  # Manifest Kubernetes per K3s
│   └── scripts/              # Script VM setup / deploy
├── docs/
│   ├── architecture.md       # Diagrammi e pattern
│   ├── adr/                  # Architecture Decision Records
│   ├── use-cases/            # Use case diagrams
│   └── er-diagrams/          # ER diagrams per servizio
├── .github/
│   └── workflows/            # CI/CD — un workflow per servizio
├── pom.xml                   # Maven parent POM
├── .gitignore
├── claude-md-main.md         # Istruzioni AI — architettura
└── claude-md-services.md     # Istruzioni AI — implementazione servizi
```

---

## Sviluppo Locale

**Prerequisiti**: Docker Desktop, Java 21, Node.js 20+, Maven 3.9+

```bash
# Avvia tutta l'infrastruttura (DB, RabbitMQ, Keycloak)
cd infra
docker compose up -d

# Build e avvio di un singolo servizio
cd services/user-service
mvn spring-boot:run

# Frontend
cd frontend
npm install && npm run dev
```

L'API Gateway è disponibile su `http://localhost:8080`.  
Keycloak Admin Console: `http://localhost:8180` (admin / admin).  
RabbitMQ Management: `http://localhost:15672` (guest / guest).

---

## Pattern Architetturali

- **Microservice Architecture** — decomposizione per dominio
- **API Gateway** — singolo ingresso, validazione JWT, circuit breaker
- **Pub/Sub** — comunicazione asincrona via RabbitMQ
- **Database per Service** — ogni servizio possiede il proprio DB logico
- **Circuit Breaker** — Resilience4j per resilienza inter-servizio
- **3-Tier Architecture** — `controller / service / repository` per ogni microservizio
- **Externalized Configuration** — env vars, ConfigMap, Secrets (12-Factor)

---

## Ruoli

| Ruolo | Descrizione |
|---|---|
| `PROFESSIONAL` | Si registra, completa il profilo (skill, portfolio), si candida ai progetti, riceve pagamento e feedback |
| `COMPANY` | Si registra, pubblica progetti, seleziona candidati, firma micro-contratti, paga tramite piattaforma |
| `ADMIN` | Valida i professionisti, configura la commissione (default 8%), monitora le transazioni |

---

## CI/CD

Ogni servizio ha il proprio workflow GitHub Actions (`.github/workflows/<service>.yml`) attivato solo quando cambiano i file in `services/<service>/**`:

1. **Push / PR → `main`**: build Maven + unit test
2. **Merge → `main`**: Docker buildx `linux/arm64` → push `ghcr.io`
3. **Deploy**: SSH su Oracle Cloud VM → `kubectl set image`

---

## Cloud

Oracle Cloud Infrastructure Always Free — Ampere A1 VM (4 OCPU, 24 GB RAM, 200 GB storage).  
L'architettura è **cloud-agnostica**: i manifest Kubernetes funzionano identicamente su AKS, EKS, GKE o qualsiasi cluster CNCF.
