# ADR-002 — Keycloak Configuration and Realm Management

| Campo        | Valore                                     |
|--------------|--------------------------------------------|
| **Status**   | Accepted                                   |
| **Data**     | 2026-06-19                                 |
| **Autore**   | Team SkillMatch                            |
| **Contesto** | Identity & Access Management del progetto  |

---

## Contesto

SkillMatch richiede un sistema di autenticazione e autorizzazione centralizzato che supporti:

- Registrazione e login utenti (SPA React) tramite **Authorization Code + PKCE**.
- Tre ruoli applicativi distinti: `PROFESSIONAL`, `COMPANY`, `ADMIN`.
- Comunicazione sicura tra microservizi (service-to-service) tramite **Client Credentials**.
- Validazione JWT distribuita — ogni microservizio verifica autonomamente il token senza chiamare Keycloak ad ogni request.

L'autenticazione è esternalizzata a Keycloak per seguire il principio **12-Factor III** (Externalized Configuration) e per non accoppiare la logica di identità ai singoli servizi.

---

## Decisione

Si usa **Keycloak 26.0** come Identity Provider. Il realm `skillmatch` è definito e versionato nel repository come file JSON (`infra/keycloak/skillmatch-realm.json`), importato automaticamente all'avvio del container tramite il flag `--import-realm`.

---

## Struttura del Realm

### Realm: `skillmatch`

| Parametro                     | Valore        | Motivazione                                        |
|-------------------------------|---------------|----------------------------------------------------|
| `sslRequired`                 | `external`    | TLS obbligatorio solo verso l'esterno; intra-cluster HTTP |
| `accessTokenLifespan`         | 300 s (5 min) | Token brevi riducono la finestra di esposizione    |
| `ssoSessionIdleTimeout`       | 1800 s        | Sessione scade dopo 30 min di inattività           |
| `ssoSessionMaxLifespan`       | 36000 s       | Sessione massima 10 ore (giornata lavorativa)      |
| `bruteForceProtected`         | `true`        | Blocco account dopo 5 tentativi falliti            |
| `passwordPolicy`              | lunghezza ≥ 8, almeno 1 maiuscola, 1 cifra, diverso da username | Sicurezza base OWASP |
| `registrationEmailAsUsername` | `true`        | Email come identificatore univoco                  |

### Client 1: `skillmatch-spa`

Usato dalla React SPA per l'autenticazione degli utenti umani.

| Parametro                  | Valore            |
|----------------------------|-------------------|
| `publicClient`             | `true`            |
| `standardFlowEnabled`      | `true`            |
| `implicitFlowEnabled`      | `false`           |
| `directAccessGrantsEnabled`| `false`           |
| `pkce.code.challenge.method` | `S256`          |
| `redirectUris`             | `http://localhost:3000/*`, `https://skillmatch.example.com/*` |
| `accessTokenLifespan`      | 300 s             |

**Perché Authorization Code + PKCE?**  
Le SPA non possono mantenere un client secret sicuro (il codice è nel browser). PKCE mitiga l'intercettazione del code. Implicit Flow è deprecato (OAuth 2.1 draft).

### Client 2: `skillmatch-m2m`

Usato per comunicazioni service-to-service (es. api-gateway → user-service tramite token con ruolo interno).

| Parametro                  | Valore    |
|----------------------------|-----------|
| `publicClient`             | `false`   |
| `serviceAccountsEnabled`   | `true`    |
| `standardFlowEnabled`      | `false`   |
| `accessTokenLifespan`      | 60 s      |

**Perché Client Credentials?**  
I servizi backend non hanno utente umano associato. Client Credentials è il flow OAuth 2.0 designato per la machine-to-machine authentication. Token di 60 secondi minimizza il rischio di compromissione.

### Ruoli Realm

| Ruolo          | Descrizione                                                   |
|----------------|---------------------------------------------------------------|
| `PROFESSIONAL` | Professionista — applica a progetti, riceve pagamenti         |
| `COMPANY`      | Azienda — pubblica progetti, seleziona candidati              |
| `ADMIN`        | Amministratore — valida professionisti, configura commissioni |

