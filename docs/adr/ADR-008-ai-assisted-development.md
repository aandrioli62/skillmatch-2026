# ADR-008: Sviluppo Assistito da Agenti AI

| Campo        | Valore                                          |
|--------------|--------------------------------------------------|
| **Status**   | Accepted                                         |
| **Data**     | 2026-09-06                                       |
| **Autore**   | Aura Andrioli                                    |
| **Contesto** | Metodologia di sviluppo                          |

---

## Contesto

SkillMatch è sviluppato da una sola persona, entro la deadline vincolata dalla sessione d'esame, con uno scope tecnico ampio: 7 microservizi Spring Boot (incluso l'API Gateway), un frontend React, due motori di database (PostgreSQL e MongoDB), un message broker (RabbitMQ), un identity provider (Keycloak), un orchestratore Kubernetes (K3s) e le relative pipeline CI/CD. Scrivere, collegare e distribuire tutto questo interamente a mano, da soli, non è compatibile con il tempo a disposizione. La traccia d'esame non impone né vieta l'uso di strumenti di assistenza allo sviluppo.

---

## Decisione

Si adotta uno sviluppo assistito da agenti AI (Claude, nelle varianti Sonnet e Opus a seconda della sessione), con una separazione esplicita dei ruoli:

- Le **decisioni architetturali** (decomposizione in servizi, scelta dei pattern, modello dati concettuale, scelte infrastrutturali) restano umane e vengono fissate in documenti versionati nel repository (`CLAUDE.md`, `CLAUDE-SERVICES.md`, l'archivio ADR), che ogni sessione dell'agente carica automaticamente come contesto.
- La **generazione del codice** (implementazione dei servizi, migrazioni SQL, manifesti Kubernetes, pipeline CI/CD, documentazione tecnica) è delegata all'agente, vincolata da quei documenti.

**Una precisazione necessaria per l'onestà di questo ADR**: lo sviluppo non è avvenuto come un singolo thread continuo con un solo agente che segue il progetto dall'inizio alla fine. Il lavoro è stato distribuito nel tempo su più sessioni Claude Code distinte, spesso aperte in parallelo, ciascuna responsabile di un'area (ad esempio una sessione dedicata alla documentazione, una al frontend e al pannello admin, una al deploy K3s e all'infrastruttura). Sessioni diverse non condividono automaticamente lo stesso contesto conversazionale, solo gli stessi documenti versionati nel repository: questo ha causato, verificabilmente, incoerenze temporanee tra parti del sistema sviluppate in parallelo, poi individuate e corrette (vedi `docs/development-process.md`, sezione sui limiti). Inoltre, alcuni artefatti presenti nel repository (la configurazione dei gate di qualità JaCoCo/PMD, parte della suite di test dello User Service, il file `.github/copilot-instructions.md`) non sono riconducibili con certezza a nessuna delle sessioni Claude Code identificabili: è plausibile un contributo aggiuntivo di un altro strumento di assistenza (es. GitHub Copilot, il cui file di istruzioni è presente nel repository) o lavoro manuale non ricostruibile a posteriori con precisione.

---

## Alternative Considerate

| Alternativa | Motivo del rifiuto |
|---|---|
| Sviluppo interamente manuale | Tempi non sostenibili per una singola persona entro la deadline, dato lo scope richiesto dalla traccia (microservizi, orchestrazione, identity provider, message broker) |
| Un solo agente/sessione per l'intero progetto, in sequenza, dall'inizio alla fine | Sarebbe stato preferibile per la tracciabilità e la coerenza, ma la pratica reale (sessioni aperte in momenti diversi, a volte in parallelo, su porzioni diverse del sistema) non ha seguito questo schema ideale |
| Pair programming con un secondo sviluppatore umano | La traccia consente esplicitamente il lavoro individuale; non era necessario un secondo sviluppatore per rispettare il vincolo del corso |

---

## Conseguenze

**Positive:**
- Ha reso concretamente sostenibile, per una singola persona, uno scope tecnico che tipicamente richiederebbe un team.
- Le decisioni architetturali restano tracciabili e verificabili nei documenti versionati, indipendentemente da quale sessione o agente abbia poi scritto il codice conforme ad esse.
- Iterazione rapida su uno stack eterogeneo (Java/Spring Boot, React, Kubernetes, RabbitMQ, Keycloak) che richiederebbe altrimenti competenze specialistiche multiple.

**Negative / Rischi:**
- Dipendenza dalla qualità del contesto fornito: una sessione senza il documento giusto caricato, o con istruzioni ambigue, produce codice incoerente con il resto del sistema.
- Rischio di deriva architetturale su sessioni lunghe, aggravato in questo progetto dal fatto che sessioni diverse e parallele possono divergere silenziosamente se non condividono lo stesso contesto aggiornato: è successo concretamente (vedi sopra), non è un rischio solo teorico.
- Necessità di verifica umana sistematica: il codice generato da un agente è plausibile anche quando è sbagliato, cioè compila e sembra corretto a uno sguardo superficiale anche quando la logica di business contiene un errore non ovvio.
- La comprensione del codice da parte dello sviluppatore richiede uno sforzo di lettura e verifica aggiuntivo, che non sarebbe necessario nella stessa misura scrivendolo a mano riga per riga.
- Tracciabilità parziale della generazione: dato il numero di sessioni coinvolte e la possibile presenza di un secondo strumento di assistenza, non è sempre ricostruibile con certezza quale porzione di codice sia stata generata da quale agente, corretta a mano, o scritta da uno strumento diverso. Questa incertezza non è stata risolta retroattivamente (es. riscrivendo la git history) per non produrre una documentazione non fedele ai fatti.

---

## File di Riferimento

| File | Scopo |
|---|---|
| [docs/development-process.md](../development-process.md) | Processo di sviluppo effettivo, divisione delle responsabilità, limiti della metodologia |
| [CLAUDE.md](../../CLAUDE.md) | Artefatto di contesto architetturale caricato da ogni sessione |
| [CLAUDE-SERVICES.md](../../CLAUDE-SERVICES.md) | Artefatto di contesto per l'implementazione dei servizi |
| [.github/copilot-instructions.md](../../.github/copilot-instructions.md) | Presente nel repository, origine non attribuibile con certezza a questo processo (vedi sopra) |
