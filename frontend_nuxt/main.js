export { toast } from './composables/useToast'

export const API_DOMAIN = 'https://www.open-isle.com'
export const API_PORT = ''
export const API_BASE_URL = API_PORT ? `${API_DOMAIN}:${API_PORT}` : API_DOMAIN
