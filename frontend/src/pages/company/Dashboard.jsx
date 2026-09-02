import { Box, Paper, Typography } from '@mui/material'
import { useAuth } from '../../context/AuthContext'

const STATS = [
  { label: 'I miei progetti', value: '—' },
  { label: 'Candidati in attesa', value: '—' },
  { label: 'Contratti attivi', value: '—' },
]

export default function CompanyDashboard() {
  const { username } = useAuth()

  return (
    <>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 600 }}>
        Bentornato, {username}
      </Typography>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' }, gap: 2 }}>
        {STATS.map((stat) => (
          <Paper key={stat.label} variant="outlined" sx={{ p: 3, borderRadius: 3 }}>
            <Typography variant="body2" color="text.secondary">
              {stat.label}
            </Typography>
            <Typography variant="h4" sx={{ mt: 1, fontWeight: 700 }}>
              {stat.value}
            </Typography>
          </Paper>
        ))}
      </Box>
    </>
  )
}
