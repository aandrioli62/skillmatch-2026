import { Button, Dialog, DialogActions, DialogContent, DialogTitle, Rating, TextField, Typography } from '@mui/material'
import { useState } from 'react'
import api from '../services/api'

export default function FeedbackDialog({ open, projectId, onClose, onSubmitted }) {
  const [rating, setRating] = useState(0)
  const [comment, setComment] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const handleClose = () => {
    if (submitting) return
    setRating(0)
    setComment('')
    setError(null)
    onClose()
  }

  const submit = () => {
    setSubmitting(true)
    setError(null)
    api
      .post('/feedbacks', { projectId, rating, comment })
      .then(() => {
        setRating(0)
        setComment('')
        onSubmitted()
      })
      .catch((err) => setError(err.response?.data?.detail || err.message))
      .finally(() => setSubmitting(false))
  }

  return (
    <Dialog open={open} onClose={handleClose} fullWidth maxWidth="xs">
      <DialogTitle>Lascia un feedback</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Come valuti la collaborazione su questo progetto?
        </Typography>
        <Rating value={rating} onChange={(_, value) => setRating(value ?? 0)} size="large" />
        <TextField
          fullWidth
          multiline
          minRows={3}
          label="Commento (opzionale)"
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          inputProps={{ maxLength: 2000 }}
          sx={{ mt: 2 }}
          error={Boolean(error)}
          helperText={error}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={submitting}>
          Annulla
        </Button>
        <Button variant="contained" onClick={submit} disabled={submitting || rating === 0}>
          Invia feedback
        </Button>
      </DialogActions>
    </Dialog>
  )
}
