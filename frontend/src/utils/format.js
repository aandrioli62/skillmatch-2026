export function formatDate(isoString) {
  if (!isoString) return ''
  return new Date(isoString).toLocaleDateString('it-IT')
}

export const CANDIDATURE_STATUS_LABEL = {
  PENDING: { label: 'In attesa', color: 'warning' },
  ACCEPTED: { label: 'Accettata', color: 'success' },
  REJECTED: { label: 'Rifiutata', color: 'error' },
  WITHDRAWN: { label: 'Ritirata', color: 'default' },
}

export function candidatureStatusInfo(status) {
  return CANDIDATURE_STATUS_LABEL[status] ?? { label: status, color: 'default' }
}

export const PROJECT_STATUS_LABEL = {
  DRAFT: { label: 'Bozza', color: 'default' },
  OPEN: { label: 'Aperto', color: 'success' },
  ASSIGNED: { label: 'Assegnato', color: 'info' },
  IN_PROGRESS: { label: 'In corso', color: 'info' },
  COMPLETED: { label: 'Completato', color: 'default' },
  CLOSED: { label: 'Chiuso', color: 'default' },
}

export function projectStatusInfo(status) {
  return PROJECT_STATUS_LABEL[status] ?? { label: status, color: 'default' }
}

export const CONTRACT_STATUS_LABEL = {
  DRAFT: { label: 'Bozza', color: 'default' },
  PENDING_SIGNATURES: { label: 'In attesa di firme', color: 'warning' },
  ACTIVE: { label: 'Attivo', color: 'success' },
  COMPLETED: { label: 'Completato', color: 'default' },
  CANCELLED: { label: 'Annullato', color: 'error' },
}

export function contractStatusInfo(status) {
  return CONTRACT_STATUS_LABEL[status] ?? { label: status, color: 'default' }
}

export function shortId(uuid) {
  return uuid ? uuid.slice(0, 8) : ''
}