I ruoli sono **realm roles** (non client roles) per essere disponibili in tutti i client senza dover duplicare le configurazioni.

### Protocol Mappers

Due mapper personalizzati sul client `skillmatch-spa`:

1. **`realm-roles-mapper`** — inserisce i ruoli realm nel claim `roles` (array di stringhe) dell'access token e dell'ID token. Semplifica la lettura lato Spring Security (`hasAuthority('PROFESSIONAL')`).
2. **`audience-mapper`** — aggiunge `skillmatch-spa` nell'array `aud` del token. Necessario per la validazione dell'audience nei resource server.

Il claim `realm_access.roles` rimane disponibile tramite il `clientScope` standard `roles` (compatibilità con librerie che leggono il formato nativo Keycloak).

### Configurazione Spring Boot (Resource Server)

Ogni microservizio valida i JWT tramite:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/skillmatch}
          jwk-set-uri: ${KEYCLOAK_JWK_URI:http://localhost:8180/realms/skillmatch/protocol/openid-connect/certs}
```

Per leggere i ruoli dal claim `roles`, aggiungere in ogni servizio:

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
    converter.setAuthoritiesClaimName("roles");
    converter.setAuthorityPrefix("ROLE_");
    JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
    jwtConverter.setJwtGrantedAuthoritiesConverter(converter);
    return jwtConverter;
}
```

---

## Flusso di Autenticazione (Authorization Code + PKCE)

```
Browser (React SPA)
  │
  │  1. Genera code_verifier (random) e code_challenge = SHA-256(code_verifier)
  │  2. Redirect a Keycloak con response_type=code, code_challenge, code_challenge_method=S256
  ▼
Keycloak :8180
  │
  │  3. Presenta login form; utente si autentica
  │  4. Emette authorization_code; redirect a http://localhost:3000/callback?code=...
  ▼
Browser
  │
  │  5. POST /token con code + code_verifier (no client_secret!)
  ▼
Keycloak
  │
  │  6. Verifica SHA-256(code_verifier) == code_challenge
  │  7. Ritorna: { access_token, refresh_token, id_token }
  ▼
Browser
  │
  │  8. Chiama API Gateway con Authorization: Bearer <access_token>
  ▼
API Gateway :8080
  │
  │  9. Verifica JWT firma con JWKS pubblico di Keycloak (nessuna chiamata remota)
  │  10. Routing verso il microservizio appropriato
  ▼
Microservizio
  │
  │  11. Verifica JWT (stessa logica) + controlla ruolo richiesto
  │  12. Risposta
  ▼
Browser
```

---

## Gestione del Realm come Configuration-as-Code

### Strategia di Import Automatico

Il `docker-compose.yml` monta `./keycloak` in `/opt/keycloak/data/import` e avvia Keycloak con `--import-realm`. All'avvio, Keycloak importa `skillmatch-realm.json` **se il realm non esiste già**.

```yaml
# infra/docker-compose.yml (estratto)
keycloak:
  image: quay.io/keycloak/keycloak:26.0
  command: start-dev --http-port=8180 --import-realm
  volumes:
    - ./keycloak:/opt/keycloak/data/import:ro
```

> **Nota**: `--import-realm` NON sovrascrive un realm esistente. Per forzare la re-importazione in sviluppo, usare `docker compose down -v keycloak` e riavviare.

### Export del Realm

Dopo modifiche alla configurazione Keycloak tramite Admin Console, esportare il realm con:

```bash
# Metodo 1 — via kc.sh dentro il container (raccomandato, include tutti gli utenti)
./infra/scripts/export-realm.sh --method docker

# Metodo 2 — via Admin REST API (richiede curl + jq)
./infra/scripts/export-realm.sh --method api

# Override variabili
KEYCLOAK_ADMIN=admin KEYCLOAK_PASSWORD=secret \
  ./infra/scripts/export-realm.sh --method api --output /tmp/custom-export.json
```

Lo script scrive il JSON in `infra/keycloak/skillmatch-realm.json`.

### Workflow Git

```
1. Modificare la configurazione in Keycloak Admin Console
2. Eseguire ./infra/scripts/export-realm.sh
3. Rimuovere dal JSON eventuali secret prima del commit:
   - skillmatch-m2m.secret → sostituire con "change-me-in-production"
   - password utenti test → mantenere solo in locale / .gitignore se necessario
4. git add infra/keycloak/skillmatch-realm.json
5. git commit -m "chore(keycloak): update realm configuration — <descrizione>"
6. Il CI/CD (GitHub Actions) non ri-deploya Keycloak automaticamente;
   le modifiche al realm richiedono un rollout manuale in produzione.
```

### Sicurezza in Produzione

| Elemento               | Sviluppo                  | Produzione                                   |
|------------------------|---------------------------|----------------------------------------------|
| `KEYCLOAK_ADMIN`       | `admin`                   | Secret K8s (`keycloak-admin-secret`)         |
| `KC_DB_PASSWORD`       | `secret`                  | Secret K8s (`postgres-secret`)               |
| `skillmatch-m2m.secret`| `change-me-in-production` | Generato e iniettato via K8s Secret          |
| `sslRequired`          | `external`                | `all` (dietro Ingress con TLS)               |
| `KC_HOSTNAME`          | `localhost`               | FQDN reale (`keycloak.skillmatch.example.com`) |

> **Non committare mai** password reali o client secret nel JSON versionato.  
> Usare K8s Secrets e la funzionalità di override di Keycloak tramite env vars.

---

## Utenti di Test

Inclusi nel realm JSON **solo per sviluppo locale**. Devono essere rimossi o sostituiti con credenziali non committate in ambienti condivisi.

| Username                   | Password         | Ruolo          |
|----------------------------|------------------|----------------|
| `admin@skillmatch.dev`     | `Admin1234!`     | `ADMIN`        |
| `mario.rossi@example.com`  | `Professional1!` | `PROFESSIONAL` |
| `hr@techcorp.com`          | `Company1234!`   | `COMPANY`      |

---

## Alternative Considerate

| Alternativa            | Motivo del rifiuto                                                            |
|------------------------|-------------------------------------------------------------------------------|
| Spring Security + DB   | Richiederebbe implementare login, token management, PKCE da zero             |
| Auth0 / Okta           | Vendor lock-in; costo per uso non triviale; no self-hosted gratuito          |
| Cognito (AWS)          | Accoppia l'architettura ad AWS; contraddice il requisito cloud-agnostic       |
| Keycloak Operator (K8s)| Overhead eccessivo per un progetto accademico monocluster                    |

**Keycloak self-hosted** è la scelta che massimizza il controllo, rispetta il vincolo cloud-agnostic e permette di eseguire gratuitamente su Oracle Cloud Free Tier.

---

## Conseguenze

**Positive:**
- Nessuna logica di autenticazione nei microservizi — solo validazione JWT stateless.
- Configurazione realm versionata in Git → riproducibilità ambienti (dev/CI/prod).
- Standard OAuth 2.0 / OIDC → librerie mature in ogni linguaggio.
- `--import-realm` rende il `docker compose up` completamente self-contained.

**Negative / Rischi:**
- Aggiungere un singolo componente con memoria propria (JVM Keycloak ~512 MB).
- Le modifiche al realm in produzione richiedono un processo manuale (export → commit → deploy).
- Token short-lived (5 min) richiedono una gestione corretta del refresh nel frontend.

---

## File di Riferimento

| File                                     | Scopo                                           |
|------------------------------------------|-------------------------------------------------|
| [infra/keycloak/skillmatch-realm.json](../../infra/keycloak/skillmatch-realm.json) | Definizione completa del realm (Configuration-as-Code) |
| [infra/scripts/export-realm.sh](../../infra/scripts/export-realm.sh) | Script per esportare e aggiornare il realm JSON |
| [infra/docker-compose.yml](../../infra/docker-compose.yml) | Configurazione Docker locale con auto-import    |
| [infra/k8s/keycloak/](../../infra/k8s/keycloak/) | Manifesti Kubernetes per il deploy su K3s       |
