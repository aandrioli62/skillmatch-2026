import { useEffect, useState } from 'react'
import api from '../services/api'
import { useAuth } from './useAuth'
import { useCurrentUser } from './useCurrentUser'

// Resolves a human-friendly display name for the top bar greeting: first+last
// name for professionals, company name (ragione sociale) for companies. Admins
// have no user-service row by design, so their name comes straight from
// Keycloak's own profile instead.
export function useDisplayName() {
  const { keycloak, hasRole } = useAuth()
  const { user } = useCurrentUser()
  const [fetchedName, setFetchedName] = useState(null)

  const isAdmin = hasRole('ADMIN')
  const { given_name: givenName, family_name: familyName } = keycloak.tokenParsed || {}
  const adminName = [givenName, familyName].filter(Boolean).join(' ') || null

  useEffect(() => {
    if (isAdmin || !user) return

    const endpoint =
      user.role === 'PROFESSIONAL'
        ? `/users/${user.id}/professional-profile`
        : `/users/${user.id}/company-profile`

    api.get(endpoint).then((res) => {
      setFetchedName(
        user.role === 'PROFESSIONAL'
          ? [res.data.firstName, res.data.lastName].filter(Boolean).join(' ')
          : res.data.companyName,
      )
    })
  }, [isAdmin, user])

  return isAdmin ? adminName : fetchedName
}
