import { clearToken, getToken } from '~/utils/auth'

const authorizationFromHeaders = (headers?: HeadersInit): string | null => {
  if (!headers) return null
  if (headers instanceof Headers) return headers.get('Authorization')
  if (Array.isArray(headers)) {
    const entry = headers.find(([key]) => key.toLowerCase() === 'authorization')
    return entry?.[1] ?? null
  }
  const record = headers as Record<string, string>
  return record.Authorization ?? record.authorization ?? null
}

const bearerTokenFromRequest = (input: RequestInfo | URL, init?: RequestInit): string | null => {
  const authorization =
    authorizationFromHeaders(init?.headers) ??
    (input instanceof Request ? input.headers.get('Authorization') : null)
  if (!authorization?.startsWith('Bearer ')) return null
  return authorization.substring(7).trim() || null
}

export default defineNuxtPlugin(() => {
  if (import.meta.client) {
    const patchedWindow = window as any
    if (patchedWindow.__openisleAuthFetchPatched) return
    patchedWindow.__openisleAuthFetchPatched = true
    const originalFetch = window.fetch
    window.fetch = async (input, init) => {
      const requestToken = bearerTokenFromRequest(input, init)
      const response = await originalFetch(input, init)
      if (response.status === 401) {
        try {
          const data = await response.clone().json()
          if (
            data &&
            data.error === 'Invalid or expired token' &&
            requestToken &&
            requestToken === getToken()
          ) {
            clearToken()
          }
        } catch (e) {
          // ignore JSON parsing errors
        }
      }
      return response
    }
  }
})
