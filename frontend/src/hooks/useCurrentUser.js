import { useCallback, useEffect, useState } from 'react'
import api from '../services/api'

// Resolves the internal user record for the authenticated caller. A 404 means the
// Keycloak identity has no matching row in user-service yet (e.g. an account created
// directly in Keycloak, bypassing /register) — RequireProfile uses this to prompt
// for the missing registration step instead of leaving the app in a broken state.
export function useCurrentUser() {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [notRegistered, setNotRegistered] = useState(false)

  const refetch = useCallback(() => {
    api
      .get('/users/me')
      .then((res) => {
        setUser(res.data)
        setNotRegistered(false)
      })
      .catch((err) => {
        if (err.response?.status === 404) {
          setNotRegistered(true)
        }
      })
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    refetch()
  }, [refetch])

  return { user, loading, notRegistered, refetch }
}
