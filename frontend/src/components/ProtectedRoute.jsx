import { useEffect } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export default function ProtectedRoute({ allowedRoles, children }) {
  const { authenticated, roles, keycloak } = useAuth()

  useEffect(() => {
    if (!authenticated) {
      keycloak.login()
    }
  }, [authenticated, keycloak])

  if (!authenticated) {
    return null
  }

  const authorized = !allowedRoles || allowedRoles.some((role) => roles.includes(role))

  if (!authorized) {
    return <Navigate to="/unauthorized" replace />
  }

  return children
}
