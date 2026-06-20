import { authState, getToken, loadCurrentUser } from '~/utils/auth'

export default defineNuxtRouteMiddleware(async (to) => {
  if (import.meta.server) return

  const token = getToken()
  if (!token) {
    return navigateTo(
      {
        path: '/login',
        query: { redirect: to.fullPath },
      },
      { replace: true },
    )
  }

  if (!authState.role) {
    await loadCurrentUser()
  }

  if (authState.role !== 'ADMIN') {
    return navigateTo('/', { replace: true })
  }
})
