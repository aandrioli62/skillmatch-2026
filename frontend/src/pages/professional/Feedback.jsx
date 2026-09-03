import { Box, List, ListItem, ListItemText, Paper, Rating, Typography } from '@mui/material'
import { useEffect, useState } from 'react'
import DataSection from '../../components/DataSection'
import api from '../../services/api'
import { formatDate } from '../../utils/format'

export default function ProfessionalFeedback() {
  const [feedback, setFeedback] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    api
      .get('/feedbacks/received/mine')
      .then((res) => setFeedback(res.data))
      .catch((err) => setError(err.message))
  }, [])

  const average = feedback?.length
    ? (feedback.reduce((sum, f) => sum + f.rating, 0) / feedback.length).toFixed(1)
    : null

  return (
    <>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 600 }}>
        Feedback ricevuti
      </Typography>

      {average && (
        <Paper variant="outlined" sx={{ p: 3, borderRadius: 3, mb: 3, display: 'inline-flex', alignItems: 'center', gap: 2 }}>
          <Typography variant="h4" sx={{ fontWeight: 700 }}>
            {average}
          </Typography>
          <Rating value={Number(average)} precision={0.1} readOnly />
          <Typography variant="body2" color="text.secondary">
            ({feedback.length} {feedback.length === 1 ? 'recensione' : 'recensioni'})
          </Typography>
        </Paper>
      )}

      <DataSection
        loading={feedback === null && !error}
        error={error}
        isEmpty={feedback?.length === 0}
        emptyLabel="Non hai ancora ricevuto feedback."
      >
        <List disablePadding>
          {feedback?.map((item) => (
            <ListItem key={item.id} divider sx={{ alignItems: 'flex-start' }}>
              <ListItemText
                primary={
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Rating value={item.rating} readOnly size="small" />
                    <Typography variant="caption" color="text.secondary">
                      {formatDate(item.createdAt)}
                    </Typography>
                  </Box>
                }
                secondary={item.comment || 'Nessun commento'}
              />
            </ListItem>
          ))}
        </List>
      </DataSection>
    </>
  )
}
