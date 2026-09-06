# SkillMatch 2026

> **Progettazione di Architetture di Servizi**, Università del Salento  
> Prof. Luca Mainetti, A.Y. 2025/26, Deadline: August 2026

Piattaforma microservizi che mette in contatto **professionisti** e **aziende** per micro-progetti a breve termine (formazione, consulenza, prototipazione).

---

## Metodologia di Sviluppo

Il progetto è sviluppato da una sola persona (Aura Andrioli), con sviluppo assistito da agenti AI (Claude). Le decisioni architetturali restano umane e sono fissate nei documenti versionati in questo repository (`CLAUDE.md`, `CLAUDE-SERVICES.md`, l'archivio ADR); la generazione del codice è delegata all'agente, vincolata da quei documenti. Per la motivazione di questa scelta, i suoi limiti onestamente riconosciuti e il processo di sviluppo effettivo, vedi [ADR-008](docs/adr/ADR-008-ai-assisted-development.md) e [docs/development-process.md](docs/development-process.md).

---

## Architettura

| Layer | Tecnologia |
|---|---|
| Frontend | React SPA (responsive), containerizzato e deployato come pod a sé |
| API Gateway | Spring Cloud Gateway: JWT, routing, circuit breaker |
| Microservizi | Java 21 + Spring Boot 3.x (7 servizi) |
| Auth | Keycloak (OAuth 2.0 / OIDC), Realm `skillmatch`, registrazione self-service via Admin REST API |
| Messaggistica | RabbitMQ, topic exchange `skillmatch.events` |
| Database relazionale | PostgreSQL (istanza singola, 6 DB logici) |
| Database documentale | MongoDB (Notification Service) |
| Containerizzazione | Docker multi-stage, `linux/arm64` |
| Orchestrazione | K3s (Kubernetes CNCF) su Oracle Cloud ARM VM |
| Container Registry | GitHub Container Registry (ghcr.io) |
| CI/CD | GitHub Actions, path-filtered per servizio |
| Observability | Spring Actuator + Prometheus + Grafana + Loki |

---

## Microservizi

| Servizio | Porta | Database | Descrizione |
|---|---|---|---|
| **api-gateway** | 8080 | n/d | Ingresso unico, JWT, circuit breaker |
| **user-service** | 8081 | user-db (PG) | Professionisti, aziende, skill, portfolio, reputazione, segnalazioni, registrazione self-service |
| **project-service** | 8082 | project-db (PG) | Micro-progetti, candidature (con accettazione/rifiuto) |
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
│   ├── events.md             # Mappa eventi RabbitMQ
│   ├── deployment.md         # Diagramma di deployment K3s
│   ├── development-process.md # Processo di sviluppo, metodologia AI-assisted
│   ├── adr/                  # Architecture Decision Records
│   ├── use-cases/            # Use case diagrams
│   ├── er-diagrams/          # ER diagrams per servizio
│   ├── sequence-diagrams/    # Sequence diagrams dei flussi principali
│   └── three-layer/          # Diagrammi 3-layer + metriche qualità (servizi core)
├── .github/
│   └── workflows/            # CI/CD, un workflow per servizio
├── pom.xml                   # Maven parent POM
├── .gitignore
├── CLAUDE.md                 # Istruzioni AI, architettura
└── CLAUDE-SERVICES.md        # Istruzioni AI, implementazione servizi
```

---

## Sviluppo Locale

**Prerequisiti**: Docker + Docker Compose, Node.js 20+, Java 21 (o usa i wrapper `mvnw` già inclusi in ogni servizio, non serve Maven installato a parte).

**1. Clona il repository e avvia lo stack:**

```bash
git clone <repo-url> && cd skillmatch-2026/infra
docker compose up
```

Parte tutto: Postgres, MongoDB, RabbitMQ, Keycloak, i 7 microservizi. Le email (verifica/reset password di Keycloak, e le 3 notifiche business che ne mandano una reale) non funzionano finché non configuri l'SMTP, vedi punto 3. Tutto il resto funziona da subito.

**2. Avvia il frontend** (in un altro terminale):

```bash
cd frontend && npm install && npm run dev
```

App su `http://localhost:3000`. Utenti di test già seedati nel realm (validi sia in locale sia in produzione, sono nel realm export committato): `test-professional`, `test-company`, `test-admin`, password uguale allo username per tutti e tre. Puoi anche registrarne di nuovi via self-service, o creare un altro admin con `infra/scripts/create-admin.sh`.

**3. (Opzionale) Email reali via Brevo:**

Crea un account gratuito su Brevo, prendi le credenziali SMTP, poi:
- Crea `infra/.env` (gitignored) con `SMTP_USER`, `SMTP_PASSWORD`, `SMTP_FROM`, `NOTIFICATION_SERVICE_CLIENT_SECRET`.
- Configura Keycloak (client `notification-service` + SMTP del realm): vedi `docs/manuale-tecnico.md`, capitolo Processo di Sviluppo, sezione "Limiti della pipeline: cosa resta manuale", per i comandi esatti.

**4. Girare i test di un servizio:**

```bash
cd services/<nome-servizio> && ./mvnw verify
```

`user-service` e `project-service` hanno gate JaCoCo (90% coverage) e PMD (complessità ciclomatica max 10) in CI: se li tocchi, `./mvnw verify` deve passare pulito prima di aprire una PR.

**5. Deploy**: push su `main` con modifiche sotto `services/<nome>/**` o `frontend/**` attiva automaticamente build, test e deploy sulla VM per quel servizio. Per modifiche ai manifest Kubernetes o nuovi secret, vedi "Limiti della pipeline" citato sopra.

Console utili in locale: Keycloak Admin (`http://localhost:8180`, admin / admin), RabbitMQ Management (`http://localhost:15672`, guest / guest), API Gateway (`http://localhost:8080`).

---

## Ambiente di Produzione (demo)

| Cosa | Dove |
|---|---|
| Sito | `https://92.4.167.195.nip.io` |
| Keycloak (login / admin console) | `https://keycloak.92.4.167.195.nip.io` |
| Utenti di test | `test-professional`, `test-company`, `test-admin`, password uguale allo username (stessi utenti dell'ambiente locale, seedati dallo stesso realm già committato) |
| Admin Keycloak reale (bootstrap del realm) | **non riportato qui di proposito**: e una credenziale con privilegi completi sul realm, vive come K8s Secret sulla VM, non in un documento versionato nel repository |

Per creare un ulteriore account ADMIN in produzione (oltre a `test-admin`), vedi `infra/scripts/create-admin.sh` puntato contro `KEYCLOAK_URL=https://keycloak.92.4.167.195.nip.io`, descritto in `docs/adr/ADR-002-keycloak-configuration.md`.

---

## Pattern Architetturali

- **Microservice Architecture**: decomposizione per dominio
- **API Gateway**: singolo ingresso, validazione JWT, circuit breaker
- **Pub/Sub**: comunicazione asincrona via RabbitMQ
- **Database per Service**: ogni servizio possiede il proprio DB logico
- **Circuit Breaker**: Resilience4j per resilienza inter-servizio
- **3-Tier Architecture**: `controller / service / repository` per ogni microservizio
- **Externalized Configuration**: env vars, ConfigMap, Secrets (12-Factor)

---

## Ruoli

| Ruolo | Descrizione |
|---|---|
| `PROFESSIONAL` | Si registra (self-service, `POST /api/v1/auth/register`), completa il profilo (skill da catalogo condiviso, portfolio), si candida ai progetti, riceve pagamento e feedback, può segnalare una controparte |
| `COMPANY` | Si registra (self-service), pubblica progetti, seleziona o rifiuta candidati, firma micro-contratti, paga tramite piattaforma, può segnalare una controparte |
| `ADMIN` | Valida i professionisti, configura la commissione (default 8%), monitora le transazioni (con filtri e totali aggregati), gestisce segnalazioni e sospensioni |

---

## CI/CD

Ogni servizio ha il proprio workflow GitHub Actions (`.github/workflows/<service>.yml`) attivato solo quando cambiano i file in `services/<service>/**`:

1. **Push / PR → `main`**: build Maven + unit test
2. **Merge → `main`**: Docker buildx `linux/arm64` → push `ghcr.io`
3. **Deploy**: SSH su Oracle Cloud VM → `kubectl set image`

---

## Cloud

Oracle Cloud Infrastructure Always Free, Ampere A1 VM (4 OCPU, 24 GB RAM, 200 GB storage).  
L'architettura è **cloud-agnostica**: i manifest Kubernetes funzionano identicamente su AKS, EKS, GKE o qualsiasi cluster CNCF.
