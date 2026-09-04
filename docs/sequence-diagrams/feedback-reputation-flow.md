# Flusso di feedback reciproco e aggiornamento della reputazione

Questo diagramma mostra come azienda e professionista si scambiano feedback al termine di un progetto pagato, e come il feedback ricevuto da un professionista aggiorni in modo asincrono la sua reputazione nello User Service. Precondizione: esiste gia' una riga `feedback_eligibility` per il progetto, creata dal flusso di pagamento.

```mermaid
sequenceDiagram
    actor Company as Company (Browser)
    actor Professional as Professional (Browser)
    participant GW as API Gateway
    participant FS as Feedback Service
    participant MQ as RabbitMQ (exchange skillmatch.events)
    participant US as User Service
    participant NS as Notification Service

    Company->>GW: POST /api/v1/feedbacks (projectId, rating 1-5, comment opzionale)
    GW->>FS: POST /api/v1/feedbacks
    Note over Company,FS: L'azienda valuta il professionista
    FS->>FS: Verifica eleggibilita' (feedback_eligibility), determina revieweeId = professionista
    FS->>FS: Verifica assenza di un feedback gia' esistente per questa coppia e progetto (vincolo UNIQUE), salva la riga in feedbacks

    FS->>FS: Ricalcola avg_rating e total_reviews leggendo tutti i feedback ricevuti dal professionista
    Note over FS: Il ricalcolo avviene perche' il destinatario e' un professionista
    FS-->>MQ: feedback.aggregated
    Note over FS,NS: asincrono via RabbitMQ (skillmatch.events)

    MQ-->>US: feedback.aggregated
    Note over MQ,US: coda user-service.feedback.aggregated
    US->>US: Aggiorna professional_profiles: avg_rating, total_reviews, reputation_level
    Note over US: Regola reputazione: Junior se media < 3.5 O recensioni < 3; Affidabile se media >= 3.5 E recensioni >= 3; Top Performer se media >= 4.5 E recensioni >= 10

    MQ-->>NS: feedback.aggregated
    Note over MQ,NS: binding #
    NS->>NS: Notifica il professionista che la sua reputazione e' stata aggiornata

    rect rgb(230, 240, 250)
    Note over Professional,FS: Flusso simmetrico e parallelo: il professionista valuta l'azienda
    Professional->>GW: POST /api/v1/feedbacks (projectId, rating, comment opzionale)
    GW->>FS: POST /api/v1/feedbacks
    FS->>FS: Verifica eleggibilita', determina revieweeId = azienda, verifica UNIQUE, salva la riga in feedbacks
    Note over FS: In questo caso il Feedback Service NON pubblica feedback.aggregated, perche' nel sistema solo i professionisti accumulano una reputazione aggregata (le aziende non hanno un punteggio di reputazione)
    end

    Company->>GW: GET /api/v1/feedbacks/given/mine
    Professional->>GW: GET /api/v1/feedbacks/received/mine
    GW->>FS: GET /api/v1/feedbacks/given/mine e /api/v1/feedbacks/received/mine
    FS->>Company: Feedback dati e ricevuti
    FS->>Professional: Feedback dati e ricevuti
```
