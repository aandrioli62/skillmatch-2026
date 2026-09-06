import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import { Box, Button, Container, Divider, Paper, Typography } from '@mui/material'
import { Link } from 'react-router-dom'

export default function Privacy() {
  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'grey.50', py: 6 }}>
      <Container maxWidth="md">
        <Button component={Link} to="/" startIcon={<ArrowBackIcon />} sx={{ mb: 2 }}>
          Torna alla home
        </Button>
        <Paper sx={{ p: { xs: 3, sm: 5 } }}>
          <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
            Informativa Privacy
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            SkillMatch è un progetto sviluppato a scopo didattico per l'esame di Progettazione di
            Architetture di Servizi (Università del Salento). Questa informativa descrive, a scopo
            dimostrativo, quali dati la piattaforma tratta e come.
          </Typography>
          <Divider sx={{ mb: 3 }} />

          <Typography variant="h6" sx={{ fontWeight: 700, mt: 3, mb: 1 }}>
            1. Titolare del trattamento
          </Typography>
          <Typography sx={{ mb: 2 }}>
            Il titolare del trattamento dei dati è il gestore della piattaforma SkillMatch. Per
            qualsiasi richiesta relativa ai propri dati, l'utente può contattare un amministratore
            tramite gli strumenti disponibili nella piattaforma.
          </Typography>

          <Typography variant="h6" sx={{ fontWeight: 700, mt: 3, mb: 1 }}>
            2. Dati raccolti
          </Typography>
          <Typography component="div" sx={{ mb: 2 }}>
            A seconda del ruolo, raccogliamo:
            <ul>
              <li>Professionisti: nome, cognome, email, bio, competenze, portfolio, conto di pagamento indicato.</li>
              <li>Aziende: ragione sociale, email, dati aziendali indicati nel profilo.</li>
              <li>Tutti gli utenti: candidature, contratti, transazioni, feedback e segnalazioni generati usando la piattaforma.</li>
            </ul>
          </Typography>

          <Typography variant="h6" sx={{ fontWeight: 700, mt: 3, mb: 1 }}>
            3. Finalità del trattamento
          </Typography>
          <Typography sx={{ mb: 2 }}>
            I dati sono trattati esclusivamente per far funzionare la piattaforma: mettere in
            contatto professionisti e aziende, gestire candidature e contratti, calcolare
            commissioni e generare fatture, calcolare il livello di reputazione a partire dai
            feedback ricevuti, e gestire eventuali segnalazioni tra utenti.
          </Typography>

          <Typography variant="h6" sx={{ fontWeight: 700, mt: 3, mb: 1 }}>
            4. Conservazione e condivisione
          </Typography>
          <Typography sx={{ mb: 2 }}>
            I dati sono conservati nei database interni della piattaforma per il tempo in cui
            l'account resta attivo. Non vengono condivisi con soggetti terzi al di fuori dei
            servizi che compongono la piattaforma stessa (gestione utenti, progetti, contratti,
            pagamenti, feedback, notifiche).
          </Typography>

          <Typography variant="h6" sx={{ fontWeight: 700, mt: 3, mb: 1 }}>
            5. Cookie
          </Typography>
          <Typography sx={{ mb: 2 }}>
            La piattaforma usa un cookie tecnico di sessione, necessario per mantenere l'accesso
            effettuato tramite il sistema di autenticazione, e la memoria locale del browser per
            alcune preferenze di visualizzazione. Non sono presenti cookie di profilazione o di
            terze parti.
          </Typography>

          <Typography variant="h6" sx={{ fontWeight: 700, mt: 3, mb: 1 }}>
            6. Diritti dell'utente
          </Typography>
          <Typography sx={{ mb: 2 }}>
            L'utente può richiedere in qualsiasi momento accesso, correzione o cancellazione dei
            propri dati contattando un amministratore della piattaforma.
          </Typography>

        </Paper>
      </Container>
    </Box>
  )
}
