import { Button, Chip, List, ListItem, ListItemText, Snackbar, Stack, Typography } from '@mui/material'
import { useEffect, useState } from 'react'
import DataSection from '../../components/DataSection'
import FeedbackDialog from '../../components/FeedbackDialog'
import ReportDialog from '../../components/ReportDialog'
import api from '../../services/api'
import { contractStatusInfo, shortId } from '../../utils/format'

export default function ProfessionalContracts() {
  const [contracts, setContracts] = useState(null)
  const [error, setError] = useState(null)

  // Fetched live rather than trusting contract.commissionRate, which is only
  // the platform default recorded when the contract was drafted — the admin
  // may have changed the rate since, and the actual charge at payment time
  // always uses whatever rate is current then, not what's stored on the
  // contract. Used only for the pre-payment estimate below; once paid, the
  // real transaction net amount is shown instead.
  const [commissionRate, setCommissionRate] = useState(null)

  const [transactions, setTransactions] = useState([])
  const [reviewedProjectIds, setReviewedProjectIds] = useState(new Set())

  const [feedbackTarget, setFeedbackTarget] = useState(null)
  const [reportTarget, setReportTarget] = useState(null)
  const [message, setMessage] = useState(null)

  const loadData = () => {
    api
      .get('/contracts/professional/mine')
      .then((res) => setContracts(res.data))
      .catch((err) => setError(err.message))

    api
      .get('/transactions/professional/mine')
      .then((res) => setTransactions(res.data))
      .catch(() => setTransactions([]))

    api
      .get('/feedbacks/given/mine')
      .then((res) => setReviewedProjectIds(new Set(res.data.map((f) => f.projectId))))
      .catch(() => setReviewedProjectIds(new Set()))
  }

  useEffect(loadData, [])

  useEffect(() => {
    api
      .get('/commission-config/current')
      .then((res) => setCommissionRate(res.data.ratePercentage))
      .catch(() => setCommissionRate(null))
  }, [])

  const transactionByContractId = new Map(transactions.map((tx) => [tx.contractId, tx]))
  const paidContractIds = new Set(transactions.map((tx) => tx.contractId))

  const signContract = (contract) => {
    api
      .put(`/contracts/${contract.id}/sign`)
      .then(() => {
        setMessage('Contratto firmato.')
        loadData()
      })
      .catch((err) => setMessage(err.response?.data?.detail || err.message))
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
            const isPaid = paidContractIds.has(contract.id)
            const transaction = transactionByContractId.get(contract.id)
            const rate = commissionRate ?? contract.commissionRate
            const netAmount = isPaid
              ? transaction.netAmount
              : (contract.amount - (contract.amount * rate) / 100).toFixed(2)
            return (
              <ListItem key={contract.id} divider>
                <ListItemText
                  primary={`Contratto #${shortId(contract.id)} — €${contract.amount}`}
                  secondary={`Netto per te: €${netAmount}${isPaid ? '' : ' (stima)'}`}
                />
                <Stack direction="row" spacing={1} alignItems="center">
                  {contract.status === 'PENDING_SIGNATURES' && (
                    <Button size="small" variant="contained" onClick={() => signContract(contract)}>
                      Firma
                    </Button>
                  )}
                  {contract.status === 'COMPLETED' && isPaid && !reviewedProjectIds.has(contract.projectId) && (
                    <Button size="small" variant="outlined" onClick={() => setFeedbackTarget(contract)}>
                      Lascia un feedback
                    </Button>
                  )}
                  {(contract.status === 'ACTIVE' || contract.status === 'COMPLETED') && (
                    <Button size="small" color="error" onClick={() => setReportTarget(contract)}>
                      Segnala
                    </Button>
                  )}
                  {contract.status !== 'PENDING_SIGNATURES' && (
                    <Chip label={statusInfo.label} color={statusInfo.color} size="small" />
                  )}
                </Stack>
              </ListItem>
            )
          })}
        </List>
      </DataSection>

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

      <ReportDialog
        open={Boolean(reportTarget)}
        reportedUserId={reportTarget?.companyId}
        onClose={() => setReportTarget(null)}
        onSubmitted={() => {
          setMessage('Segnalazione inviata.')
          setReportTarget(null)
        }}
      />

      <Snackbar open={Boolean(message)} autoHideDuration={4000} onClose={() => setMessage(null)} message={message} />
    </>
  )
}
