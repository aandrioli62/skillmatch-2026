import { Alert, Box, Button, CircularProgress, Paper, Stack, TextField, Typography } from '@mui/material'
import { useState } from 'react'
import { useAuth } from '../hooks/useAuth'
import { useCurrentUser } from '../hooks/useCurrentUser'
import api from '../services/api'

// Safety net for Keycloak identities with no matching row in user-service yet —
// covers the pre-existing demo accounts (created directly in Keycloak) and any
// account created outside the self-service /register flow.
export default function RequireProfile({ children }) {
  const { keycloak, hasRole } = useAuth()
  // ADMIN accounts are never registered in user-service (no profile, no DB row
  // — they only exist as a Keycloak realm role), so this safety net doesn't
  // apply to them at all; the hook call still happens (rules of hooks), its
  // result is just ignored below.
  const { user, loading, notRegistered, refetch } = useCurrentUser()

  const role = hasRole('PROFESSIONAL') ? 'PROFESSIONAL' : hasRole('COMPANY') ? 'COMPANY' : null

  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [companyName, setCompanyName] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  if (hasRole('ADMIN')) {
    return children
  }

  if (loading) {
    return (
      <Box sx={{ display: 'flex', minHeight: '50vh', alignItems: 'center', justifyContent: 'center' }}>
        <CircularProgress />
      </Box>
    )
  }

  if (!notRegistered && user?.status === 'SUSPENDED') {
    return (
      <Box sx={{ display: 'flex', minHeight: '50vh', alignItems: 'center', justifyContent: 'center', p: 2 }}>
        <Alert severity="error" sx={{ maxWidth: 480 }}>
          Il tuo account è stato sospeso. Non puoi accedere a nessuna funzionalità della piattaforma finché un
          amministratore non lo riattiva.
        </Alert>
      </Box>
    )
  }

  if (!notRegistered) {
    return children
  }

  if (!role) {
    return (
      <Box sx={{ display: 'flex', minHeight: '50vh', alignItems: 'center', justifyContent: 'center', p: 2 }}>
        <Alert severity="warning">
          Il tuo account non ha un ruolo assegnato. Contatta un amministratore.
        </Alert>
      </Box>
    )
  }

  const submit = (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    api
      .post('/users', {
        keycloakId: keycloak.tokenParsed?.sub,
        email: keycloak.tokenParsed?.email,
        role,
      })
      .then((res) => {
        const userId = res.data.id
        if (role === 'PROFESSIONAL') {
          return api.put(`/users/${userId}/professional-profile`, { firstName, lastName })
        }
        return api.put(`/users/${userId}/company-profile`, { companyName })
      })
      .then(() => refetch())
      .catch((err) => setError(err.response?.data?.detail || err.message))
      .finally(() => setSubmitting(false))
  }

  return (
    <Box sx={{ display: 'flex', minHeight: '50vh', alignItems: 'center', justifyContent: 'center', p: 2 }}>
      <Paper sx={{ p: 4, maxWidth: 480, width: '100%' }} component="form" onSubmit={submit}>
        <Typography variant="h6" sx={{ mb: 2 }}>
          Completa la registrazione
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Il tuo account esiste ma manca ancora qualche informazione per usare la piattaforma.
        </Typography>
        <Stack spacing={2}>
          {role === 'PROFESSIONAL' && (
            <>
              <TextField
                label="Nome"
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                required
                fullWidth
              />
              <TextField
                label="Cognome"
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                required
                fullWidth
              />
            </>
          )}
          {role === 'COMPANY' && (
            <TextField
              label="Ragione sociale"
              value={companyName}
              onChange={(e) => setCompanyName(e.target.value)}
              required
              fullWidth
            />
          )}
          {error && <Alert severity="error">{error}</Alert>}
          <Button type="submit" variant="contained" disabled={submitting}>
            Continua
          </Button>
        </Stack>
      </Paper>
    </Box>
  )
}
