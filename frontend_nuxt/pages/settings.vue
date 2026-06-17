<template>
  <div class="settings-page">
    <AvatarCropper
      :src="tempAvatar"
      :show="showCropper"
      @close="showCropper = false"
      @crop="onCropped"
    />
    <div v-if="isLoadingPage" class="loading-page">
      <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
    </div>
    <div v-else>
      <div class="settings-title">个人资料设置</div>
      <div class="profile-section">
        <div class="avatar-row">
          <!-- label 充当点击区域，内部隐藏 input -->
          <label class="avatar-container">
            <BaseUserAvatar
              :src="avatar"
              :user-id="userId"
              alt="avatar"
              class="avatar-preview"
              :disable-link="true"
            />
            <!-- 半透明蒙层：hover 时出现 -->
            <div class="avatar-overlay">更换头像</div>
            <input type="file" class="avatar-input" accept="image/*" @change="onAvatarChange" />
          </label>
        </div>
        <div class="form-row username-row">
          <BaseInput
            icon="user-icon"
            v-model="username"
            @input="usernameError = ''"
            placeholder="用户名"
          />
          <div class="setting-description">用户名是你在社区的唯一标识</div>
          <div v-if="usernameError" class="error-message">{{ usernameError }}</div>
        </div>
        <div class="form-row introduction-row">
          <div class="setting-title">自我介绍</div>
          <BaseInput v-model="introduction" textarea rows="3" placeholder="说些什么..." />
          <div class="setting-description">自我介绍会出现在你的个人主页，可以简要介绍自己</div>
        </div>
        <div class="form-row switch-row">
          <div class="setting-title">毛玻璃效果</div>
          <BaseSwitch v-model="frosted" />
        </div>
      </div>
      <div v-if="role === 'ADMIN'" class="admin-section">
        <h3>管理员设置</h3>
        <div class="form-row dropdown-row">
          <div class="setting-title">发布规则</div>
          <Dropdown v-model="publishMode" :fetch-options="fetchPublishModes" />
        </div>
        <div class="form-row dropdown-row">
          <div class="setting-title">密码强度</div>
          <Dropdown v-model="passwordStrength" :fetch-options="fetchPasswordStrengths" />
        </div>
        <div class="form-row dropdown-row">
          <div class="setting-title">AI 优化次数</div>
          <Dropdown v-model="aiFormatLimit" :fetch-options="fetchAiLimits" />
        </div>
        <div class="form-row dropdown-row">
          <div class="setting-title">注册模式</div>
          <Dropdown v-model="registerMode" :fetch-options="fetchRegisterModes" />
        </div>
      </div>
      <div class="buttons">
        <div v-if="isSaving" class="save-button disabled">保存中...</div>
        <div v-else @click="save" class="save-button">保存</div>
      </div>
      <div class="danger-section">
        <div class="setting-title danger-title">账号注销</div>
        <div class="setting-description">
          注销后账号将无法继续登录，历史内容会保留为已注销用户。
        </div>
        <div class="delete-account-row">
          <BaseInput
            icon="lock"
            v-model="deletePassword"
            type="password"
            autocomplete="current-password"
            @input="deletePasswordError = ''"
            placeholder="输入当前密码确认注销"
          />
          <div v-if="deletePasswordError" class="error-message delete-error">
            {{ deletePasswordError }}
          </div>
          <button
            class="delete-button"
            type="button"
            :disabled="isDeletingAccount"
            @click="deleteAccount"
          >
            {{ isDeletingAccount ? '注销中...' : '注销账号' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import AvatarCropper from '~/components/AvatarCropper.vue'
import BaseInput from '~/components/BaseInput.vue'
import Dropdown from '~/components/Dropdown.vue'
import BaseSwitch from '~/components/BaseSwitch.vue'
import BaseUserAvatar from '~/components/BaseUserAvatar.vue'
import { toast } from '~/main'
import { getApiErrorMessage } from '~/utils/apiError'
import { fetchCurrentUser, getToken, setToken, clearToken } from '~/utils/auth'
import { frostedState, setFrosted } from '~/utils/frosted'
import { useConfirm } from '~/composables/useConfirm'
const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl
const { confirm } = useConfirm()
const username = ref('')
const introduction = ref('')
const usernameError = ref('')
const deletePassword = ref('')
const deletePasswordError = ref('')
const avatar = ref('')
const avatarFile = ref(null)
const tempAvatar = ref('')
const showCropper = ref(false)
const role = ref('')
const userId = ref(null)
const publishMode = ref('DIRECT')
const passwordStrength = ref('LOW')
const aiFormatLimit = ref(3)
const registerMode = ref('DIRECT')
const isLoadingPage = ref(false)
const isSaving = ref(false)
const isDeletingAccount = ref(false)
const frosted = ref(true)

onMounted(async () => {
  isLoadingPage.value = true
  const user = await fetchCurrentUser()

  if (user) {
    username.value = user.username
    introduction.value = user.introduction || ''
    avatar.value = user.avatar
    userId.value = user.id
    role.value = user.role
    if (role.value === 'ADMIN') {
      loadAdminConfig()
    }
  } else {
    toast.error('请先登录')
    navigateTo('/login', { replace: true })
  }
  isLoadingPage.value = false
  frosted.value = frostedState.enabled
})

const onAvatarChange = (e) => {
  const file = e.target.files[0]
  if (file) {
    const reader = new FileReader()
    reader.onload = () => {
      tempAvatar.value = reader.result
      showCropper.value = true
    }
    reader.readAsDataURL(file)
  }
}
watch(frosted, (val) => setFrosted(val))
const onCropped = ({ file, url }) => {
  avatarFile.value = file
  avatar.value = url
}
const fetchPublishModes = () => {
  return Promise.resolve([
    { id: 'DIRECT', name: '直接发布', icon: 'send-icon' },
    { id: 'REVIEW', name: '审核后发布', icon: 'search-icon' },
  ])
}
const fetchPasswordStrengths = () => {
  return Promise.resolve([
    { id: 'LOW', name: '低', icon: 'unlock' },
    { id: 'MEDIUM', name: '中', icon: 'lock-one' },
    { id: 'HIGH', name: '高', icon: 'lock' },
  ])
}
const fetchAiLimits = () => {
  return Promise.resolve([
    { id: 3, name: '3次' },
    { id: 5, name: '5次' },
    { id: 10, name: '10次' },
    { id: -1, name: '无限' },
  ])
}
const fetchRegisterModes = () => {
  return Promise.resolve([
    { id: 'DIRECT', name: '直接注册', icon: 'send-icon' },
    { id: 'WHITELIST', name: '白名单审核制', icon: 'search-icon' },
  ])
}
const loadAdminConfig = async () => {
  try {
    const token = getToken()
    const res = await fetch(`${API_BASE_URL}/api/admin/config`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    if (res.ok) {
      const data = await res.json()
      publishMode.value = data.publishMode
      passwordStrength.value = data.passwordStrength
      aiFormatLimit.value = data.aiFormatLimit
      registerMode.value = data.registerMode
    }
  } catch (e) {
    // ignore
  }
}
const save = async () => {
  isSaving.value = true

  do {
    let token = getToken()
    usernameError.value = ''
    if (!username.value) {
      usernameError.value = '用户名不能为空'
    }
    if (usernameError.value) {
      toast.error(usernameError.value)
      break
    }
    if (avatarFile.value) {
      const form = new FormData()
      form.append('file', avatarFile.value)
      const res = await fetch(`${API_BASE_URL}/api/users/me/avatar`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: form,
      })
      const data = await res.json()
      if (res.ok) {
        avatar.value = data.url
      } else {
        toast.error(getApiErrorMessage(data, '头像上传失败，请稍后重试'))
        break
      }
    }
    const res = await fetch(`${API_BASE_URL}/api/users/me`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify({ username: username.value, introduction: introduction.value }),
    })

    const data = await res.json()
    if (!res.ok) {
      toast.error(getApiErrorMessage(data, '资料保存失败，请稍后重试'))
      break
    }
    if (data.token) {
      setToken(data.token)
      token = data.token
    }
    if (role.value === 'ADMIN') {
      await fetch(`${API_BASE_URL}/api/admin/config`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({
          publishMode: publishMode.value,
          passwordStrength: passwordStrength.value,
          aiFormatLimit: aiFormatLimit.value,
          registerMode: registerMode.value,
        }),
      })
    }
    toast.success('保存成功')
  } while (!isSaving.value)

  isSaving.value = false
}

