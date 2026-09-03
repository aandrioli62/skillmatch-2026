import { Alert, Box, CircularProgress, Divider, Paper, Typography } from '@mui/material'

export default function DataSection({ title, loading, error, isEmpty, emptyLabel, children }) {
  return (
    <Paper variant="outlined" sx={{ borderRadius: 3, overflow: 'hidden' }}>
      {title && (
        <>
          <Box sx={{ p: 2, px: 3 }}>
            <Typography variant="h6">{title}</Typography>
          </Box>
          <Divider />
        </>
      )}
      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
          <CircularProgress size={28} />
        </Box>
      )}
      {!loading && error && (
        <Alert severity="error" sx={{ borderRadius: 0 }}>
          Impossibile caricare i dati: {error}
        </Alert>
      )}
      {!loading && !error && isEmpty && (
        <Typography variant="body2" color="text.secondary" sx={{ p: 3 }}>
          {emptyLabel}
        </Typography>
      )}
      {!loading && !error && !isEmpty && children}
    </Paper>
  )
}
