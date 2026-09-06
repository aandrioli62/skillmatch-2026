import { Box, Button, Paper, Typography } from '@mui/material'
import { Component } from 'react'

// React only invokes componentDidCatch/getDerivedStateFromError on class
// components — there is no hook equivalent, so this can't be a function component.
export default class ErrorBoundary extends Component {
  state = { hasError: false }

  static getDerivedStateFromError() {
    return { hasError: true }
  }

  componentDidCatch(error, info) {
    console.error('Unhandled UI error:', error, info)
  }

  render() {
    if (!this.state.hasError) {
      return this.props.children
    }

    return (
      <Box
        sx={{
          display: 'flex',
          minHeight: '100vh',
          alignItems: 'center',
          justifyContent: 'center',
          p: 2,
          bgcolor: 'grey.50',
        }}
      >
        <Paper sx={{ p: 4, maxWidth: 420, width: '100%', textAlign: 'center' }}>
          <Typography variant="h6" sx={{ mb: 1, fontWeight: 700 }}>
            Qualcosa è andato storto
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Si è verificato un errore imprevisto. Prova a ricaricare la pagina.
          </Typography>
          <Button variant="contained" onClick={() => window.location.reload()}>
            Ricarica la pagina
          </Button>
        </Paper>
      </Box>
    )
  }
}
