# Use Case: Professionista

Il professionista e lo stakeholder che offre le proprie competenze sulla piattaforma SkillMatch per svolgere micro-progetti su commissione delle aziende. Il suo percorso tipico comprende la registrazione e la validazione del profilo, la candidatura ai progetti aperti, la firma dei micro-contratti e, a lavoro concluso, la ricezione del compenso e dei feedback. Di seguito sono descritti i casi d'uso principali con i relativi endpoint REST reali esposti dai microservizi coinvolti.

```mermaid
flowchart LR
    Prof(["Professionista"])
    UC1((Registrazione e completamento profilo))
    UC2((Candidatura a un progetto))
    UC3((Firma del micro-contratto))
    UC4((Visualizzazione feedback e reputazione))
    UC5((Ricezione del compenso))
    UCValida((Validazione profilo))
    UCContratto((Creazione automatica contratto))

    Prof --> UC1
    Prof --> UC2
    Prof --> UC3
    Prof --> UC4
    Prof --> UC5

    UC1 -.include.-> UCValida
    UC2 -.extends.-> UCContratto
    UC3 -.include.-> UCContratto
```

## Caso d'uso 1: Registrazione e completamento profilo

**Attore**: Professionista

**Precondizioni**: L'utente possiede un account Keycloak nel realm `skillmatch` oppure e in fase di registrazione.

**Flusso principale**:
1. Il professionista si autentica tramite Keycloak usando il flusso Authorization Code + PKCE.
2. Il frontend chiama `POST /api/v1/users` (User Service) per creare il record `User`, che viene salvato con `status=PENDING`.
3. Lo User Service pubblica l'evento `user.registered` sull'exchange `skillmatch.events`, consumato dal Notification Service, che invia una notifica di benvenuto.
4. Il professionista completa il proprio profilo con `PUT /api/v1/users/{userId}/professional-profile` (richiede ruolo PROFESSIONAL), inserendo nome, cognome, bio, conto di pagamento (`payment_account`), competenze e portfolio.
5. Il profilo resta in attesa di validazione da parte di un ADMIN (vedi caso d'uso admin "Validazione dei professionisti", `POST /api/v1/admin/users/{userId}/validate`), che porta `status` a `VALIDATED` e pubblica l'evento `user.validated`.

**Postcondizioni**: L'utente ha un account con profilo professionale compilato. Solo dopo la validazione dell'admin lo `status` diventa `VALIDATED` e il professionista puo candidarsi ai progetti.

## Caso d'uso 2: Candidatura a un progetto

**Attore**: Professionista

**Precondizioni**: Il professionista e autenticato e il proprio profilo ha `status=VALIDATED`.

**Flusso principale**:
1. Il professionista consulta i progetti aperti con `GET /api/v1/projects/open`.
2. Sceglie un progetto e invia la candidatura con `POST /api/v1/projects/{projectId}/candidatures` (ruolo PROFESSIONAL).
3. Il Project Service verifica sincronamente, tramite chiamata REST protetta da Circuit Breaker Resilience4j, che lo `status` dell'utente presso lo User Service sia `VALIDATED`, applicando la regola di business "solo chi supera la validazione puo candidarsi ai progetti".
4. Se la verifica ha esito positivo, la candidatura viene registrata e il professionista puo consultarla con `GET /api/v1/projects/candidatures/mine`.
5. Se l'azienda accetta la candidatura tramite `PUT /api/v1/projects/{projectId}/candidatures/{candidatureId}/accept`, viene pubblicato l'evento `candidature.accepted`, che fa nascere automaticamente un micro-contratto in stato `DRAFT` presso il Contract Service.

**Postcondizioni**: La candidatura risulta registrata (e successivamente accettata o rifiutata dall'azienda). In caso di accettazione esiste un contratto `DRAFT` associato pronto per la firma.

## Caso d'uso 3: Firma del micro-contratto

**Attore**: Professionista

**Precondizioni**: Esiste un contratto in stato `DRAFT` o `PENDING_SIGNATURES` generato a seguito dell'accettazione della candidatura, e l'azienda ha gia firmato per prima.

**Flusso principale**:
1. Il professionista consulta il contratto proposto (creato automaticamente in stato `DRAFT` dal Contract Service in seguito all'evento `candidature.accepted`).
2. L'azienda firma per prima, portando il contratto da `DRAFT` a `PENDING_SIGNATURES`.
3. Il professionista firma a sua volta con `PUT /api/v1/contracts/{contractId}/sign`.
4. Con entrambe le firme presenti, il contratto transita da `PENDING_SIGNATURES` ad `ACTIVE`.

**Postcondizioni**: Il contratto e `ACTIVE` e il micro-progetto puo iniziare. Al termine del lavoro l'azienda segnera il progetto come completato e chiudera il contratto (`ACTIVE`->`COMPLETED`).

## Caso d'uso 4: Visualizzazione feedback e reputazione

**Attore**: Professionista

**Precondizioni**: Il contratto e stato completato e il pagamento e avvenuto (evento `payment.completed` pubblicato dal Payment Service).

**Flusso principale**:
1. Il Feedback Service riceve l'evento `payment.completed` e abilita la valutazione reciproca per il progetto.
2. Il professionista lascia un feedback all'azienda tramite `POST /api/v1/feedbacks`, indicando un rating da 1 a 5 e un commento opzionale.
3. Riceve a sua volta un feedback dall'azienda tramite lo stesso endpoint, invocato dal lato azienda.
4. Consulta i feedback ricevuti con `GET /api/v1/feedbacks/received/mine` e quelli dati con `GET /api/v1/feedbacks/given/mine`.
5. Alla ricezione di un nuovo feedback, viene pubblicato l'evento `feedback.aggregated`, consumato dallo User Service, che ricalcola automaticamente il livello di reputazione secondo le regole: Junior (media inferiore a 3.5 oppure recensioni totali inferiori a 3), Affidabile (media maggiore o uguale a 3.5 e recensioni totali maggiori o uguali a 3), Top Performer (media maggiore o uguale a 4.5 e recensioni totali maggiori o uguali a 10).

**Postcondizioni**: Il feedback e registrato e il livello di reputazione del professionista e aggiornato e consultabile pubblicamente sul suo profilo.

## Caso d'uso 5: Ricezione del compenso

**Attore**: Professionista

**Precondizioni**: Il contratto e `COMPLETED` e l'azienda ha avviato il pagamento tramite `POST /api/v1/payments`.

**Flusso principale**:
1. Il Payment Service calcola la commissione di piattaforma (percentuale configurata dall'admin, di default 8%) e l'importo netto spettante al professionista.
2. Il Payment Service registra la transazione e genera un'unica fattura per l'azienda, comprensiva di compenso e commissione.
3. Viene pubblicato l'evento `payment.completed`, che notifica il professionista dell'avvenuto pagamento e abilita il flusso di feedback reciproco.
4. Il professionista consulta le proprie transazioni con `GET /api/v1/transactions/professional/mine`.

**Postcondizioni**: Il professionista ha ricevuto il compenso netto sul proprio conto di pagamento e puo visualizzare lo storico delle transazioni.
