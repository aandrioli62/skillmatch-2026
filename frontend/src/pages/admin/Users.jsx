import FlagIcon from '@mui/icons-material/Flag'
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
import UserDetailDialog from '../../components/UserDetailDialog'
import api from '../../services/api'

const STATUS_COLOR = {
  PENDING: 'warning',
  VALIDATED: 'success',
  SUSPENDED: 'error',
  DEACTIVATED: 'default',
}

export default function AdminUsers() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)

  const [profileNames, setProfileNames] = useState({})

  const [suspendTarget, setSuspendTarget] = useState(null)
  const [validateTarget, setValidateTarget] = useState(null)
  const [deactivateTarget, setDeactivateTarget] = useState(null)
  const [detailTarget, setDetailTarget] = useState(null)
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

  const confirmValidate = () => {
    const wasSuspended = validateTarget.status === 'SUSPENDED'
    api
      .post(`/admin/users/${validateTarget.id}/validate`)
      .then(() => {
        setMessage(wasSuspended ? 'Account riattivato.' : 'Account validato.')
        setValidateTarget(null)
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

  const deactivateUser = () => {
    api
      .post(`/admin/users/${deactivateTarget.id}/deactivate`)
      .then(() => {
        setMessage('Account eliminato.')
        setDeactivateTarget(null)
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
            <ListItem
              key={user.id}
              divider
              onClick={() => setDetailTarget(user)}
              sx={{ cursor: 'pointer' }}
            >
              <ListItemText
                primary={
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    {profileNames[user.id] || user.email}
                    <Chip label={user.role} size="small" variant="outlined" />
                    <Chip label={user.status} color={STATUS_COLOR[user.status] ?? 'default'} size="small" />
                    {user.openReportCount > 0 && (
                      <Chip
                        icon={<FlagIcon />}
                        label={`${user.openReportCount} segnalazion${user.openReportCount === 1 ? 'e' : 'i'}`}
                        color="error"
                        size="small"
                      />
                    )}
                  </Box>
                }
                secondary={user.email}
              />
              <Stack direction="row" spacing={1} alignItems="center">
                {user.status !== 'VALIDATED' && user.status !== 'DEACTIVATED' && (
                  <Button
                    size="small"
                    variant="contained"
                    onClick={(e) => {
                      e.stopPropagation()
                      setValidateTarget(user)
                    }}
                  >
                    {user.status === 'SUSPENDED' ? 'Riattiva' : 'Valida'}
                  </Button>
                )}
                {user.status !== 'SUSPENDED' && user.status !== 'DEACTIVATED' && (
                  <Button
                    size="small"
                    variant="outlined"
                    color="error"
                    onClick={(e) => {
                      e.stopPropagation()
                      setSuspendTarget(user)
                    }}
                  >
                    Sospendi
                  </Button>
                )}
                {user.status !== 'DEACTIVATED' && (
                  <Button
                    size="small"
                    variant="contained"
                    color="error"
                    onClick={(e) => {
                      e.stopPropagation()
                      setDeactivateTarget(user)
                    }}
                  >
                    Elimina
                  </Button>
                )}
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

      <Dialog open={Boolean(validateTarget)} onClose={() => setValidateTarget(null)}>
        <DialogTitle>
          {validateTarget?.status === 'SUSPENDED' ? 'Riattivare questo account?' : 'Validare questo account?'}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2">
            {validateTarget?.role === 'COMPANY'
              ? `"${validateTarget?.email}" potrà pubblicare progetti e gestire candidature.`
              : `"${validateTarget?.email}" potrà candidarsi ai progetti pubblicati.`}
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setValidateTarget(null)}>Annulla</Button>
          <Button variant="contained" onClick={confirmValidate}>
            {validateTarget?.status === 'SUSPENDED' ? 'Riattiva' : 'Valida'}
          </Button>
        </DialogActions>
      </Dialog>

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

      <Dialog open={Boolean(deactivateTarget)} onClose={() => setDeactivateTarget(null)}>
        <DialogTitle>Eliminare questo account?</DialogTitle>
        <DialogContent>
          <Typography variant="body2">
            "{deactivateTarget?.email}" non potrà più accedere alla piattaforma — l'operazione non è reversibile da
            qui. Contratti, pagamenti e feedback già collegati a questo account restano consultabili.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeactivateTarget(null)}>Annulla</Button>
          <Button variant="contained" color="error" onClick={deactivateUser}>
            Elimina
          </Button>
        </DialogActions>
      </Dialog>

      <UserDetailDialog
        key={detailTarget?.id ?? 'none'}
        user={detailTarget}
        onClose={() => {
          setDetailTarget(null)
          // Archiving a report inside the dialog doesn't update this list's
          // own copy of openReportCount — refresh on close so the badge
          // reflects it without waiting for a manual page reload.
          loadUsers()
        }}
        onValidate={(user) => {
          setDetailTarget(null)
          setValidateTarget(user)
        }}
        onSuspend={(user) => {
          setDetailTarget(null)
          setSuspendTarget(user)
        }}
        onDeactivate={(user) => {
          setDetailTarget(null)
          setDeactivateTarget(user)
        }}
      />

      <Snackbar open={Boolean(message)} autoHideDuration={4000} onClose={() => setMessage(null)} message={message} />
    </>
  )
}
