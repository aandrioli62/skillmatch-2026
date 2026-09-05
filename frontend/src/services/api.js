import axios from 'axios'
import keycloak from '../keycloak'

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

const api = axios.create({ baseURL })

api.interceptors.request.use(async (config) => {
  await keycloak.updateToken(30).catch(() => keycloak.login())
  config.headers.Authorization = `Bearer ${keycloak.token}`
  return config
})

export default api

// No auth interceptor: for anonymous endpoints (e.g. registration), where forcing
// a token refresh via the interceptor above would redirect an anonymous visitor
// straight to the login page instead of letting the request go through as-is.
export const publicApi = axios.create({ baseURL })