const deleteAccount = async () => {
  deletePasswordError.value = ''
  if (!deletePassword.value) {
    deletePasswordError.value = '请输入当前密码'
    return
  }

  const ok = await confirm(
    '注销账号',
    '注销后账号将无法继续登录，历史内容会保留为已注销用户。确认继续吗？',
  )
  if (!ok) return

  try {
    isDeletingAccount.value = true
    const token = getToken()
    const res = await fetch(`${API_BASE_URL}/api/users/me`, {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify({ password: deletePassword.value }),
    })
    const data = await res.json().catch(() => ({}))
    if (res.ok) {
      clearToken()
      toast.success('账号已注销')
      await navigateTo('/login', { replace: true })
      return
    }
    if (data.field === 'password' || data.reason_code === 'INVALID_PASSWORD') {
      deletePasswordError.value = getApiErrorMessage(data, '当前密码不正确')
      return
    }
    toast.error(getApiErrorMessage(data, '注销失败，请稍后重试'))
  } catch (e) {
    toast.error('注销失败，请稍后重试')
  } finally {
    isDeletingAccount.value = false
  }
}
</script>

<style scoped>
.loading-page {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 300px;
}

.settings-page {
  background-color: var(--background-color);
  padding: 40px;
  height: calc(100% - 80px);
  overflow-y: auto;
}

