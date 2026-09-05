import { Box, CircularProgress } from '@mui/material'
import { useEffect, useRef, useState } from 'react'
import { AuthContext } from '../hooks/useAuth'
import keycloak from '../keycloak'

export function AuthProvider({ children }) {
  const [initialized, setInitialized] = useState(false)
  const didInit = useRef(false)

  useEffect(() => {
    // React 18/19 StrictMode invokes effects twice in dev; keycloak-js
    // throws if init() is called a second time on the same instance.
    if (didInit.current) return
    didInit.current = true

    keycloak
      .init({
        onLoad: 'login-required',
        pkceMethod: 'S256',
        checkLoginIframe: false,
      })
      .finally(() => setInitialized(true))

    keycloak.onTokenExpired = () => {
      keycloak.updateToken(30).catch(() => keycloak.login())
    }
  }, [])

  if (!initialized) {
    return (
      <Box
        sx={{
          display: 'flex',
          minHeight: '100vh',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <CircularProgress />
      </Box>
    )
  }

  const roles = keycloak.tokenParsed?.realm_access?.roles ?? []

  const value = {
    keycloak,
    roles,
    hasRole: (role) => roles.includes(role),
    username: keycloak.tokenParsed?.preferred_username,
    // Trailing slash required: Keycloak's redirect URI wildcard match (".../*")
    // only matches paths under that prefix, and window.location.origin never
    // has a trailing slash on its own.
    logout: () => keycloak.logout({ redirectUri: window.location.origin + '/' }),
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
