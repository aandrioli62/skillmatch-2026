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
  Pagination,
  Snackbar,
  Stack,
  Typography,
} from '@mui/material'
import { useEffect, useState } from 'react'
import DataSection from '../../components/DataSection'
import api from '../../services/api'

const STATUS_COLOR = {
  PENDING: 'warning',
  VALIDATED: 'success',
  SUSPENDED: 'error',
}

export default function AdminUsers() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)

  const [profileNames, setProfileNames] = useState({})

  const [suspendTarget, setSuspendTarget] = useState(null)
  const [message, setMessage] = useState(null)

  const loadUsers = () => {
    api
      .get('/admin/users', { params: { page, size: 10 } })
      .then((res) => {
        setData(res.data)
        res.data.content
          .filter((u) => u.role === 'PROFESSIONAL')
          .forEach((u) => {
            api
              .get(`/admin/users/${u.id}/professional-profile`)
              .then((profileRes) => {
                const { firstName, lastName } = profileRes.data
                setProfileNames((prev) => ({ ...prev, [u.id]: [firstName, lastName].filter(Boolean).join(' ') }))
              })
              .catch(() => {})
          })
      })
      .catch((err) => setError(err.message))
  }

  useEffect(loadUsers, [page])

  const validateUser = (user) => {
    api
      .post(`/admin/users/${user.id}/validate`)
      .then(() => {
        setMessage('Professionista validato.')
        loadUsers()
      })
      .catch((err) => setMessage(err.response?.data?.detail || err.message))
  }

  const suspendUser = () => {
    api
      .post(`/admin/users/${suspendTarget.id}/suspend`)
      .then(() => {
        setMessage('Utente sospeso.')
        setSuspendTarget(null)
        loadUsers()
      })
      .catch((err) => setMessage(err.response?.data?.detail || err.message))
  }

  return (
    <>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 600 }}>
        Validazione utenti
      </Typography>

      <DataSection
        loading={data === null && !error}
        error={error}
        isEmpty={data?.content.length === 0}
        emptyLabel="Nessun utente registrato."
      >
        <List disablePadding>
          {data?.content.map((user) => (
            <ListItem key={user.id} divider>
              <ListItemText
                primary={
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    {profileNames[user.id] || user.email}
                    <Chip label={user.role} size="small" variant="outlined" />
                  </Box>
                }
                secondary={user.email}
              />
              <Stack direction="row" spacing={1} alignItems="center">
                {user.role === 'PROFESSIONAL' && user.status !== 'VALIDATED' && (
                  <Button size="small" variant="contained" onClick={() => validateUser(user)}>
                    Valida
                  </Button>
                )}
                {user.status !== 'SUSPENDED' && (
                  <Button size="small" variant="outlined" color="error" onClick={() => setSuspendTarget(user)}>
                    Sospendi
                  </Button>
                )}
                <Chip label={user.status} color={STATUS_COLOR[user.status] ?? 'default'} size="small" />
              </Stack>
            </ListItem>
          ))}
        </List>
      </DataSection>

      {data && data.totalPages > 1 && (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
          <Pagination
            count={data.totalPages}
            page={page + 1}
            onChange={(_, value) => setPage(value - 1)}
          />
        </Box>
      )}

      <Dialog open={Boolean(suspendTarget)} onClose={() => setSuspendTarget(null)}>
        <DialogTitle>Sospendere l'utente?</DialogTitle>
        <DialogContent>
          <Typography variant="body2">
            "{suspendTarget?.email}" perderà l'accesso alle funzionalità della piattaforma.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSuspendTarget(null)}>Annulla</Button>
          <Button variant="contained" color="error" onClick={suspendUser}>
            Sospendi
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={Boolean(message)} autoHideDuration={4000} onClose={() => setMessage(null)} message={message} />
    </>
  )
}
