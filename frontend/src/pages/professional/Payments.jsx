import { List, ListItem, ListItemText, Typography } from '@mui/material'
import { useEffect, useState } from 'react'
import DataSection from '../../components/DataSection'
import api from '../../services/api'
import { formatDate, shortId } from '../../utils/format'

export default function ProfessionalPayments() {
  const [transactions, setTransactions] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    api
      .get('/transactions/professional/mine')
      .then((res) => setTransactions(res.data))
      .catch((err) => setError(err.message))
  }, [])

  return (
    <>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 600 }}>
        I miei pagamenti
      </Typography>

      <DataSection
        loading={transactions === null && !error}
        error={error}
        isEmpty={transactions?.length === 0}
        emptyLabel="Non hai ancora ricevuto pagamenti."
      >
        <List disablePadding>
          {transactions?.map((tx) => (
            <ListItem key={tx.id} divider>
              <ListItemText
                primary={`Ricevuto: €${tx.netAmount}`}
                secondary={`Contratto #${shortId(tx.contractId)} — commissione trattenuta: €${tx.commissionAmount} — ${formatDate(tx.completedAt)}`}
              />
            </ListItem>
          ))}
        </List>
      </DataSection>
    </>
  )
}
