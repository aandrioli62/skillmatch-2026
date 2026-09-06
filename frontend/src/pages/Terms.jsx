import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import { Box, Button, Container, Divider, Paper, Typography } from '@mui/material'
import { Link } from 'react-router-dom'

export default function Terms() {
  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'grey.50', py: 6 }}>
      <Container maxWidth="md">
        <Button component={Link} to="/" startIcon={<ArrowBackIcon />} sx={{ mb: 2 }}>
          Torna alla home
        </Button>
        <Paper sx={{ p: { xs: 3, sm: 5 } }}>
          <Typography variant="h4" sx={{ fontWeight: 700, mb: 1 }}>
            Termini di Servizio
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            SkillMatch è un progetto sviluppato a scopo didattico per l'esame di Progettazione di
            Architetture di Servizi (Università del Salento). I pagamenti registrati sulla
            piattaforma sono simulati a fini dimostrativi: nessuna transazione reale viene
            eseguita.
          </Typography>
          <Divider sx={{ mb: 3 }} />

          <Typography variant="h6" sx={{ fontWeight: 700, mt: 3, mb: 1 }}>
            1. Oggetto del servizio
          </Typography>
          <Typography sx={{ mb: 2 }}>
            SkillMatch mette in contatto professionisti e aziende per lo svolgimento di
            micro-progetti a breve termine (formazione, consulenza, prototipazione).
          </Typography>

          <Typography variant="h6" sx={{ fontWeight: 700, mt: 3, mb: 1 }}>
            2. Ruoli e registrazione
          </Typography>
          <Typography sx={{ mb: 2 }}>
            La piattaforma prevede tre ruoli: Professionista, Azienda e Amministratore. I
            professionisti devono essere validati da un amministratore prima di poter candidarsi
            ai progetti pubblicati dalle aziende.
          </Typography>

          <Typography variant="h6" sx={{ fontWeight: 700, mt: 3, mb: 1 }}>
            3. Commissione della piattaforma
          </Typography>
          <Typography sx={{ mb: 2 }}>
            Sul pagamento di ogni contratto la piattaforma trattiene una commissione (8% di
            default), configurabile dagli amministratori. La fattura generata per l'azienda
            include sia il compenso del professionista sia la commissione trattenuta.
          </Typography>

          <Typography variant="h6" sx={{ fontWeight: 700, mt: 3, mb: 1 }}>
            4. Feedback e reputazione
          </Typography>
          <Typography sx={{ mb: 2 }}>
            Al termine di un contratto pagato, professionista e azienda possono lasciarsi
            reciprocamente un voto da 1 a 5 con un commento facoltativo. Il livello di reputazione
            del professionista (Junior, Affidabile, Top Performer) è calcolato automaticamente a
            partire dai feedback ricevuti.
          </Typography>

          <Typography variant="h6" sx={{ fontWeight: 700, mt: 3, mb: 1 }}>
            5. Segnalazioni e sospensione
          </Typography>
          <Typography sx={{ mb: 2 }}>
            Un utente può segnalare la controparte di un contratto in caso di comportamento
            scorretto. Un amministratore esamina la segnalazione e può, a propria discrezione,
            archiviarla o sospendere l'account coinvolto.
          </Typography>

          <Typography variant="h6" sx={{ fontWeight: 700, mt: 3, mb: 1 }}>
            6. Limitazione di responsabilità
          </Typography>
          <Typography sx={{ mb: 2 }}>
            Trattandosi di un progetto didattico, la piattaforma è fornita "così com'è", senza
            alcuna garanzia, e non è idonea a gestire transazioni economiche reali.
          </Typography>

          <Typography variant="h6" sx={{ fontWeight: 700, mt: 3, mb: 1 }}>
            7. Modifiche ai termini
          </Typography>
          <Typography sx={{ mb: 2 }}>
            Questi termini possono essere aggiornati in qualsiasi momento; la versione in vigore è
            sempre quella pubblicata su questa pagina.
          </Typography>
        </Paper>
      </Container>
    </Box>
  )
}
