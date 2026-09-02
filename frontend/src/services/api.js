import axios from 'axios'
import keycloak from '../keycloak'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
})

api.interceptors.request.use(async (config) => {
  await keycloak.updateToken(30).catch(() => keycloak.login())
  config.headers.Authorization = `Bearer ${keycloak.token}`
  return config
})

export default api
