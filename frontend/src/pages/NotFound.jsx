import { Box, Button, Typography } from '@mui/material'
import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <Box sx={{ textAlign: 'center', mt: 8 }}>
      <Typography variant="h4" gutterBottom>
        404 — Pagina non trovata
      </Typography>
      <Button component={Link} to="/" variant="contained" sx={{ mt: 2 }}>
        Torna alla home
      </Button>
    </Box>
  )
}
