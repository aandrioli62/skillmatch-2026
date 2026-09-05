import {
  Alert,
  Button,
  Link as MuiLink,
  Paper,
  Stack,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import AuthBackground from '../components/AuthBackground'
import keycloak from '../keycloak'
import { publicApi } from '../services/api'

export default function Register() {
  const [searchParams] = useSearchParams()
  const initialRole = searchParams.get('role') === 'COMPANY' ? 'COMPANY' : 'PROFESSIONAL'

  const [role, setRole] = useState(initialRole)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [companyName, setCompanyName] = useState('')

  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)
  const [done, setDone] = useState(false)

  const submit = (e) => {
    e.preventDefault()
    setError(null)

    if (password !== confirmPassword) {
      setError('Le password non coincidono.')
      return
    }

    setSubmitting(true)
    publicApi
      .post('/auth/register', {
        email,
        password,
        role,
        firstName: role === 'PROFESSIONAL' ? firstName : undefined,
        lastName: role === 'PROFESSIONAL' ? lastName : undefined,
        companyName: role === 'COMPANY' ? companyName : undefined,
      })
      .then(() => setDone(true))
      .catch((err) => setError(err.response?.data?.detail || err.message))
      .finally(() => setSubmitting(false))
  }

  if (done) {
    return (
      <AuthBackground>
        <Paper elevation={8} sx={{ p: 4, maxWidth: 480, width: '100%', position: 'relative', zIndex: 1 }}>
          <Typography variant="h6" sx={{ mb: 2 }}>
            Account creato
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Ora puoi accedere con l'email e la password appena scelte.
          </Typography>
          <Button
            variant="contained"
            fullWidth
            onClick={() => keycloak.login({ redirectUri: window.location.origin + '/' })}
          >
            Vai al login
          </Button>
        </Paper>
      </AuthBackground>
    )
  }

  return (
    <AuthBackground>
      <Paper
        elevation={8}
        sx={{ p: 4, maxWidth: 480, width: '100%', position: 'relative', zIndex: 1 }}
        component="form"
        onSubmit={submit}
      >
        <Typography variant="h5" sx={{ mb: 3, fontWeight: 700 }}>
          Crea un account SkillMatch
        </Typography>
        <Stack spacing={2}>
          <ToggleButtonGroup
            color="primary"
            exclusive
            fullWidth
            value={role}
            onChange={(e, value) => value && setRole(value)}
          >
            <ToggleButton type="button" value="PROFESSIONAL">Professionista</ToggleButton>
            <ToggleButton type="button" value="COMPANY">Azienda</ToggleButton>
          </ToggleButtonGroup>

          <TextField
            label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            fullWidth
          />
          <TextField
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            fullWidth
            helperText="Almeno 8 caratteri"
          />
          <TextField
            label="Conferma password"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
            fullWidth
          />

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

          <Button type="submit" variant="contained" disabled={submitting} fullWidth>
            Registrati
          </Button>

          <Typography variant="body2" align="center">
            Hai già un account?{' '}
            <MuiLink
              component="button"
              type="button"
              onClick={() => keycloak.login({ redirectUri: window.location.origin + '/' })}
            >
              Accedi
            </MuiLink>
          </Typography>
        </Stack>
      </Paper>
    </AuthBackground>
  )
}
