import {
  Box,
  Grid,
  List,
  ListItem,
  ListItemText,
  MenuItem,
  Pagination,
  Paper,
  TextField,
  Typography,
} from '@mui/material'
import { useEffect, useState } from 'react'
import DataSection from '../../components/DataSection'
import api from '../../services/api'
import { formatDate, shortId } from '../../utils/format'

const STATUS_OPTIONS = ['INITIATED', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED']

function StatCard({ label, value }) {
  return (
    <Paper variant="outlined" sx={{ p: 2, borderRadius: 3 }}>
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="h5" sx={{ mt: 0.5, fontWeight: 700 }}>
        {value}
      </Typography>
    </Paper>
  )
}

export default function AdminTransactions() {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')

  const [data, setData] = useState(null)
  const [error, setError] = useState(null)
  const [summary, setSummary] = useState(null)

  const filterParams = {
    status: status || undefined,
    from: from ? `${from}T00:00:00` : undefined,
    to: to ? `${to}T23:59:59` : undefined,
  }

  useEffect(() => {
    api
      .get('/transactions/admin/all', { params: { ...filterParams, page, size: 10 } })
      .then((res) => setData(res.data))
      .catch((err) => setError(err.message))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, status, from, to])

  useEffect(() => {
    api
      .get('/transactions/admin/summary', { params: filterParams })
      .then((res) => setSummary(res.data))
      .catch(() => setSummary(null))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, from, to])

  const resetPageAnd = (setter) => (value) => {
    setPage(0)
    setter(value)
  }

  return (
    <>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 600 }}>
        Transazioni
      </Typography>

      <Paper variant="outlined" sx={{ p: 2, mb: 3, borderRadius: 3 }}>
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField
              select
              fullWidth
              label="Stato"
              value={status}
              onChange={(e) => resetPageAnd(setStatus)(e.target.value)}
            >
              <MenuItem value="">Tutti</MenuItem>
              {STATUS_OPTIONS.map((s) => (
                <MenuItem key={s} value={s}>
                  {s}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
            <TextField
              type="date"
              fullWidth
              label="Dal"
              InputLabelProps={{ shrink: true }}
              value={from}
              onChange={(e) => resetPageAnd(setFrom)(e.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
            <TextField
              type="date"
              fullWidth
              label="Al"
              InputLabelProps={{ shrink: true }}
              value={to}
              onChange={(e) => resetPageAnd(setTo)(e.target.value)}
            />
          </Grid>
        </Grid>
      </Paper>

      {summary && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid size={{ xs: 6, sm: 3 }}>
            <StatCard label="Transazioni" value={summary.count} />
          </Grid>
          <Grid size={{ xs: 6, sm: 3 }}>
            <StatCard label="Volume totale" value={`€${summary.totalVolume}`} />
          </Grid>
          <Grid size={{ xs: 6, sm: 3 }}>
            <StatCard label="Commissioni incassate" value={`€${summary.totalCommission}`} />
          </Grid>
          <Grid size={{ xs: 6, sm: 3 }}>
            <StatCard label="Netto ai professionisti" value={`€${summary.totalNet}`} />
          </Grid>
        </Grid>
      )}

      <DataSection
        loading={data === null && !error}
        error={error}
        isEmpty={data?.content.length === 0}
        emptyLabel="Nessuna transazione per i filtri selezionati."
      >
        <List disablePadding>
          {data?.content.map((tx) => (
            <ListItem key={tx.id} divider>
              <ListItemText
                primary={`€${tx.totalAmount} — Contratto #${shortId(tx.contractId)}`}
                secondary={`Commissione: €${tx.commissionAmount} — Netto: €${tx.netAmount} — ${tx.status} — ${formatDate(tx.completedAt)}`}
              />
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
    </>
  )
}
