import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  List,
  ListItem,
  ListItemText,
  Snackbar,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useEffect, useState } from 'react'
import DataSection from '../../components/DataSection'
import api from '../../services/api'
import { candidatureStatusInfo } from '../../utils/format'

export default function ProfessionalProjects() {
  const [projects, setProjects] = useState(null)
  const [projectsError, setProjectsError] = useState(null)

  const [candidatures, setCandidatures] = useState(null)

  const [applyTarget, setApplyTarget] = useState(null)
  const [coverLetter, setCoverLetter] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState(null)
  const [successMessage, setSuccessMessage] = useState(null)

  const loadData = () => {
    api
      .get('/projects/open')
      .then((res) => setProjects(res.data))
      .catch((err) => setProjectsError(err.message))

    api
      .get('/projects/candidatures/mine')
      .then((res) => setCandidatures(res.data))
      .catch(() => setCandidatures([]))
  }

  useEffect(loadData, [])

  const candidatureByProjectId = new Map((candidatures ?? []).map((c) => [c.projectId, c]))

  const openApplyDialog = (project) => {
    setApplyTarget(project)
    setCoverLetter('')
    setSubmitError(null)
  }

  const closeApplyDialog = () => {
    if (submitting) return
    setApplyTarget(null)
  }

  const submitApplication = () => {
    setSubmitting(true)
    setSubmitError(null)
    api
      .post(`/projects/${applyTarget.id}/candidatures`, { coverLetter })
      .then(() => {
        setSuccessMessage('Candidatura inviata con successo.')
        setApplyTarget(null)
        loadData()
      })
      .catch((err) => {
        setSubmitError(err.response?.data?.detail || err.message)
      })
      .finally(() => setSubmitting(false))
  }

  return (
    <>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 600 }}>
        Progetti disponibili
      </Typography>

      <DataSection
        loading={projects === null && !projectsError}
        error={projectsError}
        isEmpty={projects?.length === 0}
        emptyLabel="Nessun progetto aperto al momento."
      >
        <List disablePadding>
          {projects?.map((project) => {
            const existingCandidature = candidatureByProjectId.get(project.id)
            return (
              <ListItem key={project.id} divider sx={{ alignItems: 'flex-start', py: 2 }}>
                <ListItemText
                  primary={project.title}
                  secondary={
                    <>
                      <Typography variant="body2" color="text.secondary" component="span" display="block">
                        {project.description}
                      </Typography>
                      <Typography variant="body2" color="text.secondary" component="span" display="block">
                        Budget: €{project.budget} — {project.durationDays} giorni
                      </Typography>
                      <Stack direction="row" spacing={1} sx={{ mt: 1, flexWrap: 'wrap', gap: 0.5 }}>
                        {project.requirements?.map((req) => (
                          <Chip key={req.id} label={req.skillName} size="small" variant="outlined" />
                        ))}
                      </Stack>
                    </>
                  }
                />
                <Box sx={{ pl: 2 }}>
                  {existingCandidature ? (
                    <Chip
                      label={candidatureStatusInfo(existingCandidature.status).label}
                      color={candidatureStatusInfo(existingCandidature.status).color}
                      size="small"
                    />
                  ) : (
                    <Button variant="contained" size="small" onClick={() => openApplyDialog(project)}>
                      Candidati
                    </Button>
                  )}
                </Box>
              </ListItem>
            )
          })}
        </List>
      </DataSection>

      <Dialog open={Boolean(applyTarget)} onClose={closeApplyDialog} fullWidth maxWidth="sm">
        <DialogTitle>Candidati a "{applyTarget?.title}"</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            multiline
            minRows={4}
            fullWidth
            label="Lettera di presentazione (opzionale)"
            value={coverLetter}
            onChange={(e) => setCoverLetter(e.target.value)}
            inputProps={{ maxLength: 2000 }}
            sx={{ mt: 1 }}
            error={Boolean(submitError)}
            helperText={submitError}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={closeApplyDialog} disabled={submitting}>
            Annulla
          </Button>
          <Button onClick={submitApplication} variant="contained" disabled={submitting}>
            Invia candidatura
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={Boolean(successMessage)}
        autoHideDuration={4000}
        onClose={() => setSuccessMessage(null)}
        message={successMessage}
      />
    </>
  )
}
