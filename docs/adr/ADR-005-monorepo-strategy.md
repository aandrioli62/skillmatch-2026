# ADR-005 - Monorepo Strategy

| Campo        | Valore                                    |
|--------------|---------------------------------------------|
| **Status**   | Accepted                                    |
| **Data**     | 2026-09-04                                  |
| **Autore**   | Team SkillMatch                             |
| **Contesto** | Organizzazione del codice sorgente          |

---

## Contesto

SkillMatch è composto da 7 microservizi backend, un frontend React e uno strato di infrastruttura (Docker Compose, manifesti Kubernetes, configurazione Keycloak). L'architettura a microservizi tipicamente convive con una strategia multi-repo, dove ogni servizio vive nel proprio repository Git indipendente, con permessi, versioning e pipeline CI/CD separati.

Il vincolo determinante in questo progetto è la dimensione del team: la traccia d'esame impone esplicitamente un massimo di due studenti per gruppo. Gestire 7 repository per i microservizi, più uno per il frontend e uno per l'infrastruttura, significherebbe per 1-2 persone dover mantenere fino a 9 repository separati, con conseguente duplicazione di README, configurazione CI, issue tracker e cronologia Git frammentata su più progetti.

---

## Decisione

Si adotta un **repository unico** (`skillmatch`), organizzato per cartelle:

```
skillmatch/
├── services/<nome-servizio>/   # 7 microservizi, ciascuno standalone
├── frontend/                   # React SPA
├── infra/                      # Docker Compose, K8s, Keycloak, script
├── docs/                       # ADR, use case, diagrammi
└── .github/workflows/          # Una pipeline per servizio, path-filtrata
```

Il monorepo è un **contenitore organizzativo**, non un cambio di architettura: ogni servizio in `services/<nome>/` resta un progetto Maven standalone con il proprio `pom.xml`, `Dockerfile` e `application.yml`, senza dipendenze di build dirette verso altri servizi o verso un modulo Maven padre condiviso. Nessun servizio importa codice Java di un altro servizio: l'unico accoppiamento tra servizi avviene tramite contratti REST versionati o eventi RabbitMQ (vedi ADR-006), esattamente come avverrebbe in una strategia multi-repo.

L'indipendenza di deploy tipica dei microservizi è preservata tramite **path filter** nelle pipeline GitHub Actions: ogni servizio ha il proprio file di workflow (es. `.github/workflows/user-service.yml`) che si attiva solo quando cambiano file dentro `services/user-service/**`. Un push che modifica solo il Payment Service non ricostruisce né ridistribuisce gli altri 6 servizi.

```yaml
# .github/workflows/user-service.yml (estratto)
on:
  push:
    branches: [main]
    paths:
      - "services/user-service/**"
      - ".github/workflows/user-service.yml"
```

---

## Vantaggi concreti per un team di 1-2 persone

- **Un solo posto per issue, pull request e documentazione**: nessuna necessità di decidere in quale dei 9 repository aprire un'issue che riguarda un flusso cross-service.
- **Refactoring cross-service atomico**: cambiare il formato di un evento RabbitMQ (es. aggiungere un campo a `payment.completed`) tocca sia il Payment Service (publisher) sia il Feedback Service (consumer). In un monorepo questo è un'unica pull request con un unico commit atomico, revisionabile in un solo colpo d'occhio; in un multi-repo servirebbero due PR coordinate manualmente su due repository diversi, con il rischio di deployare un lato senza l'altro.
- **Nessun overhead di gestione di N repository**: niente permessi da configurare N volte, niente README duplicati da tenere sincronizzati, niente clonazione di 9 repository separati per avere l'intero sistema in locale.
- **Changelog e history unificati**: per la presentazione d'esame è utile poter mostrare un'unica cronologia Git che racconta l'evoluzione dell'intero sistema, invece di dover ricostruire la timeline aggregando 9 cronologie separate.

---

## Svantaggi del multi-repo che qui non si applicano

Le ragioni che normalmente motivano una strategia multi-repo in un'architettura a microservizi sono:

- **Permessi granulari per team diversi**: utile quando team distinti sono responsabili di servizi distinti e non devono avere accesso in scrittura al codice degli altri team. Con un team di 1-2 persone che lavora su tutti i servizi, non esiste alcuna necessità di permessi differenziati.
- **Dimensione del repository**: un monorepo con decine di microservizi e anni di storia può diventare pesante da clonare. Con 7 servizi Spring Boot di dimensioni contenute e una storia di poche settimane/mesi, questo problema non si presenta nella pratica.
- **CI che gira su tutto il repository ad ogni push**: è il rischio concreto più rilevante di un monorepo, ma è già mitigato dai path filter per-servizio: ogni pipeline si attiva solo per il proprio sottoalbero di file, replicando l'isolamento di build che si avrebbe con repository separati.

---

## Alternative Considerate

| Alternativa | Motivo del rifiuto |
|-------------|---------------------|
| Un repository per microservizio (9 repository: 7 servizi + frontend + infra) | Overhead di coordinamento sproporzionato per un team di 1-2 persone entro una deadline fissa (agosto 2026); nessun beneficio di permessi granulari dato che non esistono team distinti da isolare |
| Monorepo con un'unica pipeline CI che builda e testa tutto ad ogni push | Tempi di build inutilmente lunghi: un cambio a un singolo servizio innescherebbe la compilazione e il test di tutti gli altri 6, anche se non modificati; i path filter per-servizio già adottati ottengono lo stesso isolamento di build del multi-repo senza pagarne l'overhead organizzativo |

---

## Conseguenze

**Positive:**
- Coordinamento semplificato per un team di 1-2 persone: un solo repository da clonare, un solo posto per issue/PR/documentazione.
- Refactoring cross-service (es. modifiche al contratto di un evento RabbitMQ) eseguibile in un'unica pull request atomica.
- Pipeline CI/CD isolate per servizio grazie ai path filter, preservando l'indipendenza di build e deploy tipica dei microservizi.
- Cronologia Git unificata, utile per la documentazione e la presentazione d'esame.

**Negative / Rischi:**
- Richiede disciplina nel mantenere i servizi effettivamente indipendenti a livello di codice (nessun import diretto tra moduli `services/*`), dato che il monorepo non impone questo isolamento a livello di build come farebbero repository fisicamente separati.
- Una configurazione errata dei path filter potrebbe far scattare pipeline non necessarie, o peggio, non far scattare una pipeline necessaria dopo una modifica.
- Se il team dovesse crescere in futuro oltre 1-2 persone con esigenze di permessi differenziati per servizio, la strategia andrebbe rivista verso un multi-repo.

---

## File di Riferimento

| File | Scopo |
|------|-------|
| [.github/workflows/](../../.github/workflows/) | Una pipeline per servizio, ciascuna con path filter dedicato |
| [.github/workflows/ci-base.yml](../../.github/workflows/ci-base.yml) | Configurazione CI condivisa/base riutilizzata dalle pipeline dei singoli servizi |
| [services/](../../services/) | Cartella con i 7 microservizi, ciascuno standalone |
| [ADR-001-microservice-architecture.md](ADR-001-microservice-architecture.md) | Contesto della decomposizione in 7 microservizi |
