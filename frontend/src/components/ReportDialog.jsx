import { Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField, Typography } from '@mui/material'
import { useState } from 'react'
import api from '../services/api'

export default function ReportDialog({ open, reportedUserId, onClose, onSubmitted }) {
  const [reason, setReason] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const handleClose = () => {
    if (submitting) return
    setReason('')
    setError(null)
    onClose()
  }

  const submit = () => {
    setSubmitting(true)
    setError(null)
    api
      .post('/reports', { reportedUserId, reason })
      .then(() => {
        setReason('')
        onSubmitted()
      })
      .catch((err) => setError(err.response?.data?.detail || err.message))
      .finally(() => setSubmitting(false))
  }

  return (
    <Dialog open={open} onClose={handleClose} fullWidth maxWidth="xs">
      <DialogTitle>Segnala</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Descrivi il problema riscontrato. Un amministratore esaminerà la segnalazione.
        </Typography>
        <TextField
          fullWidth
          multiline
          minRows={3}
          autoFocus
          label="Motivo"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          inputProps={{ maxLength: 2000 }}
          error={Boolean(error)}
          helperText={error}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={submitting}>
          Annulla
        </Button>
        <Button variant="contained" color="error" onClick={submit} disabled={submitting || !reason.trim()}>
          Invia segnalazione
        </Button>
      </DialogActions>
    </Dialog>
  )
}
