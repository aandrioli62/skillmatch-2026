import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  List,
  ListItem,
  ListItemText,
  MenuItem,
  Snackbar,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import { useEffect, useState } from 'react'
import DataSection from '../../components/DataSection'
import api from '../../services/api'
import { candidatureStatusInfo, formatDate, projectStatusInfo, shortId } from '../../utils/format'

const REPUTATION_LEVELS = ['JUNIOR', 'AFFIDABILE', 'TOP_PERFORMER']

const EMPTY_FORM = {
  title: '',
  description: '',
  durationDays: '',
  budget: '',
  requirements: [{ skillName: '', minReputationLevel: '' }],
}

export default function CompanyProjects() {
  const [projects, setProjects] = useState(null)
  const [projectsError, setProjectsError] = useState(null)

  const [candidatesTarget, setCandidatesTarget] = useState(null)
  const [candidates, setCandidates] = useState(null)
  const [candidatesError, setCandidatesError] = useState(null)
  const [actionError, setActionError] = useState(null)

  const [publishTarget, setPublishTarget] = useState(null)
  const [completeTarget, setCompleteTarget] = useState(null)

  const [createOpen, setCreateOpen] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [createError, setCreateError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const [message, setMessage] = useState(null)

  const loadProjects = () => {
    api
      .get('/projects/mine')
      .then((res) => setProjects(res.data))
      .catch((err) => setProjectsError(err.message))
  }

  useEffect(loadProjects, [])

  const publishProject = () => {
    api
      .put(`/projects/${publishTarget.id}/publish`)
      .then(() => {
        setMessage('Progetto pubblicato.')
        setPublishTarget(null)
        loadProjects()
      })
      .catch((err) => setMessage(err.response?.data?.detail || err.message))
  }

  const completeProject = () => {
    api
      .put(`/projects/${completeTarget.id}/complete`)
      .then(() => {
        setMessage('Progetto completato.')
        setCompleteTarget(null)
        loadProjects()
      })
      .catch((err) => setMessage(err.response?.data?.detail || err.message))
  }

  const openCandidates = (project) => {
    setCandidatesTarget(project)
    setCandidates(null)
    setCandidatesError(null)
    setActionError(null)
    api
      .get(`/projects/${project.id}/candidatures`)
      .then((res) => setCandidates(res.data))
      .catch((err) => setCandidatesError(err.message))
  }

  const acceptCandidate = (candidatureId) => {
    setActionError(null)
    api
      .put(`/projects/${candidatesTarget.id}/candidatures/${candidatureId}/accept`)
      .then(() => {
        setMessage('Candidatura accettata.')
        setCandidatesTarget(null)
        loadProjects()
      })
      .catch((err) => setActionError(err.response?.data?.detail || err.message))
  }

  const openCreateDialog = () => {
    setForm(EMPTY_FORM)
    setCreateError(null)
    setCreateOpen(true)
  }

  const updateRequirement = (index, field, value) => {
    setForm((prev) => ({
      ...prev,
      requirements: prev.requirements.map((req, i) => (i === index ? { ...req, [field]: value } : req)),
    }))
  }

  const addRequirement = () => {
    setForm((prev) => ({
      ...prev,
      requirements: [...prev.requirements, { skillName: '', minReputationLevel: '' }],
    }))
  }

  const removeRequirement = (index) => {
    setForm((prev) => ({ ...prev, requirements: prev.requirements.filter((_, i) => i !== index) }))
  }

  const submitCreate = () => {
    setSubmitting(true)
    setCreateError(null)
    api
      .post('/projects', {
        title: form.title,
        description: form.description,
        durationDays: form.durationDays ? Number(form.durationDays) : null,
        budget: Number(form.budget),
        requirements: form.requirements
          .filter((r) => r.skillName.trim())
          .map((r) => ({
            skillName: r.skillName,
            minReputationLevel: r.minReputationLevel || null,
          })),
      })
      .then(() => {
        setMessage('Progetto creato in bozza.')
        setCreateOpen(false)
        loadProjects()
      })
      .catch((err) => setCreateError(err.response?.data?.detail || err.message))
      .finally(() => setSubmitting(false))
  }

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h5" sx={{ fontWeight: 600 }}>
          I miei progetti
        </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreateDialog}>
          Nuovo progetto
        </Button>
      </Box>

      <DataSection
        loading={projects === null && !projectsError}
        error={projectsError}
        isEmpty={projects?.length === 0}
        emptyLabel="Non hai ancora pubblicato nessun progetto."
      >
        <List disablePadding>
          {projects?.map((project) => {
            const statusInfo = projectStatusInfo(project.status)
            return (
              <ListItem key={project.id} divider sx={{ alignItems: 'flex-start', py: 2 }}>
                <ListItemText
                  primary={
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      {project.title}
                      <Chip label={statusInfo.label} color={statusInfo.color} size="small" />
                    </Box>
                  }
                  secondary={`Budget: €${project.budget} — ${project.durationDays} giorni`}
                />
                <Stack direction="row" spacing={1} sx={{ pl: 2 }}>
                  {project.status === 'DRAFT' && (
                    <Button size="small" variant="contained" onClick={() => setPublishTarget(project)}>
                      Pubblica
                    </Button>
                  )}
                  {['OPEN', 'ASSIGNED', 'IN_PROGRESS'].includes(project.status) && (
                    <Button size="small" variant="outlined" onClick={() => openCandidates(project)}>
                      Vedi candidati
                    </Button>
                  )}
                  {['ASSIGNED', 'IN_PROGRESS'].includes(project.status) && (
                    <Button size="small" variant="outlined" color="success" onClick={() => setCompleteTarget(project)}>
                      Completa
                    </Button>
                  )}
                </Stack>
              </ListItem>
            )
          })}
        </List>
      </DataSection>

      <Dialog open={Boolean(candidatesTarget)} onClose={() => setCandidatesTarget(null)} fullWidth maxWidth="sm">
        <DialogTitle>Candidati per "{candidatesTarget?.title}"</DialogTitle>
        <DialogContent>
          <DataSection
            loading={candidates === null && !candidatesError}
            error={candidatesError}
            isEmpty={candidates?.length === 0}
            emptyLabel="Nessuna candidatura ricevuta per questo progetto."
          >
            <List disablePadding>
              {candidates?.map((candidate) => {
                const statusInfo = candidatureStatusInfo(candidate.status)
                return (
                  <ListItem key={candidate.id} divider sx={{ alignItems: 'flex-start' }}>
                    <ListItemText
                      primary={
                        <Tooltip title={candidate.professionalId}>
                          <span>Professionista #{shortId(candidate.professionalId)}</span>
                        </Tooltip>
                      }
                      secondary={
                        <>
                          Candidato il {formatDate(candidate.appliedAt)}
                          {candidate.coverLetter ? ` — "${candidate.coverLetter}"` : ''}
                        </>
                      }
                    />
                    {candidate.status === 'PENDING' ? (
                      <Button size="small" variant="contained" onClick={() => acceptCandidate(candidate.id)}>
                        Accetta
                      </Button>
                    ) : (
                      <Chip label={statusInfo.label} color={statusInfo.color} size="small" />
                    )}
                  </ListItem>
                )
              })}
            </List>
          </DataSection>
          {actionError && (
            <Typography variant="body2" color="error" sx={{ mt: 2 }}>
              {actionError}
            </Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCandidatesTarget(null)}>Chiudi</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={Boolean(publishTarget)} onClose={() => setPublishTarget(null)}>
        <DialogTitle>Pubblicare il progetto?</DialogTitle>
        <DialogContent>
          <Typography variant="body2">
            "{publishTarget?.title}" diventerà visibile a tutti i professionisti validati, che potranno candidarsi.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPublishTarget(null)}>Annulla</Button>
          <Button variant="contained" onClick={publishProject}>
            Pubblica progetto
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={Boolean(completeTarget)} onClose={() => setCompleteTarget(null)}>
        <DialogTitle>Completare il progetto?</DialogTitle>
        <DialogContent>
          <Typography variant="body2">
            Stai per segnare "{completeTarget?.title}" come completato. L'operazione abilita il pagamento e non può
            essere annullata.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCompleteTarget(null)}>Annulla</Button>
          <Button variant="contained" color="success" onClick={completeProject}>
            Completa progetto
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={createOpen} onClose={() => !submitting && setCreateOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Nuovo progetto</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Titolo"
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              fullWidth
              required
            />
            <TextField
              label="Descrizione"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              fullWidth
              multiline
              minRows={3}
            />
            <Stack direction="row" spacing={2}>
              <TextField
                label="Durata (giorni)"
                type="number"
                value={form.durationDays}
                onChange={(e) => setForm({ ...form, durationDays: e.target.value })}
                fullWidth
              />
              <TextField
                label="Budget (€)"
                type="number"
                value={form.budget}
                onChange={(e) => setForm({ ...form, budget: e.target.value })}
                fullWidth
                required
              />
            </Stack>

            <Typography variant="subtitle2">Competenze richieste</Typography>
            {form.requirements.map((req, index) => (
              <Stack direction="row" spacing={1} key={index} alignItems="center">
                <TextField
                  label="Skill"
                  value={req.skillName}
                  onChange={(e) => updateRequirement(index, 'skillName', e.target.value)}
                  fullWidth
                />
                <TextField
                  select
                  label="Livello minimo"
                  value={req.minReputationLevel}
                  onChange={(e) => updateRequirement(index, 'minReputationLevel', e.target.value)}
                  sx={{ minWidth: 160 }}
                >
                  <MenuItem value="">Nessuno</MenuItem>
                  {REPUTATION_LEVELS.map((level) => (
                    <MenuItem key={level} value={level}>
                      {level}
                    </MenuItem>
                  ))}
                </TextField>
                <IconButton
                  onClick={() => removeRequirement(index)}
                  disabled={form.requirements.length === 1}
                  size="small"
                >
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </Stack>
            ))}
            <Button startIcon={<AddIcon />} onClick={addRequirement} sx={{ alignSelf: 'flex-start' }}>
              Aggiungi competenza
            </Button>

            {createError && (
              <Typography variant="body2" color="error">
                {createError}
              </Typography>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)} disabled={submitting}>
            Annulla
          </Button>
          <Button
            variant="contained"
            onClick={submitCreate}
            disabled={submitting || !form.title || !form.budget}
          >
            Crea progetto
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={Boolean(message)} autoHideDuration={4000} onClose={() => setMessage(null)} message={message} />
    </>
  )
}
