import { Chip, List, ListItem, ListItemText, Typography } from '@mui/material'
import { useEffect, useState } from 'react'
import DataSection from '../../components/DataSection'
import api from '../../services/api'
import { candidatureStatusInfo, formatDate } from '../../utils/format'

export default function ProfessionalApplications() {
  const [candidatures, setCandidatures] = useState(null)
  const [error, setError] = useState(null)

  const [projectTitles, setProjectTitles] = useState({})

  useEffect(() => {
    api
      .get('/projects/candidatures/mine')
      .then((res) => {
        setCandidatures(res.data)
        res.data.forEach((c) => {
          api
            .get(`/projects/${c.projectId}`)
            .then((projectRes) => {
              setProjectTitles((prev) => ({ ...prev, [c.projectId]: projectRes.data.title }))
            })
            .catch(() => {})
        })
      })
      .catch((err) => setError(err.message))
  }, [])

  return (
    <>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 600 }}>
        Le mie candidature
      </Typography>

      <DataSection
        loading={candidatures === null && !error}
        error={error}
        isEmpty={candidatures?.length === 0}
        emptyLabel="Non ti sei ancora candidato a nessun progetto."
      >
        <List disablePadding>
          {candidatures?.map((candidature) => {
            const statusInfo = candidatureStatusInfo(candidature.status)
            return (
              <ListItem key={candidature.id} divider sx={{ alignItems: 'flex-start' }}>
                <ListItemText
                  primary={projectTitles[candidature.projectId] ?? 'Caricamento…'}
                  secondary={
                    <>
                      Candidato il {formatDate(candidature.appliedAt)}
                      {candidature.coverLetter ? ` — "${candidature.coverLetter}"` : ''}
                    </>
                  }
                />
                <Chip label={statusInfo.label} color={statusInfo.color} size="small" />
              </ListItem>
            )
          })}
        </List>
      </DataSection>
    </>
  )
}
