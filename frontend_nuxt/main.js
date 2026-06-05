export { toast } from './composables/useToast'

export const API_DOMAIN = 'http://localhost:8080'
export const API_PORT = ''
export const API_BASE_URL = API_PORT ? `${API_DOMAIN}:${API_PORT}` : API_DOMAIN
