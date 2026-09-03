import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  List,
  ListItem,
  ListItemText,
  Snackbar,
  Stack,
  Typography,
} from '@mui/material'
import { useEffect, useState } from 'react'
import DataSection from '../../components/DataSection'
import FeedbackDialog from '../../components/FeedbackDialog'
import api from '../../services/api'
import { contractStatusInfo, formatDate, shortId } from '../../utils/format'

export default function CompanyContracts() {
  const [contracts, setContracts] = useState(null)
  const [error, setError] = useState(null)

  const [transactions, setTransactions] = useState([])
  const [reviewedProjectIds, setReviewedProjectIds] = useState(new Set())

  const [feedbackTarget, setFeedbackTarget] = useState(null)

  const [completeTarget, setCompleteTarget] = useState(null)
  const [payTarget, setPayTarget] = useState(null)
  const [payError, setPayError] = useState(null)
  const [paying, setPaying] = useState(false)

  const [invoice, setInvoice] = useState(null)
  const [invoiceError, setInvoiceError] = useState(null)

  const [message, setMessage] = useState(null)

  const loadData = () => {
    api
      .get('/contracts/company/mine')
      .then((res) => setContracts(res.data))
      .catch((err) => setError(err.message))

    api
      .get('/transactions/company/mine')
      .then((res) => setTransactions(res.data))
      .catch(() => setTransactions([]))

    api
      .get('/feedbacks/given/mine')
      .then((res) => setReviewedProjectIds(new Set(res.data.map((f) => f.projectId))))
      .catch(() => setReviewedProjectIds(new Set()))
  }

  useEffect(loadData, [])

  const transactionByContractId = new Map(transactions.map((tx) => [tx.contractId, tx]))

  const signContract = (contract) => {
    api
      .put(`/contracts/${contract.id}/sign`)
      .then(() => {
        setMessage('Contratto firmato.')
        loadData()
      })
      .catch((err) => setMessage(err.response?.data?.detail || err.message))
  }

  const completeContract = () => {
    api
      .put(`/contracts/${completeTarget.id}/complete`)
      .then(() => {
        setMessage('Contratto completato.')
        setCompleteTarget(null)
        loadData()
      })
      .catch((err) => setMessage(err.response?.data?.detail || err.message))
  }

  const showInvoice = (transactionId) => {
    setInvoice(null)
    setInvoiceError(null)
    api
      .get(`/transactions/${transactionId}/invoice`)
      .then((res) => setInvoice(res.data))
      .catch((err) => setInvoiceError(err.response?.data?.detail || err.message))
  }

  const payContract = () => {
    setPaying(true)
    setPayError(null)
    api
      .post('/payments', { contractId: payTarget.id })
      .then((res) => {
        setMessage('Pagamento completato.')
        setPayTarget(null)
        loadData()
        showInvoice(res.data.id)
      })
      .catch((err) => setPayError(err.response?.data?.detail || err.message))
      .finally(() => setPaying(false))
  }

  return (
    <>
      <Typography variant="h5" sx={{ mb: 3, fontWeight: 600 }}>
        I miei contratti
      </Typography>

      <DataSection
        loading={contracts === null && !error}
        error={error}
        isEmpty={contracts?.length === 0}
        emptyLabel="Nessun contratto ancora generato."
      >
        <List disablePadding>
          {contracts?.map((contract) => {
            const statusInfo = contractStatusInfo(contract.status)
            const transaction = transactionByContractId.get(contract.id)
            return (
              <ListItem key={contract.id} divider>
                <ListItemText
                  primary={`Contratto #${shortId(contract.id)} — €${contract.amount}`}
                  secondary={`Professionista #${shortId(contract.professionalId)} — commissione ${contract.commissionRate}%`}
                />
                <Stack direction="row" spacing={1} alignItems="center">
                  {contract.status === 'DRAFT' && (
                    <Button size="small" variant="contained" onClick={() => signContract(contract)}>
                      Firma
                    </Button>
                  )}
                  {contract.status === 'ACTIVE' && (
                    <Button size="small" variant="outlined" color="success" onClick={() => setCompleteTarget(contract)}>
                      Completa
                    </Button>
                  )}
                  {contract.status === 'COMPLETED' && !transaction && (
                    <Button size="small" variant="contained" onClick={() => setPayTarget(contract)}>
                      Paga
                    </Button>
                  )}
                  {contract.status === 'COMPLETED' && transaction && (
                    <Button size="small" variant="outlined" onClick={() => showInvoice(transaction.id)}>
                      Vedi fattura
                    </Button>
                  )}
                  {contract.status === 'COMPLETED' &&
                    transaction &&
                    !reviewedProjectIds.has(contract.projectId) && (
                      <Button size="small" variant="outlined" onClick={() => setFeedbackTarget(contract)}>
                        Lascia un feedback
                      </Button>
                    )}
                  {!['DRAFT', 'ACTIVE'].includes(contract.status) &&
                    !(contract.status === 'COMPLETED' && !transaction) && (
                      <Chip label={statusInfo.label} color={statusInfo.color} size="small" />
                    )}
                </Stack>
              </ListItem>
            )
          })}
        </List>
      </DataSection>

      <Dialog open={Boolean(completeTarget)} onClose={() => setCompleteTarget(null)}>
        <DialogTitle>Completare il contratto?</DialogTitle>
        <DialogContent>
          <Typography variant="body2">
            Confermi che il lavoro relativo al contratto #{shortId(completeTarget?.id)} è terminato? Dopo potrai
            procedere al pagamento.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCompleteTarget(null)}>Annulla</Button>
          <Button variant="contained" color="success" onClick={completeContract}>
            Completa contratto
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={Boolean(payTarget)} onClose={() => !paying && setPayTarget(null)}>
        <DialogTitle>Confermare il pagamento?</DialogTitle>
        <DialogContent>
          <Typography variant="body2">
            Stai per pagare €{payTarget?.amount} per il contratto #{shortId(payTarget?.id)}. La piattaforma
            tratterrà una commissione del {payTarget?.commissionRate}%. L'operazione non può essere annullata.
          </Typography>
          {payError && (
            <Typography variant="body2" color="error" sx={{ mt: 2 }}>
              {payError}
            </Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPayTarget(null)} disabled={paying}>
            Annulla
          </Button>
          <Button variant="contained" onClick={payContract} disabled={paying}>
            Paga ora
          </Button>
        </DialogActions>
      </Dialog>

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

      <FeedbackDialog
        open={Boolean(feedbackTarget)}
        projectId={feedbackTarget?.projectId}
        onClose={() => setFeedbackTarget(null)}
        onSubmitted={() => {
          setMessage('Feedback inviato.')
          setFeedbackTarget(null)
          loadData()
        }}
      />

      <Snackbar open={Boolean(message)} autoHideDuration={4000} onClose={() => setMessage(null)} message={message} />
    </>
  )
}
