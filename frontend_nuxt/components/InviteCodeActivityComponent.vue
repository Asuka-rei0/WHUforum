<template>
  <div class="invite-code-activity">
    <div class="invite-code-description">
      <div class="invite-code-description-title">
        <info-icon />
        <span class="invite-code-description-title-text">邀请规则说明</span>
      </div>
      <div class="invite-code-description-content">
        <p>⚠️邀请好友注册并登录，每次可以获得500积分🎉🎉🎉</p>
        <p>邀请链接的有效期为1个月</p>
        <p>每一个邀请链接的邀请人数上限为3人</p>
        <p>通过邀请链接注册，无需注册审核</p>
        <p>每人每天仅能生产1个邀请链接</p>
      </div>
    </div>

    <div v-if="inviteLink" class="invite-code-link-content">
      <p class="invite-code-link-content-text">
        邀请链接：{{ inviteLink }}
        <span @click="copyLink"><copy class="copy-icon" /></span>
      </p>
    </div>

    <div :class="['generate-button', { disabled: !user || loadingInvite }]" @click="generateInvite">
      生成邀请链接
    </div>
  </div>
</template>

<script setup>
import { toast } from '~/main'
import { fetchCurrentUser, getToken } from '~/utils/auth'

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl
const WEBSITE_BASE_URL = config.public.websiteBaseUrl

const user = ref(null)
const isLoadingUser = ref(true)
const inviteCode = ref('')
const loadingInvite = ref(false)

const inviteLink = computed(() =>
  inviteCode.value ? `${WEBSITE_BASE_URL}/signup?invite_token=${inviteCode.value}` : '',
)

onMounted(async () => {
  isLoadingUser.value = true
  user.value = await fetchCurrentUser()
  isLoadingUser.value = false
  // if (user.value) {
  //   await fetchInvite(false)
  // }
})

const fetchInvite = async (showToast = true) => {
  loadingInvite.value = true
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    loadingInvite.value = false
    return
  }
  try {
    const res = await fetch(`${API_BASE_URL}/api/invite/generate`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    })
    if (res.ok) {
      const data = await res.json()
      inviteCode.value = data.token
      if (showToast) toast.success('邀请链接已生成')
    } else {
      const data = await res.json().catch(() => ({}))
      toast.error(data.error || '生成邀请链接失败')
    }
  } catch (e) {
    toast.error('生成邀请链接失败')
  } finally {
    loadingInvite.value = false
  }
}

const generateInvite = () => fetchInvite(true)

const copyLink = async () => {
  if (!inviteLink.value) return
  try {
    await navigator.clipboard.writeText(inviteLink.value)
    toast.success('已复制')
  } catch (e) {
    toast.error('复制失败')
  }
}
</script>

<style scoped>
.invite-code-description-title-text {
  font-size: 14px;
  font-weight: bold;
  margin-left: 5px;
}

.invite-code-description-content {
  font-size: 12px;
  opacity: 0.8;
}

.status-title {
  font-weight: bold;
}

.status-text {
  font-size: 12px;
  opacity: 0.8;
}

.invite-code-activity {
  margin-top: 20px;
  padding: 20px;
}

.generate-button {
  margin-top: 20px;
  background-color: var(--primary-color);
  color: #fff;
  padding: 8px 16px;
  border-radius: 10px;
  width: fit-content;
  cursor: pointer;
}

.generate-button:hover {
  background-color: var(--primary-color-hover);
}

.generate-button.disabled {
  background-color: var(--primary-color-disabled);
  cursor: not-allowed;
}

.generate-button.disabled:hover {
  background-color: var(--primary-color-disabled);
}

.invite-code-status-container {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 30px;
  margin-top: 20px;
}

.invite-code-status {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 10px;
  font-size: 14px;
}

.user-level-text {
  opacity: 0.8;
  font-size: 12px;
  color: var(--primary-color);
}

.invite-code-link-content {
  margin-top: 20px;
  font-size: 12px;
  opacity: 0.8;
}

.invite-code-link-content-text {
  word-break: break-all;
}

.copy-icon {
  cursor: pointer;
  margin-left: 5px;
}

@media screen and (max-width: 768px) {
  .invite-code-status-container {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
