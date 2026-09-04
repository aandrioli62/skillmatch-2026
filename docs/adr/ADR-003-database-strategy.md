# ADR-003 - Database Strategy: Polyglot Persistence

| Campo        | Valore                                    |
|--------------|--------------------------------------------|
| **Status**   | Accepted                                   |
| **Data**     | 2026-09-04                                 |
| **Autore**   | Team SkillMatch                            |
| **Contesto** | Persistenza dei microservizi               |

---

## Contesto

Il pattern **Database per Service** (adottato in ADR-001) richiede che ogni microservizio sia proprietario esclusivo del proprio storage, senza che altri servizi possano accedere direttamente al suo schema. Applicato in modo letterale, questo richiederebbe un'istanza di database dedicata per ciascuno dei 6 microservizi relazionali del sistema (User, Project, Contract, Payment, Feedback, più `identitydb` per Keycloak), oltre a un settimo storage per il Notification Service.

Il vincolo reale del progetto è però la VM Oracle Cloud Always Free su cui gira tutto (4 OCPU, 24 GB RAM, vedi ADR-004): 6 istanze PostgreSQL separate, ciascuna con il proprio overhead di processo, shared buffer e connection pool, sarebbero troppo pesanti in RAM da tenere in esecuzione contemporaneamente insieme a 7 JVM Spring Boot, Keycloak, RabbitMQ e MongoDB.

Serve quindi una strategia che rispetti lo spirito del pattern (isolamento dello schema, nessun accoppiamento tra servizi a livello di dati) senza pagarne il costo hardware pieno.

---

## Decisione

Si adotta una strategia di **polyglot persistence** con due componenti:

### 1. Una singola istanza fisica PostgreSQL 16, con database logici separati

Un solo processo PostgreSQL 16 (container `postgres`) ospita **6 database logici distinti**, creati automaticamente all'avvio da `infra/init-databases.sql`:

| Database logico | Servizio proprietario |
|------------------|------------------------|
| `identitydb` | Keycloak (creato automaticamente dalla variabile `POSTGRES_DB`) |
| `userdb` | User Service |
| `projectdb` | Project Service |
| `contractdb` | Contract Service |
| `paymentdb` | Payment Service |
| `feedbackdb` | Feedback Service |

Ogni servizio si connette con lo stesso utente applicativo (`skillmatch`), ma indirizzato al proprio database logico tramite la stringa di connessione JDBC. L'isolamento è quindi garantito a livello di **database logico e di privilegi**, non di processo fisico separato: uno schema non può leggere le tabelle di un altro database logico nella stessa istanza PostgreSQL, replicando l'effetto di isolamento del pattern Database per Service senza il costo di 6 processi PostgreSQL indipendenti.

### 2. MongoDB separato per il Notification Service

Il Notification Service usa un'istanza MongoDB dedicata (`notificationdb`) invece di un settimo database logico su PostgreSQL. La scelta è motivata dalla natura dei dati: una notifica è un documento con schema semi-strutturato, che incapsula il payload dell'evento RabbitMQ originale in un campo di tipo mappa libera (`data: Map<String, Object>` lato Java). Eventi diversi (`project.published`, `payment.completed`, `feedback.aggregated`, ...) hanno payload con struttura diversa, e forzarli in colonne relazionali fisse avrebbe richiesto o una tabella con molte colonne opzionali, o una colonna JSON su PostgreSQL che avrebbe comunque perso i vantaggi di indicizzazione e query nativa che un document store offre per questo caso d'uso.

### Migrazioni e schema

Ogni servizio relazionale gestisce il proprio schema con **Flyway** (script SQL versionati in `db/migration/`), e configura Hibernate con `ddl-auto: validate`: lo schema effettivo del database è sempre generato e governato da Flyway, mai da Hibernate in modalità `update` o `create`. Questo evita derive silenziose tra schema atteso e schema reale, specialmente importante quando più sviluppatori (o più deploy in ambienti diversi) toccano lo stesso database logico nel tempo.

---

## Il trade-off: istanza fisica condivisa

