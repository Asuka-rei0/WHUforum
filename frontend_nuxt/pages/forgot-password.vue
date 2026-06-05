<template>
  <div class="forgot-page">
    <div class="forgot-content">
      <div class="forgot-title">找回密码</div>

      <form class="step-content" @submit.prevent="sendResetLink">
        <BaseInput
          icon="mail"
          v-model="email"
          type="email"
          autocomplete="email"
          placeholder="name@whu.edu.cn"
          :disabled="emailSent"
        />
        <div v-if="emailError" class="error-message">{{ emailError }}</div>

        <div v-if="emailSent" class="mail-notice">
          <div class="notice-title">重置邮件已发送</div>
          <div class="notice-copy">{{ sentEmail }}</div>
          <button class="secondary-button" type="button" @click="resetForm({ keepEmail: true })">
            重新填写邮箱
          </button>
        </div>

        <button class="primary-button" type="submit" :disabled="isSending || emailSent">
          {{ isSending ? '发送中...' : emailSent ? '已发送' : '发送重置链接' }}
        </button>
      </form>

      <div class="hint-message">
        <info-icon />
        请输入已注册的 @whu.edu.cn 邮箱找回密码
      </div>
    </div>
  </div>
</template>

<script setup>
import { toast } from '~/main'
import BaseInput from '~/components/BaseInput.vue'
import { getApiErrorMessage } from '~/utils/apiError'
import { useRoute } from 'vue-router'

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl
const route = useRoute()

const email = ref('')
const sentEmail = ref('')
const emailError = ref('')
const isSending = ref(false)
const emailSent = ref(false)

const normalizeEmail = (value) => (value || '').trim().toLowerCase()
const isWhuEmail = (value) => normalizeEmail(value).endsWith('@whu.edu.cn')

const routeEmail = () => {
  const rawEmail = Array.isArray(route.query.email) ? route.query.email[0] : route.query.email
  return normalizeEmail(rawEmail)
}

const resetForm = ({ keepEmail = false } = {}) => {
  if (keepEmail) {
    email.value = normalizeEmail(email.value)
  } else {
    email.value = routeEmail()
  }
  sentEmail.value = ''
  emailError.value = ''
  isSending.value = false
  emailSent.value = false
}

onMounted(resetForm)
onActivated(resetForm)

const sendResetLink = async () => {
  emailError.value = ''
  const normalizedEmail = normalizeEmail(email.value)
  if (!isWhuEmail(normalizedEmail)) {
    emailError.value = '请使用已注册的 @whu.edu.cn 邮箱'
    return
  }

  try {
    isSending.value = true
    const res = await fetch(`${API_BASE_URL}/api/auth/forgot/send`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: normalizedEmail }),
    })
    const data = await res.json().catch(() => ({}))
    if (res.ok) {
      sentEmail.value = normalizedEmail
      emailSent.value = true
      toast.success('重置邮件已发送，请查收武汉大学邮箱')
      return
    }
    if (data.field === 'email' || data.reason_code === 'WHU_EMAIL_REQUIRED') {
      emailError.value = getApiErrorMessage(data, '请使用已注册的 @whu.edu.cn 邮箱')
      return
    }
    toast.error(getApiErrorMessage(data, '请填写已注册邮箱'))
  } catch (e) {
    toast.error('发送失败，请稍后重试')
  } finally {
    isSending.value = false
  }
}
</script>

<style scoped>
.forgot-page {
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: var(--background-color);
  height: 100%;
  padding: 32px 20px;
  box-sizing: border-box;
}

.forgot-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: min(100%, 400px);
}

.forgot-title {
  font-size: 24px;
  font-weight: bold;
}

.forgot-content .hint-message {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  font-size: 13px;
  color: var(--blockquote-text-color);
}

.hint-message i {
  color: var(--primary-color);
  font-size: 14px;
}

.step-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.primary-button {
  border: none;
  background-color: var(--primary-color);
  color: white;
  padding: 10px 20px;
  border-radius: 8px;
  min-height: 44px;
  text-align: center;
  cursor: pointer;
  font-weight: 700;
}

.primary-button:hover {
  background-color: var(--primary-color-hover);
}

.primary-button:disabled {
  background-color: var(--primary-color-disabled);
  cursor: not-allowed;
}

.secondary-button {
  border: 1px solid var(--normal-border-color);
  background: transparent;
  color: var(--primary-color);
  padding: 8px 14px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 700;
}

.secondary-button:hover {
  background: rgba(0, 92, 70, 0.06);
}

.mail-notice {
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: center;
}

.notice-title {
  font-size: 17px;
  font-weight: 800;
  color: var(--primary-color);
}

.notice-copy {
  font-size: 14px;
  color: var(--text-color);
  word-break: break-all;
}

.error-message {
  color: #d93025;
  font-size: 14px;
}
</style>
