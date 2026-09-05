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
  Typography,
} from '@mui/material'
import { useEffect, useState } from 'react'
import api from '../services/api'

const STATUS_COLOR = {
  PENDING: 'warning',
  VALIDATED: 'success',
  SUSPENDED: 'error',
}

// Detail card shown when an admin clicks a row in "Validazione utenti" — the
// full picture the exam spec requires before validating a professional: bio,
// skills (with certification links), portfolio, and the payout account.
export default function UserDetailDialog({ user, onClose, onValidate, onSuspend }) {
  const [profile, setProfile] = useState(null)

  useEffect(() => {
    if (!user || user.role !== 'PROFESSIONAL') {
      return undefined
    }

    let cancelled = false
    api
      .get(`/admin/users/${user.id}/professional-profile`)
      .then((res) => {
        if (!cancelled) setProfile(res.data)
      })
      .catch(() => {
        if (!cancelled) setProfile(null)
      })

    return () => {
      cancelled = true
    }
  }, [user])

  const loading = Boolean(user) && user.role === 'PROFESSIONAL' && profile === null

  if (!user) return null

  return (
    <Dialog open={Boolean(user)} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {profile ? `${profile.firstName ?? ''} ${profile.lastName ?? ''}`.trim() || user.email : user.email}
          <Chip label={user.role} size="small" variant="outlined" />
          <Chip label={user.status} color={STATUS_COLOR[user.status] ?? 'default'} size="small" />
        </Box>
      </DialogTitle>
      <DialogContent dividers>
        {user.role !== 'PROFESSIONAL' && (
          <Typography variant="body2" color="text.secondary">
            {user.email}
          </Typography>
        )}

        {user.role === 'PROFESSIONAL' && loading && (
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
      </DialogContent>
      <DialogActions>
        {user.role === 'PROFESSIONAL' && user.status !== 'VALIDATED' && (
          <Button
            variant="contained"
            onClick={() => {
              onValidate(user)
              onClose()
            }}
          >
            Valida
          </Button>
        )}
        {user.status !== 'SUSPENDED' && (
          <Button color="error" onClick={() => onSuspend(user)}>
            Sospendi
          </Button>
        )}
        <Button onClick={onClose}>Chiudi</Button>
      </DialogActions>
    </Dialog>
  )
}
