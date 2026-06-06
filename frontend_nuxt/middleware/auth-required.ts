import { authState, getToken } from '~/utils/auth'

export default defineNuxtRouteMiddleware((to) => {
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

  authState.loggedIn = true
})