.settings-title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 20px;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
}

.avatar-preview {
  width: 80px;
  height: 80px;
  border-radius: 40px;
}

.avatar-preview :deep(.base-user-avatar-img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-row {
  margin-bottom: 15px;
  display: flex;
  flex-direction: column;
}

.username-row {
  max-width: 300px;
}

.admin-section {
  margin-top: 30px;
  padding-top: 10px;
}

.setting-title {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 5px;
}

.setting-description {
  font-size: 12px;
  color: #666;
  margin-top: 5px;
}

.introduction-row {
  max-width: 500px;
}

.dropdown-row {
  max-width: 200px;
}

.switch-row {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  max-width: 200px;
}

.profile-section {
  margin-bottom: 30px;
}

.danger-section {
  max-width: 500px;
  padding-top: 18px;
  border-top: 1px solid var(--normal-border-color);
  margin-top: 30px;
  margin-bottom: 30px;
}

.danger-title {
  color: #b42318;
}

.delete-account-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 360px;
  margin-top: 12px;
}

.delete-button {
  border: none;
  background: #b42318;
  color: #fff;
  padding: 10px 18px;
  min-height: 40px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 700;
  align-self: flex-start;
}

.delete-button:hover {
  background: #8f1d14;
}

.delete-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.delete-error {
  margin: 0;
  width: 100%;
}

.buttons {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}

.avatar-container {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 40px;
  cursor: pointer;
}

/* 隐藏默认文件选择按钮 */
.avatar-input {
  display: none;
}

/* 蒙层：初始透明，hover 时渐显 */
.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 40px;
  background: rgba(0, 0, 0, 0.4);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease-in-out;
}

/* hover 触发 */
.avatar-container:hover .avatar-overlay {
  opacity: 1;
}

.error-message {
  color: red;
  font-size: 14px;
  width: calc(100% - 40px);
  margin-top: -10px;
  margin-bottom: 10px;
}

.save-button {
  margin-top: 40px;
  background-color: var(--primary-color);
  color: white;
  padding: 10px 20px;
  font-size: 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 0.2s ease-in-out;
}

.save-button:hover {
  background-color: var(--primary-color-hover);
}

.save-button.disabled:hover {
  background-color: var(--primary-color-disabled);
  cursor: not-allowed;
}

.save-button.disabled {
  background-color: var(--primary-color-disabled);
  cursor: not-allowed;
}
</style>
