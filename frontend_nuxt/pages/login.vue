<template>
  <div class="login-page">
    <div class="auth-panel">
      <img class="whu-emblem" src="/whu-emblem.webp" alt="武汉大学校园论坛" />
      <div class="auth-title">珞珈论坛</div>
      <div class="auth-subtitle">武汉大学校园论坛</div>

      <form v-if="mode === 'login'" class="auth-form" @submit.prevent="submitLogin">
        <div class="field-group">
          <label class="field-label" for="login-email">邮箱</label>
          <BaseInput
            id="login-email"
            icon="mail"
            v-model="email"
            type="email"
            autocomplete="email"
            placeholder="name@whu.edu.cn"
          />
          <div v-if="emailError" class="error-message">{{ emailError }}</div>
        </div>

        <div class="field-group">
          <label class="field-label" for="login-password">密码</label>
          <BaseInput
            id="login-password"
            icon="lock"
            v-model="password"
            type="password"
            autocomplete="current-password"
            placeholder="请输入密码"
          />
          <div v-if="passwordError" class="error-message">{{ passwordError }}</div>
        </div>

        <button class="primary-button" type="submit" :disabled="isSubmitting">
          <loading-four v-if="isSubmitting" class="button-icon" />
          <span>{{ isSubmitting ? '登录中...' : '邮箱登录' }}</span>
        </button>
      </form>

      <div v-else class="auth-form">
        <div class="mail-notice">
          <div class="notice-title">激活邮件已重新发送</div>
          <div class="verify-copy">{{ pendingVerifyEmail }}</div>
          <div class="notice-copy">请点击邮件中的按钮完成邮箱验证。</div>
        </div>
        <button class="secondary-button" type="button" @click="mode = 'login'">返回登录</button>
      </div>

      <div class="auth-links">
        <NuxtLink to="/forgot-password">忘记密码</NuxtLink>
        <span>还没有账号？</span>
        <NuxtLink to="/signup">注册</NuxtLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import BaseInput from '~/components/BaseInput.vue'
import { toast } from '~/main'
import { getApiErrorMessage } from '~/utils/apiError'
import { setToken } from '~/utils/auth'
import { registerPush } from '~/utils/push'

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl
const route = useRoute()

const mode = ref('login')
const email = ref('')
const password = ref('')
const pendingVerifyEmail = ref('')
const emailError = ref('')
const passwordError = ref('')
const isSubmitting = ref(false)

const normalizeEmail = (value) => (value || '').trim().toLowerCase()
const isWhuEmail = (value) => normalizeEmail(value).endsWith('@whu.edu.cn')

const clearErrors = () => {
  emailError.value = ''
  passwordError.value = ''
}

const redirectAfterLogin = () => {
  const target = Array.isArray(route.query.redirect)
    ? route.query.redirect[0]
    : route.query.redirect
  return typeof target === 'string' && target.startsWith('/') && !target.startsWith('//')
    ? target
    : '/'
}

const completeLogin = async (token) => {
  await setToken(token)
  registerPush()
  toast.success('登录成功')
  await navigateTo(redirectAfterLogin(), { replace: true })
}

const submitLogin = async () => {
  clearErrors()
  const loginEmail = normalizeEmail(email.value)
  if (!isWhuEmail(loginEmail)) {
    emailError.value = '请使用 @whu.edu.cn 邮箱登录'
    return
  }
  if (!password.value) {
    passwordError.value = '请输入密码'
    return
  }

  try {
    isSubmitting.value = true
    const res = await fetch(`${API_BASE_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: loginEmail,
        password: password.value,
      }),
    })
    const data = await res.json().catch(() => ({}))
    if (res.ok && data.token) {
      await completeLogin(data.token)
      return
    }
    if (data.reason_code === 'NOT_VERIFIED') {
      pendingVerifyEmail.value = loginEmail
      mode.value = 'activationSent'
      toast.error('账号尚未完成邮箱验证，激活邮件已重新发送')
      return
    }
    if (data.reason_code === 'NOT_APPROVED' && data.token) {
      await navigateTo(`/signup-reason?token=${data.token}`, { replace: true })
      return
    }
    if (data.reason_code === 'IS_APPROVING') {
      toast.error('账号正在审核中')
      return
    }
    if (data.field === 'email' || data.reason_code === 'WHU_EMAIL_REQUIRED') {
      emailError.value = getApiErrorMessage(data, '请使用 @whu.edu.cn 邮箱登录')
      return
    }
    if (data.field === 'password' || data.reason_code === 'INVALID_PASSWORD') {
      passwordError.value = getApiErrorMessage(data, '密码不正确')
      return
    }
    toast.error(getApiErrorMessage(data, '邮箱或密码不正确'))
  } catch (e) {
    toast.error('登录失败，请稍后重试')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.login-page {
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

.auth-subtitle,
.verify-copy {
  font-size: 15px;
  color: var(--text-color);
  opacity: 0.72;
  text-align: center;
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

.button-icon {
  font-size: 18px;
}

.error-message {
  color: #d93025;
  font-size: 13px;
}

.auth-links {
  width: 100%;
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 14px;
  color: var(--text-color);
  opacity: 0.78;
}

.auth-links a {
  color: var(--primary-color);
  font-weight: 700;
  text-decoration: none;
}

.auth-links a:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .login-page {
    align-items: flex-start;
    padding-top: 72px;
  }
}
</style>