Condividere un'unica istanza PostgreSQL fisica tra 6 database logici indebolisce l'isolamento rispetto alla situazione ideale di 6 istanze separate:

- Un carico anomalo su un database logico (es. una query lenta o un lock prolungato su `paymentdb`) compete per IO disco, CPU e connection pool globale con gli altri database logici (`userdb`, `projectdb`, ecc.), cosa che non accadrebbe con processi fisicamente separati.
- Un fault dell'unico processo PostgreSQL è un single point of failure per 6 servizi su 7 (solo il Notification Service, su MongoDB, ne sarebbe indipendente).

Questo è accettato come scelta pragmatica: è l'unico modo per stare nei limiti hardware gratuiti della VM Oracle Cloud, mantenendo comunque la separazione logica e di privilegi che il pattern Database per Service richiede concettualmente (nessuno schema condiviso, nessuna foreign key cross-servizio, nessuna query diretta di un servizio sul database di un altro). In un contesto di produzione reale con budget disponibile, il passo naturale sarebbe promuovere ciascun database logico a istanza fisica separata, senza alcuna modifica al codice applicativo.

---

## Alternative Considerate

| Alternativa | Motivo del rifiuto |
|-------------|---------------------|
| Un'istanza PostgreSQL dedicata per ciascun servizio (6 processi separati) | Costo RAM proibitivo sulla VM Oracle Free Tier condivisa con 7 JVM, Keycloak, RabbitMQ e MongoDB; ogni istanza PostgreSQL aggiuntiva sottrae memoria che serve alle applicazioni |
| Un unico database condiviso senza separazione logica, con tabelle di tutti i servizi nello stesso schema | Violerebbe direttamente il pattern Database per Service, creando accoppiamento implicito tra schemi (un servizio potrebbe leggere/scrivere tabelle di un altro) e rendendo impossibile evolvere uno schema senza rischio di rompere un altro servizio |
| PostgreSQL managed su cloud provider (es. RDS, Cloud SQL, Azure Database) | Introdurrebbe un costo ricorrente non sostenibile per un progetto studentesco, e legherebbe l'infrastruttura a un provider specifico, contraddicendo il requisito di design cloud-agnostic (vedi ADR-004) |

---

## Conseguenze

**Positive:**
- Rispetta lo spirito del pattern Database per Service (isolamento logico, nessun accoppiamento di schema) con un costo hardware sostenibile su hardware gratuito.
- MongoDB per le notifiche evita lo schema rigido non necessario per dati naturalmente semi-strutturati.
- Flyway + `ddl-auto: validate` garantiscono che lo schema di ogni database logico sia sempre riproducibile e versionato in Git.
- La migrazione futura verso istanze fisiche separate per servizio richiederebbe solo un cambio di configurazione infrastrutturale (stringa di connessione), zero modifiche al codice applicativo.

**Negative / Rischi:**
- L'istanza PostgreSQL condivisa è un single point of failure per 6 microservizi su 7.
- Contesa di risorse (IO, CPU, connection pool) tra database logici in caso di carico anomalo su uno di essi.
- L'isolamento è garantito solo a livello di privilegi applicativi, non di processo: un errore di configurazione delle credenziali potrebbe in teoria esporre un database logico a un servizio non proprietario.

---

## File di Riferimento

| File | Scopo |
|------|-------|
| [infra/init-databases.sql](../../infra/init-databases.sql) | Script di creazione dei database logici PostgreSQL e grant dei privilegi |
| [infra/docker-compose.yml](../../infra/docker-compose.yml) | Configurazione dei container `postgres` e `mongo` |
| [services/user-service/src/main/resources/db/migration/](../../services/user-service/src/main/resources/db/migration/) | Esempio di migrazioni Flyway versionate per un servizio |
| [infra/k8s/postgres/](../../infra/k8s/postgres/) | Manifesti Kubernetes per il deploy dell'istanza PostgreSQL su K3s |
| [infra/k8s/mongodb/](../../infra/k8s/mongodb/) | Manifesti Kubernetes per il deploy di MongoDB su K3s |
| [ADR-001-microservice-architecture.md](ADR-001-microservice-architecture.md) | Contesto generale del pattern Database per Service |
