import { Box, List, ListItem, ListItemText, Pagination, Typography } from '@mui/material'
import { useEffect, useState } from 'react'
import DataSection from '../../components/DataSection'
import api from '../../services/api'
import { formatDate, shortId } from '../../utils/format'

export default function AdminTransactions() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    api
      .get('/transactions/admin/all', { params: { page, size: 10 } })
      .then((res) => setData(res.data))
      .catch((err) => setError(err.message))
  }, [page])

  return (
    <>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 600 }}>
        Transazioni
      </Typography>

      <DataSection
        loading={data === null && !error}
        error={error}
        isEmpty={data?.content.length === 0}
        emptyLabel="Nessuna transazione ancora registrata."
      >
        <List disablePadding>
          {data?.content.map((tx) => (
            <ListItem key={tx.id} divider>
              <ListItemText
                primary={`€${tx.totalAmount} — Contratto #${shortId(tx.contractId)}`}
                secondary={`Commissione: €${tx.commissionAmount} — Netto: €${tx.netAmount} — ${formatDate(tx.completedAt)}`}
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
