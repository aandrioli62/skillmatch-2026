import {
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Link as MuiLink,
  Stack,
  Tab,
  Tabs,
  Typography,
} from '@mui/material'
import { useEffect, useState } from 'react'
import api from '../services/api'
import { formatDate } from '../utils/format'

const STATUS_COLOR = {
  PENDING: 'warning',
  VALIDATED: 'success',
  SUSPENDED: 'error',
}

// Detail card shown when an admin clicks a row in "Validazione utenti" — the
// full picture the exam spec requires before validating a professional (bio,
// skills with certification links, portfolio, payout account), plus a second
// tab with every report filed against this user (professional or company),
// so the admin can judge whether they add up to a suspension.
//
// Also reused, read-only, when a company reviews a candidature: same two tabs,
// but backed by the non-admin endpoints (a company isn't allowed the admin-only
// ones), and with the validate/suspend/deactivate/archive actions hidden —
// a company can look, not act.
export default function UserDetailDialog({ user, onClose, onValidate, onSuspend, onDeactivate, readOnly = false }) {
  const [tab, setTab] = useState(0)
  const [profile, setProfile] = useState(null)
  const [reports, setReports] = useState(null)

  const basePath = readOnly ? '/users' : '/admin/users'

  useEffect(() => {
    if (!user) return undefined
    let cancelled = false

    api
      .get(`${basePath}/${user.id}/reports`)
      .then((res) => {
        if (!cancelled) setReports(res.data)
      })
      .catch(() => {
        if (!cancelled) setReports([])
      })

    if (user.role !== 'PROFESSIONAL' && user.role !== 'COMPANY') {
      return () => {
        cancelled = true
      }
    }

    const profileEndpoint = user.role === 'PROFESSIONAL' ? 'professional-profile' : 'company-profile'
    api
      .get(`${basePath}/${user.id}/${profileEndpoint}`)
      .then((res) => {
        if (!cancelled) setProfile(res.data)
      })
      .catch(() => {
        if (!cancelled) setProfile(null)
      })

    return () => {
      cancelled = true
    }
  }, [user, basePath])

  const closeReport = (reportId) => {
    api.post(`/admin/reports/${reportId}/close`).then(() => {
      setReports((prev) => prev.map((r) => (r.id === reportId ? { ...r, status: 'CLOSED' } : r)))
    })
  }

  const hasProfile = user && (user.role === 'PROFESSIONAL' || user.role === 'COMPANY')
  const loading = Boolean(hasProfile) && profile === null

  if (!user) return null

  const openReportCount = reports?.filter((r) => r.status === 'OPEN').length ?? 0

  return (
    <Dialog open={Boolean(user)} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {profile
            ? user.role === 'COMPANY'
              ? profile.companyName || user.email
              : `${profile.firstName ?? ''} ${profile.lastName ?? ''}`.trim() || user.email
            : user.email}
          <Chip label={user.role} size="small" variant="outlined" />
          <Chip label={user.status} color={STATUS_COLOR[user.status] ?? 'default'} size="small" />
        </Box>
      </DialogTitle>

      <Tabs value={tab} onChange={(e, value) => setTab(value)} sx={{ px: 3, borderBottom: 1, borderColor: 'divider' }}>
        <Tab label="Profilo" />
        <Tab label={`Segnalazioni${openReportCount > 0 ? ` (${openReportCount})` : ''}`} />
      </Tabs>

      <DialogContent dividers>
        {tab === 0 && (
          <>
            {!hasProfile && (
              <Typography variant="body2" color="text.secondary">
                {user.email}
              </Typography>
            )}

            {hasProfile && loading && (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
                <CircularProgress size={28} />
              </Box>
            )}

            {user.role === 'PROFESSIONAL' && !loading && profile && (
              <Stack spacing={2}>
                <Typography variant="body2" color="text.secondary">
                  {user.email}
                </Typography>

                {profile.bio && (
                  <Box>
                    <Typography variant="subtitle2">Bio</Typography>
                    <Typography variant="body2">{profile.bio}</Typography>
                  </Box>
                )}

                <Box>
                  <Typography variant="subtitle2">Reputazione</Typography>
                  <Typography variant="body2">
                    {profile.reputationLevel} — media {profile.avgRating ?? '—'} su {profile.totalReviews ?? 0} recensioni
                  </Typography>
                </Box>

                <Box>
                  <Typography variant="subtitle2">Conto per la ricezione dei compensi</Typography>
                  <Typography variant="body2">{profile.paymentAccount || 'Non ancora configurato'}</Typography>
                </Box>

                <Box>
                  <Typography variant="subtitle2">Competenze</Typography>
                  {profile.skills?.length ? (
                    <Stack direction="row" spacing={1} useFlexGap sx={{ mt: 0.5, flexWrap: 'wrap' }}>
                      {profile.skills.map((s) => (
                        <Chip
                          key={s.skillId}
                          label={s.skillName}
                          size="small"
                          component={s.certificationUrl ? MuiLink : 'div'}
                          href={s.certificationUrl || undefined}
                          target={s.certificationUrl ? '_blank' : undefined}
                          clickable={Boolean(s.certificationUrl)}
                          color={s.certificationUrl ? 'primary' : 'default'}
                        />
                      ))}
                    </Stack>
                  ) : (
                    <Typography variant="body2" color="text.secondary">
                      Nessuna competenza inserita.
                    </Typography>
                  )}
                </Box>

                <Box>
                  <Typography variant="subtitle2">Portfolio</Typography>
                  {profile.portfolioItems?.length ? (
                    <Stack spacing={0.5} sx={{ mt: 0.5 }}>
                      {profile.portfolioItems.map((item) => (
                        <Box key={item.id}>
                          <Typography variant="body2" sx={{ fontWeight: 600 }}>
                            {item.url ? (
                              <MuiLink href={item.url} target="_blank" rel="noopener">
                                {item.title}
                              </MuiLink>
                            ) : (
                              item.title
                            )}
                          </Typography>
                          {item.description && (
                            <Typography variant="body2" color="text.secondary">
                              {item.description}
                            </Typography>
                          )}
                        </Box>
                      ))}
                    </Stack>
                  ) : (
                    <Typography variant="body2" color="text.secondary">
                      Nessun elemento nel portfolio.
                    </Typography>
                  )}
                </Box>
              </Stack>
            )}

            {user.role === 'COMPANY' && !loading && profile && (
              <Stack spacing={2}>
                <Typography variant="body2" color="text.secondary">
                  {user.email}
                </Typography>

                <Box>
                  <Typography variant="subtitle2">Partita IVA</Typography>
                  <Typography variant="body2">{profile.vatNumber || 'Non specificata'}</Typography>
                </Box>

                <Box>
                  <Typography variant="subtitle2">Indirizzo</Typography>
                  <Typography variant="body2">{profile.address || 'Non specificato'}</Typography>
                </Box>

                <Box>
                  <Typography variant="subtitle2">Referente</Typography>
                  <Typography variant="body2">{profile.contactPerson || 'Non specificato'}</Typography>
                </Box>

                <Box>
                  <Typography variant="subtitle2">Descrizione</Typography>
                  <Typography variant="body2">{profile.description || 'Non specificata'}</Typography>
                </Box>

                <Box>
                  <Typography variant="subtitle2">Conto per l'invio dei pagamenti</Typography>
                  <Typography variant="body2">{profile.paymentAccount || 'Non ancora configurato'}</Typography>
                </Box>
              </Stack>
            )}
          </>
        )}

        {tab === 1 && (
          <>
            {reports === null && (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
                <CircularProgress size={28} />
              </Box>
            )}
            {reports?.length === 0 && (
              <Typography variant="body2" color="text.secondary">
                Nessuna segnalazione per questo utente.
              </Typography>
            )}
            {reports && reports.length > 0 && (
              <Stack spacing={2} divider={<Box sx={{ borderBottom: 1, borderColor: 'divider' }} />}>
                {reports.map((report) => (
                  <Box key={report.id}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        Da {report.reporterEmail}
                      </Typography>
                      <Chip
                        label={report.status === 'OPEN' ? 'Aperta' : 'Archiviata'}
                        color={report.status === 'OPEN' ? 'error' : 'default'}
                        size="small"
                      />
                    </Box>
                    <Typography variant="body2" sx={{ mb: 0.5 }}>
                      {report.reason}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {formatDate(report.createdAt)}
                    </Typography>
                    {!readOnly && report.status === 'OPEN' && (
                      <Box sx={{ mt: 0.5 }}>
                        <Button size="small" onClick={() => closeReport(report.id)}>
                          Archivia
                        </Button>
                      </Box>
                    )}
                  </Box>
                ))}
              </Stack>
            )}
          </>
        )}
      </DialogContent>
      <DialogActions>
        {!readOnly && user.status !== 'VALIDATED' && user.status !== 'DEACTIVATED' && (
          <Button
            variant="contained"
            onClick={() => {
              onValidate(user)
              onClose()
            }}
          >
            {user.status === 'SUSPENDED' ? 'Riattiva' : 'Valida'}
          </Button>
        )}
        {!readOnly && user.status !== 'SUSPENDED' && user.status !== 'DEACTIVATED' && (
          <Button color="error" onClick={() => onSuspend(user)}>
            Sospendi
          </Button>
        )}
        {!readOnly && user.status !== 'DEACTIVATED' && (
          <Button color="error" onClick={() => onDeactivate(user)}>
            Elimina
          </Button>
        )}
        <Button onClick={onClose}>Chiudi</Button>
      </DialogActions>
    </Dialog>
  )
}
