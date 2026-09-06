# Processo di Sviluppo

Questo documento descrive come SkillMatch è stato effettivamente sviluppato: metodologia, ruoli, ciclo di lavoro, gestione Git, controlli di qualità e limiti onestamente riconosciuti. Copre il punto della traccia d'esame sulle "considerazioni sul processo di sviluppo adottato". Ogni affermazione qui riportata è stata verificata contro la cronologia Git reale (`git log`), i file effettivamente presenti nel repository e i report di build, non dedotta a posteriori per far apparire il processo più ordinato di quanto sia stato.

Il progetto è sviluppato da una sola persona (Aura Andrioli), con sviluppo assistito da agenti AI. Le motivazioni e i limiti di questa scelta sono discussi in dettaglio in [ADR-008](adr/ADR-008-ai-assisted-development.md); questo documento ne è il naturale proseguimento operativo.

---

## 1. Metodologia

L'architettura (decomposizione in servizi, pattern, modello dati concettuale, scelte infrastrutturali) è stata decisa a monte e fissata in documenti versionati nel repository, prima della generazione di codice. Da questi documenti è stata derivata una roadmap con step atomici (una feature o un endpoint alla volta, non un intero servizio in un colpo solo). Ogni step segue lo stesso schema: generazione assistita del codice per quello step specifico, verifica funzionale del risultato, commit, passaggio allo step successivo. Non si è proceduto a uno step successivo se il checkpoint di verifica del precedente non passava.

## 2. Artefatti di Contesto

`CLAUDE.md` e `CLAUDE-SERVICES.md` sono documenti versionati nel repository, non messaggi di una conversazione: un agente che apre una nuova sessione su questo progetto li carica automaticamente come contesto architetturale, indipendentemente dalla macchina o dalla sessione precedente. Questo risolve due problemi concreti dello sviluppo assistito da agenti:

- **Portabilità tra macchine**: aprire il progetto da un computer diverso non richiede di ricostruire a voce il contesto architetturale; è già nel repository.
- **Continuità tra sessioni**: una nuova sessione (necessaria quando una conversazione precedente si chiude o scade) riparte dallo stesso contesto invece che da zero. Il contesto vive nel repository, non nella conversazione.

Usando la terminologia corrente del *context engineering*, gli artefatti di un progetto assistito da agenti si dividono in due categorie:

- **Guide** (controlli feedforward): orientano l'agente *prima* dell'azione. In questo progetto: `CLAUDE.md` (architettura, convenzioni), `CLAUDE-SERVICES.md` (dettagli di implementazione dei servizi), l'archivio ADR (motivazioni delle decisioni), la roadmap a step.
- **Sensori** (controlli feedback): verificano il risultato *dopo* l'azione. In questo progetto: compilazione Maven, avvio effettivo del servizio, test automatici, CI su GitHub Actions, JaCoCo (copertura), PMD (complessità ciclomatica).

Una guida da sola non basta: un documento di contesto può essere ambiguo o non aggiornato, e l'agente può comunque produrre codice plausibile ma sbagliato. Un sensore da solo non basta: sapere che qualcosa non compila non dice all'agente *come* si sarebbe dovuto scomporre il sistema. Le due categorie insieme, guide che orientano e sensori che verificano, sono state il meccanismo di controllo effettivamente usato in questo progetto.

## 3. Divisione delle Responsabilità

