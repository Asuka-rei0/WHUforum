<template>
  <div class="forgot-page">
    <div class="forgot-content">
      <div class="forgot-title">找回密码</div>

      <div v-if="step === 0" class="step-content">
        <BaseInput icon="mail" v-model="email" placeholder="邮箱" />
        <div v-if="emailError" class="error-message">{{ emailError }}</div>
        <div class="primary-button" @click="sendCode" v-if="!isSending">发送验证码</div>
        <div class="primary-button disabled" v-else>发送中...</div>
      </div>
      <div v-else-if="step === 1" class="step-content">
        <BaseInput icon="mail" v-model="code" placeholder="邮箱验证码" />
        <div class="primary-button" @click="verifyCode" v-if="!isVerifying">验证</div>
        <div class="primary-button disabled" v-else>验证中...</div>
      </div>
      <div v-else class="step-content">
        <BaseInput icon="lock" v-model="password" type="password" placeholder="新密码" />
        <div v-if="passwordError" class="error-message">{{ passwordError }}</div>
        <div class="primary-button" @click="resetPassword" v-if="!isResetting">重置密码</div>
        <div class="primary-button disabled" v-else>提交中...</div>
      </div>
      <div class="hint-message">
        <info-icon />
        使用 Google 注册的用户可使用对应的邮箱进行找回密码
      </div>
    </div>
  </div>
</template>

<script setup>
import { toast } from '~/main'
import BaseInput from '~/components/BaseInput.vue'
import { useRoute } from 'vue-router'

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const step = ref(0)
const email = ref('')
const code = ref('')
const password = ref('')
const token = ref('')
const emailError = ref('')
const passwordError = ref('')
const isSending = ref(false)
const isVerifying = ref(false)
const isResetting = ref(false)
const route = useRoute()

onMounted(() => {
  if (route.query.email) {
    email.value = decodeURIComponent(route.query.email)
  }
})
const sendCode = async () => {
  if (!email.value) {
    emailError.value = '邮箱不能为空'
    return
  }
  try {
    isSending.value = true
    const res = await fetch(`${API_BASE_URL}/api/auth/forgot/send`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: email.value }),
    })
    isSending.value = false
    if (res.ok) {
      toast.success('验证码已发送')
      step.value = 1
    } else {
      toast.error('请填写已注册邮箱')
    }
  } catch (e) {
    isSending.value = false
    toast.error('发送失败')
  }
}
const verifyCode = async () => {
  try {
    isVerifying.value = true
    const res = await fetch(`${API_BASE_URL}/api/auth/forgot/verify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: email.value, code: code.value }),
    })
    isVerifying.value = false
    const data = await res.json()
    if (res.ok) {
      token.value = data.token
      step.value = 2
    } else {
      toast.error(data.error || '验证失败')
    }
  } catch (e) {
    isVerifying.value = false
    toast.error('验证失败')
  }
}
const resetPassword = async () => {
  if (!password.value) {
    passwordError.value = '密码不能为空'
    return
  }
  try {
    isResetting.value = true
    const res = await fetch(`${API_BASE_URL}/api/auth/forgot/reset`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token: token.value, password: password.value }),
    })
    isResetting.value = false
    const data = await res.json()
    if (res.ok) {
      toast.success('密码已重置')
      navigateTo('/login', { replace: true })
    } else if (data.field === 'password') {
      passwordError.value = data.error
    } else {
      toast.error(data.error || '重置失败')
    }
  } catch (e) {
    isResetting.value = false
    toast.error('重置失败')
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
}
.forgot-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 400px;
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
  background-color: var(--primary-color);
  color: white;
  padding: 10px 20px;
  border-radius: 10px;
  text-align: center;
  cursor: pointer;
}
.primary-button:hover {
  background-color: var(--primary-color-hover);
}
.primary-button.disabled {
  background-color: var(--primary-color-disabled);
  cursor: not-allowed;
}
.error-message {
  color: red;
  font-size: 14px;
}
@media (max-width: 768px) {
  .forgot-content {
    width: calc(100vw - 40px);
  }
}
</style>
