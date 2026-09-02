import { Box, Button, Typography } from '@mui/material'
import { useAuth } from '../context/AuthContext'

export default function Unauthorized() {
  const { logout } = useAuth()

  return (
    <Box sx={{ textAlign: 'center', mt: 8 }}>
      <Typography variant="h4" gutterBottom>
        Accesso non autorizzato
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
        Il tuo account non ha un ruolo valido per accedere a questa applicazione.
      </Typography>
      <Button variant="contained" onClick={logout}>
        Torna al login
      </Button>
    </Box>
  )
}
