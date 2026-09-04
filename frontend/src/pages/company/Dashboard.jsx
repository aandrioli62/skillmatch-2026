import { Box, Chip, List, ListItem, ListItemText, Paper, Typography } from '@mui/material'
import { useEffect, useState } from 'react'
import DataSection from '../../components/DataSection'
import { useAuth } from '../../hooks/useAuth'
import api from '../../services/api'
import { contractStatusInfo, projectStatusInfo } from '../../utils/format'

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

export default function CompanyDashboard() {
  const { username } = useAuth()

  const [projects, setProjects] = useState(null)
  const [projectsError, setProjectsError] = useState(null)

  const [contracts, setContracts] = useState(null)
  const [contractsError, setContractsError] = useState(null)

  useEffect(() => {
    api
      .get('/projects/mine')
      .then((res) => setProjects(res.data))
      .catch((err) => setProjectsError(err.message))

    api
      .get('/contracts/company/mine')
      .then((res) => setContracts(res.data))
      .catch((err) => setContractsError(err.message))
  }, [])

  const activeContracts = contracts?.filter((c) => c.status === 'ACTIVE').length

  return (
    <>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 600 }}>
        Bentornato, {username}
      </Typography>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' }, gap: 2, mb: 3 }}>
        <StatCard label="I miei progetti" value={projects ? projects.length : '—'} />
        <StatCard label="Contratti attivi" value={contracts ? activeContracts : '—'} />
        <StatCard label="Contratti totali" value={contracts ? contracts.length : '—'} />
      </Box>

      <Box sx={{ display: 'grid', gap: 3 }}>
        <DataSection
          title="I miei progetti"
          loading={projects === null && !projectsError}
          error={projectsError}
          isEmpty={projects?.length === 0}
          emptyLabel="Non hai ancora pubblicato nessun progetto."
        >
          <List disablePadding>
            {projects?.map((project) => {
              const statusInfo = projectStatusInfo(project.status)
              return (
                <ListItem key={project.id} divider>
                  <ListItemText
                    primary={project.title}
                    secondary={`Budget: €${project.budget} — ${project.durationDays} giorni`}
                  />
                  <Chip label={statusInfo.label} color={statusInfo.color} size="small" />
                </ListItem>
              )
            })}
          </List>
        </DataSection>

        <DataSection
          title="I miei contratti"
          loading={contracts === null && !contractsError}
          error={contractsError}
          isEmpty={contracts?.length === 0}
          emptyLabel="Nessun contratto ancora generato."
        >
          <List disablePadding>
            {contracts?.map((contract) => {
              const statusInfo = contractStatusInfo(contract.status)
              return (
                <ListItem key={contract.id} divider>
                  <ListItemText primary={`€${contract.amount}`} secondary={`Commissione: ${contract.commissionRate}%`} />
                  <Chip label={statusInfo.label} color={statusInfo.color} size="small" />
                </ListItem>
              )
            })}
          </List>
        </DataSection>
      </Box>
    </>
  )
}
