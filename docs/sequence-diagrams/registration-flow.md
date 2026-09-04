# Flusso di registrazione professionista e validazione

Questo diagramma descrive il percorso completo di un professionista dalla registrazione tramite Keycloak fino alla validazione da parte di un amministratore, passando per la creazione del record applicativo nello User Service e le notifiche asincrone generate sull'exchange RabbitMQ `skillmatch.events`.

```mermaid
sequenceDiagram
    actor Browser as Browser (React SPA)
    participant KC as Keycloak
    participant GW as API Gateway
    participant US as User Service
    participant MQ as RabbitMQ (exchange skillmatch.events)
    participant NS as Notification Service
    actor Admin

    Browser->>KC: Redirect Authorization Code + PKCE
    KC->>Browser: Form di login/registrazione
    Browser->>KC: Autenticazione dell'utente
    KC->>Browser: Redirect con authorization_code
    Browser->>KC: POST /token (code + code_verifier)
    KC->>Browser: access_token (JWT)

    Browser->>GW: POST /api/v1/users (JWT)
    GW->>US: POST /api/v1/users
    Note over GW,US: Il Gateway valida il JWT contro le JWKS di Keycloak prima di inoltrare la richiesta
    US->>US: Crea riga users, status = PENDING (userdb, PostgreSQL)

    US-->>MQ: user.registered
    Note over US,NS: asincrono via RabbitMQ (skillmatch.events), coda notification.all-events, binding #
    MQ-->>NS: user.registered
    NS->>NS: Salva notifica "Registrazione completata. Benvenuto su SkillMatch!" (notificationdb, MongoDB)

    Browser->>GW: PUT /api/v1/users/{userId}/professional-profile
    GW->>US: PUT /api/v1/users/{userId}/professional-profile (ruolo PROFESSIONAL)
    US->>US: Salva nome, cognome, bio, conto pagamento, skill, portfolio

    Admin->>GW: POST /api/v1/admin/users/{userId}/validate
    GW->>US: POST /api/v1/admin/users/{userId}/validate
    US->>US: status PENDING -> VALIDATED

    US-->>MQ: user.validated
    Note over US,NS: asincrono via RabbitMQ (skillmatch.events)
    MQ-->>NS: user.validated
    NS->>NS: Notifica "Il tuo profilo professionale e' stato validato da un amministratore."

    Note over Admin,NS: Da questo momento il professionista puo' candidarsi ai progetti
```