| Attività | Umano | Agente |
|---|---|---|
| Decisioni architetturali (decomposizione in servizi, pattern) | ✓ | |
| Scelta dello stack tecnologico | ✓ | |
| Modello dati (schema concettuale) | ✓ | ✓ (migrazioni SQL concrete a partire dallo schema deciso) |
| Configurazione Keycloak (realm, client, ruoli) | | ✓ (sotto indicazione umana su flussi OAuth2 richiesti) |
| Provisioning e configurazione della VM Oracle Cloud | ✓ | |
| Gestione del backlog | ✓ (avviata con GitHub Issues, poi abbandonata, vedi sotto) | |
| Generazione del codice applicativo | | ✓ |
| Generazione dei manifesti Kubernetes e delle pipeline CI/CD | | ✓ |
| Generazione della documentazione tecnica | | ✓ (diretta e rivista dall'umano) |
| Verifica funzionale (compilazione, avvio, chiamata reale agli endpoint) | ✓ | ✓ |
| Debugging in produzione sulla VM | ✓ | ✓ |

La riga sul backlog merita una nota: è stato aperto un set di GitHub Issues a inizio progetto per tracciare il lavoro pianificato, ma la pratica è stata abbandonata in corsa. Con una sola persona che apre, assegna a se stessa ed evade ogni issue, la sovrastruttura di un tracker formale non aggiungeva informazione utile rispetto alla cronologia dei commit stessa: è un limite/scelta reale del processo, riportato qui invece che ricostruito come se non fosse mai successo.

## 4. Ciclo di Lavoro

Il ciclo effettivo per ogni step è stato: definizione dello step (una feature o un endpoint, derivato dalla roadmap) → generazione assistita → verifica (compilazione, avvio del servizio, esecuzione dei test, chiamata reale all'endpoint tramite l'API Gateway o direttamente al servizio) → commit → step successivo. Il principio applicato, non sempre enunciato esplicitamente ma seguito in pratica: non si avanza allo step successivo se il checkpoint di verifica dello step corrente non passa. Questo ha tenuto sotto controllo la deriva dell'implementazione rispetto ai documenti di contesto, pur senza le garanzie più forti di un ciclo TDD (vedi sezione Limiti).

## 5. Gestione Git

Lo sviluppo è **trunk-based**: tutti i commit sono diretti su `main`, non esistono branch di feature di lunga durata né merge commit nella cronologia (verificato: `git log --merges` restituisce zero risultati, `git branch -a` non mostra branch oltre `main`). Non è stato quindi seguito un flusso GitHub Flow con pull request: per una sola persona che lavora in sequenza su `main`, l'overhead di una pull request contro se stessi non avrebbe aggiunto valore.

I commit seguono la convenzione **Conventional Commits** (`tipo(scope): descrizione`), con lo scope quasi sempre allineato al nome del servizio toccato (`feat(payment): ...`, `fix(user-service): ...`, `docs: ...`). La cronologia Git funziona di fatto da changelog leggibile dell'evoluzione del sistema, ed è la ragione per cui la sezione 3 sopra parla di backlog abbandonato: la cronologia dei commit ha assolto la stessa funzione di tracciamento in modo più economico per una sola persona.

## 6. Controlli di Qualità

- **CI su GitHub Actions**: `ci-base.yml` valida `infra/docker-compose.yml` ad ogni push; ogni servizio ha una pipeline dedicata e path-filtrata (`.github/workflows/<servizio>.yml`) che builda, testa e, su `main`, distribuisce solo il servizio effettivamente modificato.
- **Test automatici**: JUnit 5 + Mockito per i test unitari, `@SpringBootTest` + Testcontainers per i test di integrazione (PostgreSQL o MongoDB e RabbitMQ reali in container).
- **JaCoCo** (copertura) e **PMD** (complessità ciclomatica, soglia 10 per metodo) sono configurati come gate di build in fase `verify` su User Service e Project Service, i due servizi scelti come core per la Service Architecture. Eseguendo `./mvnw verify` su entrambi (numeri verificati direttamente dai report generati, non stimati): User Service raggiunge il 98,1% di line coverage, Project Service il 99,6%, zero violazioni PMD su entrambi. Gli altri cinque servizi non hanno questo gate configurato.

## 7. Limiti della Metodologia Adottata

Questa sezione è deliberatamente onesta e non difensiva: elenca cosa non è stato fatto, non solo cosa è andato bene.

- **Test scritti dopo il codice, non prima.** Il TDD è considerato uno dei controlli più efficaci nello sviluppo assistito da agenti, perché il test funge da vincolo verificabile che limita la deriva dell'implementazione *prima* che il codice esista. In questo progetto i test sono stati scritti sistematicamente dopo l'implementazione: hanno svolto una funzione di verifica a posteriori, non di specifica a priori.
- **Frammentazione tra sessioni.** Come discusso in [ADR-008](adr/ADR-008-ai-assisted-development.md), lo sviluppo è avvenuto su più sessioni distinte, in alcuni casi parallele, che non condividono contesto conversazionale tra loro (solo i documenti versionati). Questo ha causato incoerenze reali tra parti del sistema sviluppate in parallelo, poi individuate e corrette in una fase di revisione successiva, invece di essere prevenute a monte.
- **Tracciabilità della generazione.** I commit non distinguono il codice generato da un agente da quello scritto o corretto a mano, né, quando più sessioni sono coinvolte, quale sessione abbia prodotto cosa. Questa distinzione non è ricostruibile a posteriori senza riscrivere la cronologia Git, operazione scartata perché avrebbe prodotto una documentazione non fedele ai fatti.
- **Review dell'output prevalentemente funzionale.** La verifica di ogni step si è basata su compilazione, esito dei test e comportamento reale dell'endpoint, più che su una review architetturale sistematica del codice generato rispetto alla specifica. Non è stato adottato un processo formale di code review.
- **Assenza di specifiche formali.** L'approccio adottato è *prompt-driven strutturato*: contesto architetturale ricco e versionato, ma espresso in linguaggio naturale (Markdown), non machine-readable. Approcci *spec-driven* (ad esempio contratti OpenAPI da cui generare gli stub dei controller, o framework che impongono un ciclo esplicito propose/approve/implement prima di generare codice) offrirebbero garanzie più forti di aderenza tra intenzione e implementazione. Non sono stati adottati in questo progetto.
- **Backlog abbandonato.** Discusso in sezione 3: un tentativo iniziale di tracciare il lavoro con GitHub Issues non è stato mantenuto.

## 8. Cosa Farei Diversamente

Se dovessi ripetere questo processo, i punti della sezione precedente indicano dove intervenire prima, non a posteriori:

- **Introdurrei almeno un test-first per la logica di business più delicata** (calcolo commissione, transizioni di stato di candidature e contratti, regola di reputazione), lasciando il resto della suite test-after: non un TDD integrale, ma un uso mirato dove il costo di un errore silenzioso è più alto.
- **Userei una sola sessione lunga per area di dominio, esplicitamente**, invece di lasciare che sessioni parallele si sovrappongano sulla stessa area senza coordinamento: la frammentazione descritta in ADR-008 è stata gestita a posteriori, sarebbe stato più economico prevenirla assegnando confini più netti fin dall'inizio.
- **Deciderei in anticipo se adottare o no un tracker di backlog**, invece di iniziare con GitHub Issues e abbandonarli in corsa: per una singola persona la cronologia dei commit è probabilmente sufficiente da sola, meglio saperlo prima che scoprirlo a metà.
- **Estenderei JaCoCo e PMD a tutti i servizi fin dall'inizio**, invece che solo ai due scelti come core per la documentazione: il gate ha effettivamente forzato la scrittura di test mirati sui due servizi dove è presente, un segnale che probabilmente vale anche per gli altri cinque.
