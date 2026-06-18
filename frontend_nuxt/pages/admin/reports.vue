<template>
  <div class="reports-page">
    <header class="reports-header">
      <div>
        <div class="reports-kicker">管理员后台</div>
        <h1>内容举报</h1>
      </div>
      <button class="text-button" type="button" :disabled="loading" @click="loadReports">
        刷新
      </button>
    </header>

    <div v-if="!isAdmin" class="access-denied">仅管理员可以查看举报队列</div>

    <template v-else>
      <section class="filter-panel">
        <label>
          <span>状态</span>
          <select v-model="status">
            <option value="">全部</option>
            <option value="OPEN">待处理</option>
            <option value="REVIEWING">处理中</option>
            <option value="RESOLVED">已处理</option>
            <option value="DISMISSED">已驳回</option>
          </select>
        </label>
        <span>{{ reports.length }} 条</span>
      </section>

      <section class="report-list">
        <div v-if="loading" class="loading-block">
          <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
        </div>
        <BasePlaceholder v-else-if="reports.length === 0" text="暂无举报" />
        <template v-else>
          <article v-for="report in reports" :key="report.id" class="report-item">
            <div class="report-topline">
              <span class="status-pill" :class="statusClass(report.status)">
                {{ statusText(report.status) }}
              </span>
              <span>{{ report.targetType }} #{{ report.targetId }}</span>
              <time>{{ formatTime(report.createdAt) }}</time>
            </div>
            <h2>{{ report.targetTitle || '未命名内容' }}</h2>
            <p>{{ report.targetExcerpt || '暂无摘要' }}</p>
            <div class="report-meta">
              <span>举报人：{{ report.reporterUsername || '未知' }}</span>
              <span>原因：{{ report.reason }}</span>
            </div>
            <textarea v-model="resolutionById[report.id]" placeholder="处理备注"></textarea>
            <div class="report-actions">
              <button type="button" @click="handleReport(report, 'REVIEWING')">标记处理中</button>
              <button type="button" class="positive" @click="handleReport(report, 'RESOLVED')">
                标记已处理
              </button>
              <button type="button" class="danger" @click="handleReport(report, 'DISMISSED')">
                驳回举报
              </button>
            </div>
          </article>
        </template>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useToast } from 'vue-toastification'
import BasePlaceholder from '~/components/BasePlaceholder.vue'
import { authState, getToken, loadCurrentUser } from '~/utils/auth'
import { getApiErrorMessage } from '~/utils/apiError'

definePageMeta({ middleware: ['auth-required'] })

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl
const toast = useToast()

const reports = ref([])
const status = ref('OPEN')
const loading = ref(false)
const resolutionById = ref({})
const isAdmin = computed(() => authState.role === 'ADMIN')

const authHeaders = () => {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const loadReports = async () => {
  if (!isAdmin.value || loading.value) return
  loading.value = true
  try {
    const url = new URL(`${API_BASE_URL}/api/admin/reports`)
    url.searchParams.set('pageSize', '50')
    if (status.value) url.searchParams.set('status', status.value)
    const res = await fetch(url, { headers: authHeaders() })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) throw new Error(getApiErrorMessage(data, '加载举报失败'))
    reports.value = data
    resolutionById.value = Object.fromEntries(
      data.map((item) => [item.id, item.resolution || '']),
    )
  } catch (e) {
    toast.error(e.message || '加载举报失败')
  } finally {
    loading.value = false
  }
}

const handleReport = async (report, nextStatus) => {
  const res = await fetch(`${API_BASE_URL}/api/admin/reports/${report.id}/actions`, {
    method: 'POST',
    headers: { ...authHeaders(), 'Content-Type': 'application/json' },
    body: JSON.stringify({ status: nextStatus, resolution: resolutionById.value[report.id] || '' }),
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    toast.error(getApiErrorMessage(data, '处理举报失败'))
    return
  }
  toast.success('举报状态已更新')
  const index = reports.value.findIndex((item) => item.id === report.id)
  if (index >= 0) reports.value[index] = data
}

const statusText = (value) =>
  ({
    OPEN: '待处理',
    REVIEWING: '处理中',
    RESOLVED: '已处理',
    DISMISSED: '已驳回',
  })[value] || value

const statusClass = (value) => `status-${String(value || '').toLowerCase()}`

const formatTime = (value) => {
  if (!value) return ''
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

watch(status, loadReports)

onMounted(async () => {
  await loadCurrentUser()
  await loadReports()
})
</script>

<style scoped>
.reports-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 18px 48px;
}

.reports-header,
.filter-panel,
.report-topline,
.report-meta,
.report-actions {
  display: flex;
}

.reports-header {
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--normal-border-color);
  margin-bottom: 16px;
  padding-bottom: 16px;
}

.reports-kicker {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: 700;
}

.reports-header h1 {
  margin: 6px 0 0;
  font-size: 26px;
}

.text-button,
.report-actions button {
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  background: var(--background-color);
  color: var(--text-color);
  cursor: pointer;
  min-height: 36px;
  padding: 0 12px;
}

.filter-panel {
  align-items: center;
  justify-content: space-between;
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  margin-bottom: 14px;
  padding: 12px;
}

.filter-panel label {
  display: grid;
  gap: 6px;
  font-size: 13px;
}

.filter-panel select,
.report-item textarea {
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  background: var(--background-color);
  color: var(--text-color);
}

.filter-panel select {
  padding: 8px 10px;
}

.report-list {
  display: grid;
  gap: 12px;
}

.report-item {
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  padding: 14px;
}

.report-topline,
.report-meta,
.report-actions {
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.report-topline,
.report-meta {
  color: var(--menu-text-color);
  font-size: 13px;
}

.report-item h2 {
  font-size: 18px;
  margin: 10px 0 6px;
}

.report-item p {
  line-height: 1.7;
  margin: 0 0 10px;
  overflow-wrap: anywhere;
}

.report-item textarea {
  box-sizing: border-box;
  margin-top: 12px;
  min-height: 72px;
  padding: 10px;
  resize: vertical;
  width: 100%;
}

.report-actions {
  margin-top: 10px;
}

.report-actions .positive {
  color: #047857;
}

.report-actions .danger {
  color: #b42318;
}

.status-pill {
  border-radius: 999px;
  background: var(--normal-light-background-color);
  font-size: 12px;
  font-weight: 700;
  padding: 5px 9px;
}

.status-open {
  color: #9a3412;
}

.status-reviewing {
  color: var(--primary-color);
}

.status-resolved {
  color: #047857;
}

.status-dismissed {
  color: #6b7280;
}

.loading-block,
.access-denied {
  align-items: center;
  display: flex;
  justify-content: center;
  min-height: 220px;
}
</style>
