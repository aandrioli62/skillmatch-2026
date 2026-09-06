# Flusso di registrazione self-service e validazione

Questo diagramma descrive il percorso completo di registrazione di un nuovo utente (professionista o azienda) tramite l'endpoint applicativo self-service `POST /api/v1/auth/register`, fino alla validazione del professionista da parte di un amministratore.

Il vecchio flusso, in cui il Browser veniva reindirizzato alla UI di Keycloak per la registrazione e completava poi il profilo in una chiamata separata, non e' piu' quello primario: e' stato sostituito da questo endpoint applicativo, che orchestra in un'unica chiamata la creazione dell'utente su Keycloak, l'assegnazione del ruolo e la creazione del record applicativo nello User Service. Keycloak resta usato, invariato, SOLO per il login post-registrazione (Authorization Code + PKCE), non piu' per la registrazione stessa. Le motivazioni di questa scelta sono documentate in [ADR-007-self-service-registration](../adr/ADR-007-self-service-registration.md).

```mermaid
sequenceDiagram
    actor Browser as Browser React (pagina Register.jsx)
    participant GW as API Gateway
    participant AC as AuthController (User Service)
    participant KAC as KeycloakAdminClient (User Service)
    participant KC as Keycloak Admin REST API
    participant US as UserService (layer business, User Service)
    participant MQ as RabbitMQ (exchange skillmatch.events)
    participant NS as Notification Service
    actor Admin

    Browser->>GW: POST /api/v1/auth/register { email, password, role, firstName+lastName oppure companyName }
    GW->>AC: POST /api/v1/auth/register
    Note over GW,AC: Endpoint pubblico, nessun JWT richiesto

    AC->>AC: Valida ruolo (PROFESSIONAL o COMPANY, mai ADMIN), campi obbligatori per il ruolo, email non gia' registrata
    Note over AC: In caso di violazione: errore 409 (email duplicata) o 422 (dati non validi)

    AC->>KAC: createUserWithRole(email, password, ruolo, nome, cognome)

    KAC->>KC: POST /realms/{admin-realm}/protocol/openid-connect/token (grant_type=password, client_id=admin-cli, credenziali admin bootstrap)
    KC->>KAC: token amministrativo

    KAC->>KC: POST /admin/realms/{realm}/users (password permanente, emailVerified=true)
    Note over KAC,KC: emailVerified=true per non bloccare il primo login
    KC->>KAC: id utente Keycloak (header Location)

    KAC->>KC: GET realm role + POST /admin/realms/{realm}/users/{id}/role-mappings/realm
    KC->>KAC: ruolo PROFESSIONAL o COMPANY assegnato

    AC->>US: registerUser(...)
    US->>US: Crea riga users, status = PENDING (userdb, PostgreSQL)

    US-->>MQ: user.registered
    Note over US,NS: asincrono via RabbitMQ (skillmatch.events), coda notification.all-events, binding #
    MQ-->>NS: user.registered
    NS->>NS: Salva notifica "Registrazione completata. Benvenuto su SkillMatch!" (notificationdb, MongoDB)

    AC->>US: completa profilo minimo nella stessa chiamata
    Note over AC,US: updateProfessionalProfile (nome, cognome) oppure updateCompanyProfile (companyName), a seconda del ruolo
    US->>AC: profilo salvato

    AC->>GW: 201 Created (utente creato)
    GW->>Browser: 201 Created

    Note over Browser,US: Passo separato, successivo: il professionista arricchisce il profilo con skill (PUT /api/v1/users/{userId}/skills) e portfolio (PUT /api/v1/users/{userId}/portfolio-items)

    Admin->>GW: POST /api/v1/admin/users/{userId}/validate
    GW->>US: POST /api/v1/admin/users/{userId}/validate
    US->>US: status PENDING -> VALIDATED

    US-->>MQ: user.validated
    Note over US,NS: asincrono via RabbitMQ (skillmatch.events)
    MQ-->>NS: user.validated
    NS->>NS: Notifica "Il tuo profilo professionale e' stato validato da un amministratore."

    Note over Admin,NS: Da questo momento il professionista puo' candidarsi ai progetti. La stessa validazione si applica anche alle aziende: un'azienda con status PENDING non puo' creare un progetto (vedi docs/sequence-diagrams/project-candidature-flow.md).
```
