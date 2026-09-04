# Flusso di pubblicazione progetto, candidatura e contratto

Questo diagramma copre la pubblicazione di un progetto da parte di un'azienda, la candidatura di un professionista validato, l'accettazione della candidatura e la creazione automatica del micro-contratto. Include la chiamata REST sincrona protetta da Circuit Breaker verso lo User Service e gli eventi asincroni pubblicati sull'exchange `skillmatch.events`.

```mermaid
sequenceDiagram
    actor Company as Company (Browser)
    actor Professional as Professional (Browser)
    participant GW as API Gateway
    participant PS as Project Service
    participant US as User Service
    participant MQ as RabbitMQ (exchange skillmatch.events)
    participant CS as Contract Service
    participant NS as Notification Service

    Company->>GW: POST /api/v1/projects (ruolo COMPANY)
    GW->>PS: POST /api/v1/projects
    PS->>PS: Crea progetto, status = DRAFT (projectdb)

    Company->>GW: PUT /api/v1/projects/{projectId}/publish
    GW->>PS: PUT /api/v1/projects/{projectId}/publish
    PS->>PS: status DRAFT -> OPEN

    PS-->>MQ: project.published
    Note over PS,NS: asincrono via RabbitMQ (skillmatch.events), binding #
    MQ-->>NS: project.published
    NS->>NS: Registra la conferma di pubblicazione per l'azienda

    Professional->>GW: GET /api/v1/projects/open
    GW->>PS: GET /api/v1/projects/open
    PS->>Professional: Elenco progetti aperti

    Professional->>GW: POST /api/v1/projects/{projectId}/candidatures (ruolo PROFESSIONAL)
    GW->>PS: POST /api/v1/projects/{projectId}/candidatures

    PS->>US: GET /api/v1/users/{professionalId}
    Note over PS,US: chiamata REST sincrona protetta da Circuit Breaker Resilience4j, JWT del chiamante inoltrato
    US->>PS: Ruolo e stato del professionista

    PS->>PS: Verifica ruolo PROFESSIONAL e stato VALIDATED
    Note over PS: Regola di business: solo chi supera la validazione puo' candidarsi. In caso contrario la richiesta viene rifiutata.
    PS->>PS: Salva candidatura, status = PENDING

    Company->>GW: GET /api/v1/projects/{projectId}/candidatures
    GW->>PS: GET /api/v1/projects/{projectId}/candidatures
    PS->>Company: Elenco candidati

    Company->>GW: PUT /api/v1/projects/{projectId}/candidatures/{candidatureId}/accept (ruolo COMPANY)
    GW->>PS: PUT .../candidatures/{candidatureId}/accept
    PS->>PS: candidatura status -> ACCEPTED

    PS-->>MQ: candidature.accepted (projectId, professionalId, companyId, amount = budget)
    Note over PS,NS: asincrono via RabbitMQ (skillmatch.events)

    MQ-->>CS: candidature.accepted
    Note over MQ,CS: coda contract.candidature.accepted
    CS->>CS: Crea Contract, status = DRAFT, amount = budget, commission_rate = tasso corrente (contractdb)

    MQ-->>NS: candidature.accepted
    Note over MQ,NS: binding #
    NS->>NS: Notifica il professionista selezionato e l'azienda

    Note over Company,Professional: Passo finale (fuori sequenza principale): firma del contratto in due passi separati con PUT /api/v1/contracts/{contractId}/sign. Prima l'azienda (DRAFT -> PENDING_SIGNATURES), poi il professionista (PENDING_SIGNATURES -> ACTIVE).
```
