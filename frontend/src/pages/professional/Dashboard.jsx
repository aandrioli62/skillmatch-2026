import { Box, Chip, List, ListItem, ListItemText, Paper, Rating, Typography } from '@mui/material'
import { useEffect, useState } from 'react'
import DataSection from '../../components/DataSection'
import { useAuth } from '../../context/AuthContext'
import api from '../../services/api'
import { candidatureStatusInfo, formatDate } from '../../utils/format'

function StatCard({ label, value }) {
  return (
    <Paper variant="outlined" sx={{ p: 3, borderRadius: 3 }}>
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="h4" sx={{ mt: 1, fontWeight: 700 }}>
        {value}
      </Typography>
    </Paper>
  )
}

export default function ProfessionalDashboard() {
  const { username } = useAuth()

  const [openProjects, setOpenProjects] = useState(null)
  const [openProjectsError, setOpenProjectsError] = useState(null)

  const [candidatures, setCandidatures] = useState(null)
  const [candidaturesError, setCandidaturesError] = useState(null)

  const [feedback, setFeedback] = useState(null)
  const [feedbackError, setFeedbackError] = useState(null)

  useEffect(() => {
    api
      .get('/projects/open')
      .then((res) => setOpenProjects(res.data))
      .catch((err) => setOpenProjectsError(err.message))

    api
      .get('/projects/candidatures/mine')
      .then((res) => setCandidatures(res.data))
      .catch((err) => setCandidaturesError(err.message))

    api
      .get('/feedbacks/received/mine')
      .then((res) => setFeedback(res.data))
      .catch((err) => setFeedbackError(err.message))
  }, [])

  const projectTitleById = new Map((openProjects ?? []).map((p) => [p.id, p.title]))

  return (
    <>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 600 }}>
        Bentornato, {username}
      </Typography>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' }, gap: 2, mb: 3 }}>
        <StatCard label="Progetti aperti" value={openProjects ? openProjects.length : '—'} />
        <StatCard label="Le mie candidature" value={candidatures ? candidatures.length : '—'} />
        <StatCard label="Feedback ricevuti" value={feedback ? feedback.length : '—'} />
      </Box>

      <Box sx={{ display: 'grid', gap: 3 }}>
        <DataSection
          title="Progetti aperti"
          loading={openProjects === null && !openProjectsError}
          error={openProjectsError}
          isEmpty={openProjects?.length === 0}
          emptyLabel="Nessun progetto aperto al momento."
        >
          <List disablePadding>
            {openProjects?.map((project) => (
              <ListItem key={project.id} divider>
                <ListItemText
                  primary={project.title}
                  secondary={`Budget: €${project.budget} — ${project.durationDays} giorni`}
                />
              </ListItem>
            ))}
          </List>
        </DataSection>

        <DataSection
          title="Le mie candidature"
          loading={candidatures === null && !candidaturesError}
          error={candidaturesError}
          isEmpty={candidatures?.length === 0}
          emptyLabel="Non ti sei ancora candidato a nessun progetto."
        >
          <List disablePadding>
            {candidatures?.map((candidature) => {
              const statusInfo = candidatureStatusInfo(candidature.status)
              return (
                <ListItem key={candidature.id} divider>
                  <ListItemText
                    primary={projectTitleById.get(candidature.projectId) ?? 'Progetto'}
                    secondary={`Candidato il ${formatDate(candidature.appliedAt)}`}
                  />
                  <Chip label={statusInfo.label} color={statusInfo.color} size="small" />
                </ListItem>
              )
            })}
          </List>
        </DataSection>

        <DataSection
          title="Feedback ricevuti"
          loading={feedback === null && !feedbackError}
          error={feedbackError}
          isEmpty={feedback?.length === 0}
          emptyLabel="Non hai ancora ricevuto feedback."
        >
          <List disablePadding>
            {feedback?.map((item) => (
              <ListItem key={item.id} divider sx={{ alignItems: 'flex-start' }}>
                <ListItemText
                  primary={<Rating value={item.rating} readOnly size="small" />}
                  secondary={item.comment || 'Nessun commento'}
                />
              </ListItem>
            ))}
          </List>
        </DataSection>
      </Box>
    </>
  )
}
