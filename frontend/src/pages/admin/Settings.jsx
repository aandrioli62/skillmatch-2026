import { Button, Paper, Snackbar, Stack, TextField, Typography } from '@mui/material'
import { useEffect, useState } from 'react'
import api from '../../services/api'
import { formatDate } from '../../utils/format'

export default function AdminSettings() {
  const [current, setCurrent] = useState(null)
  const [newRate, setNewRate] = useState('')
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState(null)

  const loadConfig = () => {
    api
      .get('/admin/commission-config')
      .then((res) => setCurrent(res.data))
      .catch((err) => setError(err.message))
  }

  useEffect(loadConfig, [])

  const submit = () => {
    setSaving(true)
    setError(null)
    api
      .put('/admin/commission-config', { ratePercentage: Number(newRate) })
      .then((res) => {
        setCurrent(res.data)
        setNewRate('')
        setMessage('Commissione aggiornata.')
      })
      .catch((err) => setError(err.response?.data?.detail || err.message))
      .finally(() => setSaving(false))
  }

  return (
    <>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 600 }}>
        Configurazione commissione
      </Typography>

      <Paper variant="outlined" sx={{ p: 3, borderRadius: 3, maxWidth: 420 }}>
        <Typography variant="body2" color="text.secondary">
          Commissione attuale
        </Typography>
        <Typography variant="h3" sx={{ fontWeight: 700, mb: 1 }}>
          {current ? `${current.ratePercentage}%` : '…'}
        </Typography>
        {current && (
          <Typography variant="caption" color="text.secondary">
            In vigore dal {formatDate(current.effectiveFrom)}
          </Typography>
        )}

        <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
          <TextField
            label="Nuova percentuale"
            type="number"
            value={newRate}
            onChange={(e) => setNewRate(e.target.value)}
            error={Boolean(error)}
            helperText={error}
            fullWidth
          />
          <Button variant="contained" onClick={submit} disabled={saving || !newRate}>
            Aggiorna
          </Button>
        </Stack>
        <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
          Le transazioni già effettuate non vengono ricalcolate: la nuova percentuale si applica solo ai pagamenti futuri.
        </Typography>
      </Paper>

      <Snackbar open={Boolean(message)} autoHideDuration={4000} onClose={() => setMessage(null)} message={message} />
    </>
  )
}
