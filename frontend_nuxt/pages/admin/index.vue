<template>
  <div class="admin-dashboard">
    <header class="dashboard-header">
      <div>
        <div class="dashboard-kicker">管理员后台</div>
        <h1>管理首页</h1>
        <p>待办概览与常用入口。数据来自实时统计接口。</p>
      </div>
      <button class="text-button" type="button" :disabled="loading" @click="loadStats">
        刷新
      </button>
    </header>

    <div v-if="loading && !stats" class="loading-block">
      <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
    </div>

    <template v-else-if="stats">
      <section class="stats-grid" aria-label="待办统计">
        <NuxtLink class="stat-card urgent" to="/admin/reports?status=OPEN">
          <span class="stat-label">待处理举报</span>
          <strong class="stat-value">{{ stats.openReports }}</strong>
        </NuxtLink>
        <NuxtLink class="stat-card" to="/admin/reports?status=REVIEWING">
          <span class="stat-label">处理中举报</span>
          <strong class="stat-value">{{ stats.reviewingReports }}</strong>
        </NuxtLink>
        <NuxtLink class="stat-card" to="/admin/treeholes">
          <span class="stat-label">待复审树洞</span>
          <strong class="stat-value">{{ stats.openTreeholeCases }}</strong>
        </NuxtLink>
        <NuxtLink class="stat-card" to="/admin/posts/pending">
          <span class="stat-label">待审核帖子</span>
          <strong class="stat-value">{{ stats.pendingPosts }}</strong>
        </NuxtLink>
      </section>

      <section class="summary-grid" aria-label="站点概览">
        <div class="summary-card">
          <span>注册用户</span>
          <strong>{{ stats.users }}</strong>
        </div>
        <div class="summary-card">
          <span>帖子总数</span>
          <strong>{{ stats.posts }}</strong>
        </div>
        <div class="summary-card">
          <span>评论总数</span>
          <strong>{{ stats.comments }}</strong>
        </div>
      </section>

      <section class="quick-links" aria-label="快捷入口">
        <h2>快捷入口</h2>
        <div class="link-grid">
          <NuxtLink class="quick-link" to="/admin/reports">内容举报</NuxtLink>
          <NuxtLink class="quick-link" to="/admin/treeholes">树洞复审</NuxtLink>
          <NuxtLink class="quick-link" to="/admin/posts/pending">待审核帖子</NuxtLink>
          <NuxtLink class="quick-link" to="/admin/sensitive-words">敏感词管理</NuxtLink>
          <NuxtLink class="quick-link" to="/about/stats">站点统计</NuxtLink>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useToast } from 'vue-toastification'
import { getToken } from '~/utils/auth'
import { getApiErrorMessage } from '~/utils/apiError'

definePageMeta({ middleware: ['admin-required'] })

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl
const toast = useToast()

const stats = ref(null)
const loading = ref(false)

const authHeaders = () => {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const loadStats = async () => {
  loading.value = true
  try {
    const res = await fetch(`${API_BASE_URL}/api/stats/admin-dashboard`, {
      headers: authHeaders(),
    })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) throw new Error(getApiErrorMessage(data, '加载统计数据失败'))
    stats.value = data
  } catch (e) {
    toast.error(e.message || '加载统计数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<style scoped>
.admin-dashboard {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 18px 48px;
}

.dashboard-header {
  align-items: center;
  border-bottom: 1px solid var(--normal-border-color);
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 16px;
}

.dashboard-kicker {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: 700;
}

.dashboard-header h1 {
  font-size: 26px;
  margin: 6px 0 0;
}

.dashboard-header p {
  color: var(--menu-text-color);
  font-size: 14px;
  line-height: 1.6;
  margin: 8px 0 0;
}

.text-button {
  background: var(--background-color);
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  color: var(--text-color);
  cursor: pointer;
  min-height: 36px;
  padding: 0 12px;
}

.stats-grid,
.summary-grid,
.link-grid {
  display: grid;
  gap: 12px;
}

.stats-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  margin-bottom: 16px;
}

.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  margin-bottom: 24px;
}

.stat-card,
.summary-card,
.quick-link {
  border: 1px solid var(--normal-border-color);
  border-radius: 10px;
  background: var(--background-color);
}

.stat-card {
  color: inherit;
  display: grid;
  gap: 8px;
  padding: 16px;
  text-decoration: none;
  transition:
    border-color 0.15s ease,
    transform 0.15s ease;
}

.stat-card:not(.muted):hover {
  border-color: var(--primary-color);
  transform: translateY(-1px);
}

.stat-card.urgent {
  border-color: color-mix(in srgb, #b42318 35%, var(--normal-border-color));
}

.stat-card.muted {
  cursor: default;
  opacity: 0.92;
}

.stat-label,
.summary-card span {
  color: var(--menu-text-color);
  font-size: 13px;
}

.stat-value,
.summary-card strong {
  font-size: 28px;
  line-height: 1.1;
}

.summary-card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
}

.quick-links h2 {
  font-size: 18px;
  margin: 0 0 12px;
}

.link-grid {
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
}

.quick-link {
  color: var(--primary-color);
  font-weight: 700;
  padding: 14px 16px;
  text-decoration: none;
}

.quick-link:hover {
  background: var(--normal-light-background-color);
}

.loading-block {
  align-items: center;
  display: flex;
  justify-content: center;
  min-height: 220px;
}
</style>
