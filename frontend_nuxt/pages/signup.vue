<template>
  <div class="signup-page">
    <div class="auth-panel">
      <img class="whu-emblem" src="/whu-emblem.webp" alt="武汉大学校园论坛" />
      <div class="auth-title">加入珞珈论坛</div>
      <div class="auth-subtitle">仅支持 @whu.edu.cn 邮箱注册</div>

      <form v-if="step === 'form'" class="auth-form" @submit.prevent="submitSignup">
        <div class="field-group">
          <label class="field-label" for="signup-username">用户名</label>
          <BaseInput
            id="signup-username"
            icon="user"
            v-model="username"
            autocomplete="username"
            placeholder="设置论坛昵称"
          />
          <div v-if="usernameError" class="error-message">{{ usernameError }}</div>
        </div>

        <div class="field-group">
          <label class="field-label" for="signup-email">武汉大学邮箱</label>
          <BaseInput
            id="signup-email"
            icon="mail"
            v-model="email"
            type="email"
            autocomplete="email"
            placeholder="name@whu.edu.cn"
          />
          <div v-if="emailError" class="error-message">{{ emailError }}</div>
        </div>

        <div class="field-group">
          <label class="field-label" for="signup-password">密码</label>
          <BaseInput
            id="signup-password"
            icon="lock"
            v-model="password"
            type="password"
            autocomplete="new-password"
            placeholder="设置登录密码"
          />
          <div v-if="passwordError" class="error-message">{{ passwordError }}</div>
        </div>

        <div class="field-group">
          <label class="field-label" for="signup-confirm-password">确认密码</label>
          <BaseInput
            id="signup-confirm-password"
            icon="lock"
            v-model="confirmPassword"
            type="password"
            autocomplete="new-password"
            placeholder="再次输入密码"
          />
          <div v-if="confirmPasswordError" class="error-message">
            {{ confirmPasswordError }}
          </div>
        </div>

        <button class="primary-button" type="submit" :disabled="isSubmitting">
          <loading-four v-if="isSubmitting" class="button-icon" />
          <span>{{ isSubmitting ? '注册中...' : '注册并发送激活邮件' }}</span>
        </button>
      </form>

      <div v-else class="auth-form">
        <div class="mail-notice">
          <div class="notice-icon-wrap" aria-hidden="true">
            <CheckCorrect class="notice-icon" size="24" />
          </div>
          <div class="notice-title">激活邮件已发送</div>
          <div class="verify-copy">{{ registeredEmail }}</div>
          <div class="notice-copy">请点击邮件中的按钮完成注册。</div>
        </div>
        <button class="secondary-button edit-button" type="button" @click="step = 'form'">
          <Edit class="secondary-button-icon" size="18" />
          <span>修改信息</span>
        </button>
      </div>

      <div class="auth-links">
        <span>已经有账号？</span>
        <NuxtLink to="/login">登录</NuxtLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import BaseInput from '~/components/BaseInput.vue'
import { toast } from '~/main'
import {
  getApiErrorMessage,
  isDuplicateEmailError,
  isDuplicateUsernameError,
} from '~/utils/apiError'
import { setToken } from '~/utils/auth'
import { registerPush } from '~/utils/push'

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl
const route = useRoute()

const step = ref('form')
const username = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const registeredEmail = ref('')
const usernameError = ref('')
const emailError = ref('')
const passwordError = ref('')
const confirmPasswordError = ref('')
const isSubmitting = ref(false)

const inviteToken = computed(() => {
  const raw = route.query.invite_token
  return Array.isArray(raw) ? raw[0] || '' : raw || ''
})

const normalizeEmail = (value) => (value || '').trim().toLowerCase()
const isWhuEmail = (value) => normalizeEmail(value).endsWith('@whu.edu.cn')

const clearErrors = () => {
  usernameError.value = ''
  emailError.value = ''
  passwordError.value = ''
  confirmPasswordError.value = ''
}

const completeLogin = async (token) => {
  await setToken(token)
  registerPush()
  toast.success('注册成功')
  await navigateTo('/', { replace: true })
}

