import ConstructionIcon from '@mui/icons-material/Construction'
import { Box, Paper, Typography } from '@mui/material'

export default function PlaceholderPage({ title }) {
  return (
    <Paper
      variant="outlined"
      sx={{ p: 6, textAlign: 'center', borderRadius: 3, borderStyle: 'dashed' }}
    >
      <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 1.5 }}>
        <ConstructionIcon color="disabled" sx={{ fontSize: 40 }} />
        <Typography variant="h6">{title}</Typography>
        <Typography variant="body2" color="text.secondary">
          Questa schermata verrà implementata nella Fase 7.2.
        </Typography>
      </Box>
    </Paper>
  )
}
