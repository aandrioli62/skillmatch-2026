import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Typography,
} from '@mui/material'
import { useAuth } from '../hooks/useAuth'

const FAQ_BY_ROLE = {
  PROFESSIONAL: [
    {
      q: 'Come mi registro e quando posso iniziare a candidarmi?',
      a: 'Ti registri con nome, cognome ed email. Un amministratore deve validare il tuo profilo prima che tu possa candidarti a un progetto: nel frattempo completa competenze e portfolio dal tuo profilo, aiuta ad essere scelto più in fretta.',
    },
    {
      q: 'Come funziona la validazione del mio profilo?',
      a: 'Un amministratore esamina il tuo profilo (bio, competenze, portfolio) e lo valida. Finché non sei validato non puoi inviare candidature ai progetti.',
    },
    {
      q: 'Quando e come vengo pagato?',
      a: "Quando l'azienda segna il progetto come completato, può avviare il pagamento dalla piattaforma. Ricevi l'importo netto: la piattaforma trattiene una commissione (8% di default, può cambiare) e genera la fattura per l'azienda.",
    },
    {
      q: 'Cosa significano i livelli Junior, Affidabile e Top Performer?',
      a: 'Sono calcolati sulle recensioni ricevute: Junior con media sotto 3,5 o meno di 3 recensioni; Affidabile con media almeno 3,5 e almeno 3 recensioni; Top Performer con media almeno 4,5 e almeno 10 recensioni.',
    },
    {
      q: 'Come lascio o ricevo un feedback?',
      a: 'Dopo che il pagamento è stato completato, sia tu che azienda potete lasciarvi un voto da 1 a 5 con un commento facoltativo.',
    },
    {
      q: 'Cosa succede se una candidatura viene rifiutata?',
      a: "La candidatura passa allo stato \"Rifiutata\": puoi continuare a candidarti liberamente ad altri progetti aperti.",
    },
    {
      q: "Come segnalo un problema con un'azienda?",
      a: 'Dal dettaglio del contratto trovi il pulsante "Segnala": un amministratore esaminerà la segnalazione e deciderà se intervenire.',
    },
  ],
  COMPANY: [
    {
      q: 'Come pubblico un progetto?',
      a: 'Crei un progetto in bozza con titolo, descrizione, budget, durata e competenze richieste, poi lo pubblichi per renderlo visibile ai professionisti validati.',
    },
    {
      q: 'Come scelgo un candidato?',
      a: 'Dalla lista delle candidature ricevute puoi accettarne una (le altre in attesa vengono rifiutate automaticamente) oppure rifiutarle singolarmente senza chiudere il progetto.',
    },
    {
      q: 'Quanto costa usare la piattaforma?',
      a: "Sul pagamento viene trattenuta una commissione (8% di default, l'amministratore può cambiarla). La fattura che ricevi include sia il compenso del professionista sia la commissione.",
    },
    {
      q: 'Come pago un professionista?',
      a: "Quando il progetto è completato, avvii il pagamento dalla piattaforma: viene generata automaticamente la fattura.",
    },
    {
      q: 'Posso vedere quanto è affidabile un professionista?',
      a: 'Sì, ogni professionista mostra un livello di reputazione (Junior, Affidabile, Top Performer) calcolato sulle recensioni ricevute dalle aziende con cui ha lavorato.',
    },
    {
      q: 'Cosa faccio se un professionista non rispetta gli accordi?',
      a: 'Puoi segnalarlo dal dettaglio del contratto: un amministratore esaminerà la segnalazione e potrà sospendere l\'account coinvolto.',
    },
  ],
  ADMIN: [
    {
      q: 'Come valido un professionista?',
      a: 'Da "Validazione utenti" apri il profilo del professionista (competenze, portfolio) e confermi la validazione dal dialog di dettaglio.',
    },
    {
      q: 'Come cambio la commissione della piattaforma?',
      a: 'Da "Configurazione commissione" imposti una nuova percentuale: è effettiva da subito sui pagamenti futuri, quelli passati non vengono ricalcolati.',
    },
    {
      q: 'Cosa vedo nella sezione Transazioni?',
      a: "L'elenco di tutti i pagamenti della piattaforma, filtrabile per stato e intervallo di date, con i totali aggregati (volume, commissioni incassate, netto ai professionisti) per i filtri selezionati.",
    },
    {
      q: 'Come gestisco le segnalazioni tra utenti?',
      a: 'Dal profilo di un utente vedi le segnalazioni aperte a suo carico: puoi archiviarle se non richiedono azione, oppure sospendere l\'account coinvolto.',
    },
    {
      q: 'Perché le aziende non hanno uno stato "in attesa"?',
      a: 'Solo i professionisti richiedono una validazione prima di poter operare sulla piattaforma; per le aziende quello stato non si applica, quindi non viene mostrato.',
    },
  ],
}

function primaryRole(hasRole) {
  if (hasRole('ADMIN')) return 'ADMIN'
  if (hasRole('COMPANY')) return 'COMPANY'
  if (hasRole('PROFESSIONAL')) return 'PROFESSIONAL'
  return null
}

export default function Faq() {
  const { hasRole } = useAuth()
  const role = primaryRole(hasRole)
  const items = role ? FAQ_BY_ROLE[role] : []

  return (
    <>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 600 }}>
        Domande frequenti
      </Typography>
      {items.map((item) => (
        <Accordion key={item.q} disableGutters>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography sx={{ fontWeight: 600 }}>{item.q}</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Typography color="text.secondary">{item.a}</Typography>
          </AccordionDetails>
        </Accordion>
      ))}
    </>
  )
}