const submitSignup = async () => {
  clearErrors()
  const normalizedEmail = normalizeEmail(email.value)
  const trimmedUsername = username.value.trim()

  if (!trimmedUsername) {
    usernameError.value = '请输入用户名'
    return
  }
  if (!isWhuEmail(normalizedEmail)) {
    emailError.value = '请使用 @whu.edu.cn 邮箱注册'
    return
  }
  if (!password.value) {
    passwordError.value = '请输入密码'
    return
  }
  if (password.value !== confirmPassword.value) {
    confirmPasswordError.value = '两次输入的密码不一致'
    return
  }

  try {
    isSubmitting.value = true
    const res = await fetch(`${API_BASE_URL}/api/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: trimmedUsername,
        email: normalizedEmail,
        password: password.value,
        inviteToken: inviteToken.value || undefined,
      }),
    })
    const data = await res.json().catch(() => ({}))
    if (res.ok && data.token) {
      await completeLogin(data.token)
      return
    }
    if (res.ok) {
      registeredEmail.value = data.email || normalizedEmail
      step.value = 'emailSent'
      toast.success('激活邮件已发送，请查收武汉大学邮箱')
      return
    }
    if (data.field === 'username' || isDuplicateUsernameError(data)) {
      usernameError.value = getApiErrorMessage(data, '用户名不可用')
      return
    }
    if (
      data.field === 'email' ||
      data.reason_code === 'WHU_EMAIL_REQUIRED' ||
      isDuplicateEmailError(data)
    ) {
      emailError.value = getApiErrorMessage(data, '请使用 @whu.edu.cn 邮箱注册')
      return
    }
    if (data.field === 'password') {
      passwordError.value = getApiErrorMessage(data, '密码不符合要求')
      return
    }
    toast.error(getApiErrorMessage(data, '注册失败，请稍后重试'))
  } catch (e) {
    toast.error('注册失败，请稍后重试')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.signup-page {
  --signup-green: #00664f;
  --signup-green-deep: #003f32;
  --signup-cyan: #0a6e78;
  --signup-mint: #e8f7f1;
  --signup-sky: #e7f4ff;

  min-height: 100%;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    linear-gradient(
      180deg,
      rgba(0, 92, 70, 0.11) 0%,
      rgba(0, 92, 70, 0.035) 44%,
      rgba(10, 110, 120, 0.055) 100%
    ),
    var(--background-color);
  padding: 32px 20px;
  box-sizing: border-box;
}

.auth-panel {
  width: min(100%, 440px);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  text-align: center;
}

.whu-emblem {
  width: 104px;
  height: 104px;
  object-fit: contain;
  filter: drop-shadow(0 12px 18px rgba(0, 76, 58, 0.14));
}

.auth-title {
  font-size: 34px;
  line-height: 1.18;
  font-weight: 800;
  color: var(--signup-green);
}

.auth-subtitle {
  font-size: 15px;
  color: var(--text-color);
  opacity: 0.68;
}

.verify-copy {
  font-size: 17px;
  font-weight: 700;
  color: var(--signup-cyan);
  opacity: 0.86;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.mail-notice {
  position: relative;
  overflow: hidden;
  width: 100%;
  border: 1px solid rgba(0, 92, 70, 0.2);
  border-radius: 8px;
  padding: 24px 20px 22px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  box-sizing: border-box;
  text-align: center;
  background: rgba(250, 253, 252, 0.9);
  background: color-mix(in srgb, var(--background-color) 88%, var(--signup-mint));
  box-shadow: 0 18px 38px rgba(0, 76, 58, 0.1);
}

.mail-notice::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 3px;
  background: linear-gradient(90deg, var(--signup-green), var(--signup-cyan), #8cc6a3);
}

.notice-icon-wrap {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: var(--signup-green);
  background: rgba(0, 102, 79, 0.1);
  box-shadow: inset 0 0 0 1px rgba(0, 102, 79, 0.12);
}

.notice-icon {
  display: block;
}

.notice-title {
  font-size: 18px;
  line-height: 1.35;
  font-weight: 800;
  color: var(--signup-green);
}

.notice-copy {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-color);
  opacity: 0.86;
}

.auth-form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 8px;
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
  min-height: 48px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-sizing: border-box;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease,
    background 0.2s ease,
    color 0.2s ease;
}

.primary-button {
  border: none;
  background: linear-gradient(135deg, var(--signup-green), var(--signup-cyan));
  color: #fff;
  box-shadow: 0 12px 24px rgba(0, 92, 70, 0.16);
}

.primary-button:hover:not(:disabled) {
  transform: translateY(-1px);
  background: linear-gradient(135deg, var(--signup-green-deep), #075d68);
  box-shadow: 0 16px 30px rgba(0, 92, 70, 0.2);
}

.primary-button:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 8px 18px rgba(0, 92, 70, 0.16);
}

.primary-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  box-shadow: none;
}

.secondary-button {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(0, 92, 70, 0.22);
  background: rgba(255, 255, 255, 0.68);
  background: color-mix(in srgb, var(--background-color) 86%, var(--signup-sky));
  color: var(--signup-green);
  box-shadow: 0 10px 24px rgba(0, 76, 58, 0.07);
}

.edit-button::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    120deg,
    transparent 0%,
    transparent 36%,
    rgba(255, 255, 255, 0.55) 48%,
    transparent 60%,
    transparent 100%
  );
  transform: translateX(-120%);
  transition: transform 0.55s ease;
  pointer-events: none;
}

.edit-button:hover {
  transform: translateY(-2px);
  border-color: rgba(0, 102, 79, 0.45);
  background: linear-gradient(135deg, rgba(232, 247, 241, 0.92), rgba(231, 244, 255, 0.92));
  color: var(--signup-green-deep);
  box-shadow: 0 16px 32px rgba(0, 76, 58, 0.14);
}

.edit-button:hover::before {
  transform: translateX(120%);
}

.edit-button:active {
  transform: translateY(0);
  box-shadow: 0 8px 18px rgba(0, 76, 58, 0.1);
}

.edit-button:focus-visible {
  outline: 3px solid rgba(10, 110, 120, 0.22);
  outline-offset: 3px;
}

.secondary-button-icon {
  position: relative;
  z-index: 1;
  flex: 0 0 auto;
  transition: transform 0.2s ease;
}

.edit-button span {
  position: relative;
  z-index: 1;
}

.edit-button:hover .secondary-button-icon {
  transform: rotate(-6deg) scale(1.08);
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
  .signup-page {
    align-items: flex-start;
    padding: 64px 18px 32px;
  }

  .auth-title {
    font-size: 32px;
  }

  .mail-notice {
    padding: 22px 18px 20px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .primary-button,
  .secondary-button,
  .secondary-button-icon,
  .edit-button::before {
    transition: none;
  }

  .primary-button:hover:not(:disabled),
  .edit-button:hover,
  .edit-button:active,
  .edit-button:hover .secondary-button-icon {
    transform: none;
  }

  .edit-button:hover::before {
    transform: translateX(-120%);
  }
}
</style>
