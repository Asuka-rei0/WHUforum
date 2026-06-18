import { toast } from '~/main'
import { getApiErrorMessage } from '~/utils/apiError'
import { getToken } from '~/utils/auth'

export const useContentReport = () => {
  const config = useRuntimeConfig()
  const API_BASE_URL = config.public.apiBaseUrl

  const submitReport = async ({ targetType, targetId, reason = 'OTHER', detail = '' }) => {
    const token = getToken()
    if (!token) {
      toast.error('请先登录')
      return false
    }
    const res = await fetch(`${API_BASE_URL}/api/reports`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify({ targetType, targetId, reason, detail }),
    })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
      toast.error(getApiErrorMessage(data, '举报提交失败，请稍后重试'))
      return false
    }
    toast.success('举报已提交')
    return true
  }

  return { submitReport }
}
