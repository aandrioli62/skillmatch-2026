import { Box, Button, Chip, List, ListItem, ListItemText, Paper, Rating, Stack, Typography } from '@mui/material'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import DataSection from '../../components/DataSection'
import { useAuth } from '../../hooks/useAuth'
import { useCurrentUser } from '../../hooks/useCurrentUser'
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
  const { user } = useCurrentUser()

  const [openProjects, setOpenProjects] = useState(null)
  const [openProjectsError, setOpenProjectsError] = useState(null)

  const [candidatures, setCandidatures] = useState(null)
  const [candidaturesError, setCandidaturesError] = useState(null)

  const [feedback, setFeedback] = useState(null)
  const [feedbackError, setFeedbackError] = useState(null)

  const [skills, setSkills] = useState(null)

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

  useEffect(() => {
    if (!user) return
    api
      .get(`/users/${user.id}/professional-profile`)
      .then((res) => setSkills(res.data.skills ?? []))
      .catch(() => setSkills([]))
  }, [user])

  const projectTitleById = new Map((openProjects ?? []).map((p) => [p.id, p.title]))

  const skillNames = new Set((skills ?? []).map((s) => s.skillName.toLowerCase()))
  const nearbyProjects = (openProjects ?? [])
    .map((project) => ({
      project,
      matchedSkillIds: new Set(
        (project.requirements ?? [])
          .filter((req) => skillNames.has(req.skillName.toLowerCase()))
          .map((req) => req.id),
      ),
    }))
    .filter((entry) => entry.matchedSkillIds.size > 0)
    .sort((a, b) => b.matchedSkillIds.size - a.matchedSkillIds.size)
    .slice(0, 5)

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
          title="Progetti vicini alle tue competenze"
          loading={(openProjects === null || skills === null) && !openProjectsError}
          error={openProjectsError}
          isEmpty={nearbyProjects.length === 0}
          emptyLabel="Nessun progetto aperto corrisponde ancora alle tue competenze."
        >
          <List disablePadding>
            {nearbyProjects.map(({ project, matchedSkillIds }) => (
              <ListItem key={project.id} divider sx={{ alignItems: 'flex-start', py: 2 }}>
                <ListItemText
                  primary={project.title}
                  secondary={
                    <>
                      <Typography variant="body2" color="text.secondary" component="span" display="block">
                        Budget: €{project.budget} — {project.durationDays} giorni
                      </Typography>
                      <Stack direction="row" spacing={1} sx={{ mt: 1, flexWrap: 'wrap', gap: 0.5 }}>
                        {project.requirements?.map((req) => (
                          <Chip
                            key={req.id}
                            label={req.skillName}
                            size="small"
                            color={matchedSkillIds.has(req.id) ? 'success' : 'default'}
                            variant={matchedSkillIds.has(req.id) ? 'filled' : 'outlined'}
                          />
                        ))}
                      </Stack>
                    </>
                  }
                />
                <Button component={Link} to="/professional/projects" size="small" sx={{ ml: 2, flexShrink: 0 }}>
                  Vai al progetto
                </Button>
              </ListItem>
            ))}
          </List>
        </DataSection>

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
