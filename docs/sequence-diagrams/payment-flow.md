# Flusso di pagamento e fatturazione

Questo diagramma descrive il pagamento di un contratto completato: la verifica sincrona dei dati del contratto tramite Circuit Breaker, il calcolo di commissione e importo netto, la generazione della fattura unica e gli eventi asincroni che abilitano il feedback reciproco e le notifiche. Precondizione: il contratto e' gia' `ACTIVE`, cioe' entrambe le parti lo hanno firmato.

```mermaid
sequenceDiagram
    actor Company as Company (Browser)
    participant GW as API Gateway
    participant PJS as Project Service
    participant CS as Contract Service
    participant PS as Payment Service
    participant MQ as RabbitMQ (exchange skillmatch.events)
    participant FS as Feedback Service
    participant NS as Notification Service

    Company->>GW: PUT /api/v1/projects/{projectId}/complete
    GW->>PJS: PUT /api/v1/projects/{projectId}/complete
    PJS->>PJS: Segna il progetto come completato

    PJS-->>MQ: project.completed
    Note over PJS,NS: asincrono via RabbitMQ (skillmatch.events). Consumato solo dal Notification Service: nessun altro servizio reagisce automaticamente a questo evento.
    MQ-->>NS: project.completed
    NS->>NS: Notifica entrambe le parti

    Company->>GW: PUT /api/v1/contracts/{contractId}/complete (ruolo COMPANY)
    GW->>CS: PUT /api/v1/contracts/{contractId}/complete
    CS->>CS: status ACTIVE -> COMPLETED

    Company->>GW: POST /api/v1/payments (contractId, ruolo COMPANY)
    GW->>PS: POST /api/v1/payments

    PS->>CS: GET dettagli contratto
    Note over PS,CS: chiamata REST sincrona protetta da Circuit Breaker
    CS->>PS: Importo, professionalId, companyId, status

    PS->>PS: Verifica: nessun pagamento gia' esistente per il contratto, chiamante proprietario del contratto, stato contratto = COMPLETED
    PS->>PS: Legge tasso di commissione corrente da commission_config (es. 8%)
    PS->>PS: Calcola commission_amount = totale * tasso / 100, net_amount = totale - commission_amount
    PS->>PS: Crea Transaction (status = COMPLETED, trasferimento simulato sincrono/istantaneo) e Invoice unica (numero tipo INV-2026-000042), salva su paymentdb

    PS-->>MQ: payment.completed (projectId/contractId, companyId, professionalId, commissionAmount, ecc.)
    Note over PS,NS: asincrono via RabbitMQ (skillmatch.events)

    MQ-->>FS: payment.completed
    Note over MQ,FS: coda feedback.payment.completed
    FS->>FS: Crea riga feedback_eligibility per il progetto (abilita il feedback reciproco)

    MQ-->>NS: payment.completed
    Note over MQ,NS: binding #
    NS->>NS: Notifica l'azienda ("Pagamento elaborato e fattura generata.") e il professionista ("Hai ricevuto un pagamento...")

    Company->>GW: GET /api/v1/transactions/{transactionId}/invoice
    GW->>PS: GET /api/v1/transactions/{transactionId}/invoice
    PS->>Company: Fattura
```
