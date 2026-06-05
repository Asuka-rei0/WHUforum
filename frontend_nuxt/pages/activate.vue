<template>
  <div class="activate-page">
    <div class="auth-panel">
      <img class="whu-emblem" src="/whu-emblem.webp" alt="武汉大学校园论坛" />
      <div class="auth-title">邮箱验证</div>
      <div class="auth-subtitle">{{ statusText }}</div>

      <div v-if="isActivating" class="status-row">
        <loading-four class="status-icon" />
        <span>正在验证激活链接...</span>
      </div>

      <button
        v-if="showLoginAction"
        class="primary-button"
        type="button"
        @click="navigateTo('/login', { replace: true })"
      >
        返回登录
      </button>
    </div>
  </div>
</template>

<script setup>
import { toast } from '~/main'
import { getApiErrorMessage } from '~/utils/apiError'
import { setToken } from '~/utils/auth'
import { registerPush } from '~/utils/push'

const config = useRuntimeConfig()
const route = useRoute()
const API_BASE_URL = config.public.apiBaseUrl

const isActivating = ref(true)
const showLoginAction = ref(false)
const statusText = ref('正在确认你的武汉大学邮箱')

const tokenFromRoute = () => {
  const raw = route.query.token
  return Array.isArray(raw) ? raw[0] || '' : raw || ''
}

const completeLogin = async (token) => {
  await setToken(token)
  registerPush()
  toast.success('邮箱验证成功')
  await navigateTo('/', { replace: true })
}

const activateAccount = async () => {
  const token = tokenFromRoute().trim()
  if (!token) {
    statusText.value = '激活链接缺少 token'
    toast.error('激活链接无效')
    showLoginAction.value = true
    isActivating.value = false
    return
  }

  try {
    const res = await fetch(`${API_BASE_URL}/api/auth/activate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token }),
    })
    const data = await res.json().catch(() => ({}))
    if (res.ok && data.token && data.reason_code === 'VERIFIED_AND_APPROVED') {
      await completeLogin(data.token)
      return
    }
    if (res.ok && data.token) {
      toast.success('邮箱验证成功，请填写注册理由')
      await navigateTo(`/signup-reason?token=${data.token}`, { replace: true })
      return
    }
    statusText.value = getApiErrorMessage(data, '激活链接无效或已过期')
    toast.error(statusText.value)
    showLoginAction.value = true
  } catch (e) {
    statusText.value = '邮箱验证失败，请稍后重试'
    toast.error(statusText.value)
    showLoginAction.value = true
  } finally {
    isActivating.value = false
  }
}

onMounted(() => {
  activateAccount()
})
</script>

<style scoped>
.activate-page {
  min-height: 100%;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    linear-gradient(180deg, rgba(0, 92, 70, 0.08), transparent 42%), var(--background-color);
  padding: 32px 20px;
}

.auth-panel {
  width: min(100%, 390px);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-align: center;
}

.whu-emblem {
  width: 104px;
  height: 104px;
  object-fit: contain;
}

.auth-title {
  font-size: 34px;
  font-weight: 800;
  color: #005c46;
}

.auth-subtitle {
  font-size: 15px;
  color: var(--text-color);
  opacity: 0.72;
}

.status-row {
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-color);
  font-size: 14px;
}

.status-icon {
  font-size: 18px;
}

.primary-button {
  width: 100%;
  min-height: 44px;
  border: none;
  border-radius: 8px;
  background: #005c46;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.primary-button:hover {
  background: #003f72;
}

@media (max-width: 768px) {
  .activate-page {
    align-items: flex-start;
    padding-top: 72px;
  }
}
</style>
