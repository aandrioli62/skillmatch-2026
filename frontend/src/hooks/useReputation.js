import { useEffect, useState } from 'react'
import api from '../services/api'
import { useCurrentUser } from './useCurrentUser'

// Resolves the authenticated professional's own reputation (level, average rating,
// review count) for display in the header, next to the role chip. Returns null for
// any other role, or before the profile has loaded.
export function useReputation() {
  const { user } = useCurrentUser()
  const [reputation, setReputation] = useState(null)

  useEffect(() => {
    if (!user || user.role !== 'PROFESSIONAL') return

    api.get(`/users/${user.id}/professional-profile`).then((res) => {
      setReputation({
        level: res.data.reputationLevel,
        avgRating: res.data.avgRating,
        totalReviews: res.data.totalReviews,
      })
    })
  }, [user])

  return reputation
}
