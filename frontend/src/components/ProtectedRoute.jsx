import { Navigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export default function ProtectedRoute({ allowedRoles, children }) {
  const { roles } = useAuth()
  const authorized = allowedRoles.some((role) => roles.includes(role))

  if (!authorized) {
    return <Navigate to="/unauthorized" replace />
  }

  return children
}
