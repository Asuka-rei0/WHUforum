import { getToken } from '~/utils/auth'

const authHeaders = () => {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export async function fetchFollowings(username) {
  if (!username) return []
  const config = useRuntimeConfig()
  const API_BASE_URL = config.public.apiBaseUrl
  try {
    const res = await fetch(`${API_BASE_URL}/api/users/${username}/following`, {
      headers: authHeaders(),
    })
    return res.ok ? await res.json() : []
  } catch (e) {
    return []
  }
}

export async function fetchAdmins() {
  const config = useRuntimeConfig()
  const API_BASE_URL = config.public.apiBaseUrl
  try {
    const res = await fetch(`${API_BASE_URL}/api/users/admins`, { headers: authHeaders() })
    return res.ok ? await res.json() : []
  } catch (e) {
    return []
  }
}

export async function searchUsers(keyword) {
  if (!keyword) return []
  const config = useRuntimeConfig()
  const API_BASE_URL = config.public.apiBaseUrl
  try {
    const res = await fetch(
      `${API_BASE_URL}/api/search/users?keyword=${encodeURIComponent(keyword)}`,
      {
        headers: authHeaders(),
      },
    )
    return res.ok ? await res.json() : []
  } catch (e) {
    return []
  }
}
