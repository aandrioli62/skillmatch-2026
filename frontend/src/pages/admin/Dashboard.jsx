import { Box, Paper, Typography } from '@mui/material'
import { useEffect, useState } from 'react'
import { useAuth } from '../../context/AuthContext'
import api from '../../services/api'

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

export default function AdminDashboard() {
  const { username } = useAuth()

  const [pendingCount, setPendingCount] = useState(null)
  const [transactionCount, setTransactionCount] = useState(null)
  const [commissionRate, setCommissionRate] = useState(null)

  useEffect(() => {
    api
      .get('/admin/users', { params: { size: 100 } })
      .then((res) => {
        const pending = res.data.content.filter((u) => u.role === 'PROFESSIONAL' && u.status === 'PENDING')
        setPendingCount(pending.length)
      })
      .catch(() => setPendingCount('—'))

    api
      .get('/transactions/admin/all', { params: { size: 1 } })
      .then((res) => setTransactionCount(res.data.totalElements))
      .catch(() => setTransactionCount('—'))

    api
      .get('/admin/commission-config')
      .then((res) => setCommissionRate(res.data.ratePercentage))
      .catch(() => setCommissionRate('—'))
  }, [])

  return (
    <>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 600 }}>
        Bentornato, {username}
      </Typography>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' }, gap: 2 }}>
        <StatCard label="Utenti da validare" value={pendingCount ?? '…'} />
        <StatCard label="Transazioni totali" value={transactionCount ?? '…'} />
        <StatCard label="Commissione attuale" value={commissionRate !== null ? `${commissionRate}%` : '…'} />
      </Box>
    </>
  )
}
