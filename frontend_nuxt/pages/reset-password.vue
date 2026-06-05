<template>
  <div class="reset-page">
    <div class="auth-panel">
      <img class="whu-emblem" src="/whu-emblem.webp" alt="武汉大学校园论坛" />
      <div class="auth-title">重置密码</div>
      <div class="auth-subtitle">{{ hasToken ? '设置新的登录密码' : '重置链接无效' }}</div>

      <form v-if="hasToken" class="auth-form" @submit.prevent="submitReset">
        <div class="field-group">
          <label class="field-label" for="reset-password">新密码</label>
          <BaseInput
            id="reset-password"
            icon="lock"
            v-model="password"
            type="password"
            autocomplete="new-password"
            placeholder="请输入新密码"
          />
          <div v-if="passwordError" class="error-message">{{ passwordError }}</div>
        </div>

        <div class="field-group">
          <label class="field-label" for="reset-confirm-password">确认密码</label>
          <BaseInput
            id="reset-confirm-password"
            icon="lock"
            v-model="confirmPassword"
            type="password"
            autocomplete="new-password"
            placeholder="再次输入新密码"
          />
          <div v-if="confirmPasswordError" class="error-message">{{ confirmPasswordError }}</div>
        </div>

        <button class="primary-button" type="submit" :disabled="isSubmitting">
          <loading-four v-if="isSubmitting" class="button-icon" />
          <span>{{ isSubmitting ? '提交中...' : '重置密码' }}</span>
        </button>
      </form>

      <div v-else class="auth-form">
        <div class="mail-notice">
          <div class="notice-title">链接缺少 token</div>
          <div class="notice-copy">请重新发送密码重置邮件。</div>
        </div>
        <button class="secondary-button" type="button" @click="navigateTo('/forgot-password')">
          重新发送
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import BaseInput from '~/components/BaseInput.vue'
import { toast } from '~/main'
import { getApiErrorMessage } from '~/utils/apiError'

const config = useRuntimeConfig()
const route = useRoute()
const API_BASE_URL = config.public.apiBaseUrl

const password = ref('')
const confirmPassword = ref('')
const passwordError = ref('')
const confirmPasswordError = ref('')
const isSubmitting = ref(false)

const token = computed(() => {
  const raw = route.query.token
  return Array.isArray(raw) ? raw[0] || '' : raw || ''
})

const hasToken = computed(() => token.value.trim().length > 0)

const clearErrors = () => {
  passwordError.value = ''
  confirmPasswordError.value = ''
}

const submitReset = async () => {
  clearErrors()
  if (!password.value) {
    passwordError.value = '请输入新密码'
    return
  }
  if (password.value !== confirmPassword.value) {
    confirmPasswordError.value = '两次输入的密码不一致'
    return
  }

  try {
    isSubmitting.value = true
    const res = await fetch(`${API_BASE_URL}/api/auth/forgot/reset`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token: token.value.trim(), password: password.value }),
    })
    const data = await res.json().catch(() => ({}))
    if (res.ok) {
      toast.success('密码已重置，请重新登录')
      await navigateTo('/login', { replace: true })
      return
    }
    if (data.field === 'password') {
      passwordError.value = getApiErrorMessage(data, '密码不符合要求')
      return
    }
    toast.error(getApiErrorMessage(data, '重置失败，请稍后重试'))
  } catch (e) {
    toast.error('重置失败，请稍后重试')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.reset-page {
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
  text-align: center;
}

.auth-form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 10px;
}

.field-group {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-color);
}

.primary-button,
.secondary-button {
  width: 100%;
  min-height: 44px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.primary-button {
  border: none;
  background: #005c46;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.primary-button:hover {
  background: #003f72;
}

.primary-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.secondary-button {
  border: 1px solid var(--normal-border-color);
  background: transparent;
  color: var(--text-color);
}

.mail-notice {
  width: 100%;
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  box-sizing: border-box;
  text-align: center;
}

.notice-title {
  font-size: 18px;
  font-weight: 800;
  color: #005c46;
}

.notice-copy {
  font-size: 14px;
  color: var(--text-color);
}

.button-icon {
  font-size: 18px;
}

.error-message {
  color: #d93025;
  font-size: 13px;
}

@media (max-width: 768px) {
  .reset-page {
    align-items: flex-start;
    padding-top: 72px;
  }
}
</style>
