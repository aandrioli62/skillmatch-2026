import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  List,
  ListItem,
  ListItemText,
  Typography,
} from '@mui/material'
import { useEffect, useState } from 'react'
import DataSection from '../../components/DataSection'
import api from '../../services/api'
import { formatDate, shortId } from '../../utils/format'

export default function CompanyPayments() {
  const [transactions, setTransactions] = useState(null)
  const [error, setError] = useState(null)

  const [invoice, setInvoice] = useState(null)
  const [invoiceError, setInvoiceError] = useState(null)

  useEffect(() => {
    api
      .get('/transactions/company/mine')
      .then((res) => setTransactions(res.data))
      .catch((err) => setError(err.message))
  }, [])

  const showInvoice = (transactionId) => {
    setInvoice(null)
    setInvoiceError(null)
    api
      .get(`/transactions/${transactionId}/invoice`)
      .then((res) => setInvoice(res.data))
      .catch((err) => setInvoiceError(err.response?.data?.detail || err.message))
  }

  return (
    <>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 600 }}>
        Pagamenti
      </Typography>

      <DataSection
        loading={transactions === null && !error}
        error={error}
        isEmpty={transactions?.length === 0}
        emptyLabel="Nessun pagamento effettuato finora."
      >
        <List disablePadding>
          {transactions?.map((tx) => (
            <ListItem key={tx.id} divider>
              <ListItemText
                primary={`Pagato: €${tx.totalAmount}`}
                secondary={`Contratto #${shortId(tx.contractId)} — commissione €${tx.commissionAmount} — ${formatDate(tx.completedAt)}`}
              />
              <Button size="small" variant="outlined" onClick={() => showInvoice(tx.id)}>
                Vedi fattura
              </Button>
            </ListItem>
          ))}
        </List>
      </DataSection>

      <Dialog open={Boolean(invoice) || Boolean(invoiceError)} onClose={() => { setInvoice(null); setInvoiceError(null) }} fullWidth maxWidth="xs">
        <DialogTitle>Fattura</DialogTitle>
        <DialogContent>
          {invoiceError && (
            <Typography variant="body2" color="error">
              {invoiceError}
            </Typography>
          )}
          {invoice && (
            <Box sx={{ display: 'grid', gap: 1 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                {invoice.invoiceNumber}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Emessa il {formatDate(invoice.issuedAt)}
              </Typography>
              <Divider sx={{ my: 1 }} />
              <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="body2">Totale</Typography>
                <Typography variant="body2">€{invoice.total}</Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="body2">Commissione piattaforma</Typography>
                <Typography variant="body2">€{invoice.commission}</Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  Compenso professionista
                </Typography>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  €{invoice.professionalFee}
                </Typography>
              </Box>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => { setInvoice(null); setInvoiceError(null) }}>Chiudi</Button>
        </DialogActions>
      </Dialog>
    </>
  )
}
