import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Divider,
  IconButton,
  Paper,
  Snackbar,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useEffect, useState } from 'react'
import SkillPicker from '../components/SkillPicker'
import { useAuth } from '../hooks/useAuth'
import { useCurrentUser } from '../hooks/useCurrentUser'
import api from '../services/api'

function ProfessionalProfileForm({ userId }) {
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [bio, setBio] = useState('')
  const [paymentAccount, setPaymentAccount] = useState('')
  const [skillNames, setSkillNames] = useState([])
  const [certUrls, setCertUrls] = useState({})
  const [portfolioItems, setPortfolioItems] = useState([])

  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    api.get(`/users/${userId}/professional-profile`).then((res) => {
      const data = res.data
      setFirstName(data.firstName || '')
      setLastName(data.lastName || '')
      setBio(data.bio || '')
      setPaymentAccount(data.paymentAccount || '')
      setSkillNames((data.skills || []).map((s) => s.skillName))
      setCertUrls(
        Object.fromEntries((data.skills || []).map((s) => [s.skillName, s.certificationUrl || ''])),
      )
      setPortfolioItems(
        (data.portfolioItems || []).map((p) => ({ title: p.title, description: p.description || '', url: p.url || '' })),
      )
      setLoading(false)
    })
  }, [userId])

  const addPortfolioItem = () => {
    setPortfolioItems((prev) => [...prev, { title: '', description: '', url: '' }])
  }
  const updatePortfolioItem = (index, field, value) => {
    setPortfolioItems((prev) => prev.map((item, i) => (i === index ? { ...item, [field]: value } : item)))
  }
  const removePortfolioItem = (index) => {
    setPortfolioItems((prev) => prev.filter((_, i) => i !== index))
  }

  const submit = (e) => {
    e.preventDefault()
    setSaving(true)
    setError(null)

    Promise.all([
      api.put(`/users/${userId}/professional-profile`, { firstName, lastName, bio, paymentAccount }),
      api.put(
        `/users/${userId}/skills`,
        skillNames.map((skillName) => ({ skillName, certificationUrl: certUrls[skillName] || undefined })),
      ),
      api.put(`/users/${userId}/portfolio-items`, portfolioItems.filter((p) => p.title.trim())),
    ])
      .then(() => setSaved(true))
      .catch((err) => setError(err.response?.data?.detail || err.message))
      .finally(() => setSaving(false))
  }

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Paper sx={{ p: 4, maxWidth: 640 }} component="form" onSubmit={submit}>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 700 }}>
        Il mio profilo
      </Typography>
      <Stack spacing={2}>
        <Stack direction="row" spacing={2}>
          <TextField label="Nome" value={firstName} onChange={(e) => setFirstName(e.target.value)} required fullWidth />
          <TextField label="Cognome" value={lastName} onChange={(e) => setLastName(e.target.value)} required fullWidth />
        </Stack>
        <TextField label="Bio" value={bio} onChange={(e) => setBio(e.target.value)} multiline minRows={3} fullWidth />
        <TextField
          label="Conto per la ricezione dei compensi"
          value={paymentAccount}
          onChange={(e) => setPaymentAccount(e.target.value)}
          fullWidth
          helperText="IBAN o riferimento del conto su cui ricevere i pagamenti"
        />

        <Divider />
        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
          Competenze
        </Typography>
        <SkillPicker
          multiple
          label="Aggiungi competenze"
          value={skillNames}
          onChange={setSkillNames}
        />
        {skillNames.map((name) => (
          <TextField
            key={name}
            label={`Link certificato per "${name}" (opzionale)`}
            value={certUrls[name] || ''}
            onChange={(e) => setCertUrls((prev) => ({ ...prev, [name]: e.target.value }))}
            fullWidth
            size="small"
          />
        ))}

        <Divider />
        <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
          Portfolio
        </Typography>
        {portfolioItems.map((item, index) => (
          <Stack direction="row" spacing={1} key={index} sx={{ alignItems: 'flex-start' }}>
            <Stack spacing={1} sx={{ flex: 1 }}>
              <TextField
                label="Titolo"
                value={item.title}
                onChange={(e) => updatePortfolioItem(index, 'title', e.target.value)}
                fullWidth
                size="small"
              />
              <TextField
                label="Descrizione"
                value={item.description}
                onChange={(e) => updatePortfolioItem(index, 'description', e.target.value)}
                fullWidth
                size="small"
              />
              <TextField
                label="Link"
                value={item.url}
                onChange={(e) => updatePortfolioItem(index, 'url', e.target.value)}
                fullWidth
                size="small"
              />
            </Stack>
            <IconButton onClick={() => removePortfolioItem(index)} size="small">
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Stack>
        ))}
        <Button startIcon={<AddIcon />} onClick={addPortfolioItem} sx={{ alignSelf: 'flex-start' }}>
          Aggiungi elemento al portfolio
        </Button>

        {error && <Alert severity="error">{error}</Alert>}
        <Button type="submit" variant="contained" disabled={saving}>
          Salva
        </Button>
      </Stack>
      <Snackbar
        open={saved}
        autoHideDuration={3000}
        onClose={() => setSaved(false)}
        message="Profilo salvato"
      />
    </Paper>
  )
}

function CompanyProfileForm({ userId }) {
  const [companyName, setCompanyName] = useState('')
  const [vatNumber, setVatNumber] = useState('')
  const [address, setAddress] = useState('')
  const [contactPerson, setContactPerson] = useState('')

  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    api.get(`/users/${userId}/company-profile`).then((res) => {
      const data = res.data
      setCompanyName(data.companyName || '')
      setVatNumber(data.vatNumber || '')
      setAddress(data.address || '')
      setContactPerson(data.contactPerson || '')
      setLoading(false)
    })
  }, [userId])

  const submit = (e) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    api
      .put(`/users/${userId}/company-profile`, { companyName, vatNumber, address, contactPerson })
      .then(() => setSaved(true))
      .catch((err) => setError(err.response?.data?.detail || err.message))
      .finally(() => setSaving(false))
  }

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Paper sx={{ p: 4, maxWidth: 640 }} component="form" onSubmit={submit}>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 700 }}>
        Il mio profilo
      </Typography>
      <Stack spacing={2}>
        <TextField label="Ragione sociale" value={companyName} onChange={(e) => setCompanyName(e.target.value)} required fullWidth />
        <TextField label="Partita IVA" value={vatNumber} onChange={(e) => setVatNumber(e.target.value)} fullWidth />
        <TextField label="Indirizzo" value={address} onChange={(e) => setAddress(e.target.value)} fullWidth />
        <TextField label="Referente" value={contactPerson} onChange={(e) => setContactPerson(e.target.value)} fullWidth />
        {error && <Alert severity="error">{error}</Alert>}
        <Button type="submit" variant="contained" disabled={saving}>
          Salva
        </Button>
      </Stack>
      <Snackbar
        open={saved}
        autoHideDuration={3000}
        onClose={() => setSaved(false)}
        message="Profilo salvato"
      />
    </Paper>
  )
}

export default function Profile() {
  const { hasRole } = useAuth()
  const { user, loading } = useCurrentUser()

  if (loading || !user) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    )
  }

  if (hasRole('PROFESSIONAL')) return <ProfessionalProfileForm userId={user.id} />
  if (hasRole('COMPANY')) return <CompanyProfileForm userId={user.id} />
  return <Alert severity="info">Non c'è un profilo da modificare per questo ruolo.</Alert>
}
