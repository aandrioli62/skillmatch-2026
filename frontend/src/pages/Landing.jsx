import HandshakeIcon from '@mui/icons-material/Handshake'
import { Box, Button, Divider, Link as MuiLink, Paper, Stack, Typography } from '@mui/material'
import { Link, useNavigate } from 'react-router-dom'
import AuthBackground from '../components/AuthBackground'
import keycloak from '../keycloak'

export default function Landing() {
  const navigate = useNavigate()

  return (
    <AuthBackground>
      <Stack spacing={2} alignItems="center" sx={{ width: '100%', maxWidth: 420 }}>
      <Paper
        elevation={8}
        sx={{ p: 4, width: '100%', position: 'relative', zIndex: 1 }}
      >
        <Box
          sx={{
            width: 56,
            height: 56,
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            mb: 2,
            background: 'linear-gradient(135deg, #4f46e5, #14b8a6)',
          }}
        >
          <HandshakeIcon sx={{ color: 'white' }} />
        </Box>

        <Typography variant="h4" sx={{ mb: 1, fontWeight: 700 }}>
          SkillMatch
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Connette professionisti e aziende per micro-progetti a breve termine.
        </Typography>

        <Stack spacing={2}>
          <Button
            variant="contained"
            size="large"
            fullWidth
            onClick={() => keycloak.login({ redirectUri: window.location.origin + '/' })}
          >
            Accedi
          </Button>

          <Divider>oppure</Divider>

          <Typography variant="body2" align="center" color="text.secondary">
            Sei un professionista o un'azienda?
          </Typography>
          <Button variant="outlined" fullWidth onClick={() => navigate('/register?role=PROFESSIONAL')}>
            Registrati come professionista
          </Button>
          <Button variant="outlined" fullWidth onClick={() => navigate('/register?role=COMPANY')}>
            Registrati come azienda
          </Button>
        </Stack>
      </Paper>
      <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.85)', position: 'relative', zIndex: 1 }}>
        <MuiLink component={Link} to="/privacy" sx={{ color: 'inherit' }}>
          Privacy
        </MuiLink>
        {' · '}
        <MuiLink component={Link} to="/termini" sx={{ color: 'inherit' }}>
          Termini di Servizio
        </MuiLink>
      </Typography>
      </Stack>
    </AuthBackground>
  )
}
