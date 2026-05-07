<template>
  <div class="milk-tea-activity">
    <div class="milk-tea-description">
      <div class="milk-tea-description-title">
        <info-icon />
        <span class="milk-tea-description-title-text">升级规则说明</span>
      </div>
      <div class="milk-tea-description-content">
        <p>回复帖子每次10exp，最多3次每天</p>
        <p>发布帖子每次30exp，最多1次每天</p>
        <p>发表情每次5exp，最多3次每天</p>
      </div>
    </div>
    <div class="milk-tea-status-container">
      <div class="milk-tea-status">
        <div class="status-title">🔥 已兑换奶茶人数</div>
        <ProgressBar :value="info.redeemCount" :max="50" />
        <div class="status-text">当前 {{ info.redeemCount }} / 50</div>
      </div>
      <div v-if="isLoadingUser" class="loading-user">
        <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
        <div class="user-level-text">加载当前等级中...</div>
      </div>
      <div v-else-if="user" class="user-level">
        <LevelProgress
          :exp="user.experience"
          :current-level="user.currentLevel"
          :next-exp="user.nextLevelExp"
        />
      </div>
      <div v-else class="user-level">
        <div class="user-level-text"><user-icon /> 请登录查看自身等级</div>
      </div>
    </div>
    <div
      v-if="user && user.currentLevel >= 1 && !info.ended"
      class="redeem-button"
      @click="openDialog"
    >
      兑换
    </div>
    <div v-else class="redeem-button disabled">兑换</div>
    <RedeemPopup
      :visible="dialogVisible"
      v-model="contact"
      :loading="loading"
      @close="closeDialog"
      @submit="submitRedeem"
    />
  </div>
</template>

<script setup>
import { toast } from '~/main'
import { fetchCurrentUser, getToken } from '~/utils/auth'
import LevelProgress from '~/components/LevelProgress.vue'
import ProgressBar from '~/components/ProgressBar.vue'
import RedeemPopup from '~/components/RedeemPopup.vue'
const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const info = ref({ redeemCount: 0, ended: false })
const user = ref(null)
const dialogVisible = ref(false)
const contact = ref('')
const loading = ref(false)
const isLoadingUser = ref(true)

onMounted(async () => {
  await loadInfo()
  isLoadingUser.value = true
  user.value = await fetchCurrentUser()
  isLoadingUser.value = false
})
const loadInfo = async () => {
  const res = await fetch(`${API_BASE_URL}/api/activities/milk-tea`)
  if (res.ok) {
    info.value = await res.json()
  }
}
const openDialog = () => {
  dialogVisible.value = true
}
const closeDialog = () => {
  dialogVisible.value = false
}
const submitRedeem = async () => {
  if (!contact.value) return
  loading.value = true
  const token = getToken()
  const res = await fetch(`${API_BASE_URL}/api/activities/milk-tea/redeem`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ contact: contact.value }),
  })
  if (res.ok) {
    const data = await res.json()
    if (data.message === 'updated') {
      toast.success('您已提交过兑换，本次更新兑换信息')
    } else {
      toast.success('兑换成功！')
    }
    dialogVisible.value = false
    await loadInfo()
  } else {
    toast.error('兑换失败')
  }
  loading.value = false
}
</script>

<style scoped>
.milk-tea-description-title-text {
  font-size: 14px;
  font-weight: bold;
  margin-left: 5px;
}

.milk-tea-description-content {
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

.milk-tea-activity {
  margin-top: 20px;
  padding: 20px;
}

.redeem-button {
  margin-top: 20px;
  background-color: var(--primary-color);
  color: #fff;
  padding: 8px 16px;
  border-radius: 10px;
  width: fit-content;
  cursor: pointer;
}

.redeem-button:hover {
  background-color: var(--primary-color-hover);
}

.redeem-button.disabled {
  background-color: var(--primary-color-disabled);
  cursor: not-allowed;
}

.redeem-button.disabled:hover {
  background-color: var(--primary-color-disabled);
}

.milk-tea-status-container {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 30px;
  margin-top: 20px;
}

.milk-tea-status {
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

@media screen and (max-width: 768px) {
  .milk-tea-status-container {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
