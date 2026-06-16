<template>
  <div class="treehole-admin-page">
    <header class="admin-header">
      <div>
        <div class="admin-kicker">管理员后台</div>
        <h1>树洞复审与干预</h1>
        <p>处理 AI 上报的中高风险树洞，记录每一次复审、身份查看和干预动作。</p>
      </div>
      <button
        class="refresh-button"
        type="button"
        :disabled="loadingCases"
        @click="loadCases(true)"
      >
        <message-one class="button-icon" />
        刷新
      </button>
    </header>

    <div v-if="!isAdmin" class="access-denied">
      <user-icon class="access-icon" />
      <div>仅管理员可以查看树洞复审后台</div>
    </div>

    <template v-else>
      <div class="filters">
        <label>
          <span>风险等级</span>
          <select v-model="riskLevel">
            <option value="">全部</option>
            <option value="L2">L2 中风险</option>
            <option value="L3">L3 高风险</option>
            <option value="L4">L4 紧急风险</option>
          </select>
        </label>
        <label>
          <span>处理状态</span>
          <select v-model="caseStatus">
            <option value="">全部</option>
            <option value="OPEN">待处理</option>
            <option value="IN_PROGRESS">处理中</option>
            <option value="CONTACTED">已联系学生</option>
            <option value="TRANSFERRED">已转交</option>
            <option value="CLOSED">已关闭</option>
          </select>
        </label>
      </div>

      <div class="workspace">
        <section class="case-list" aria-label="风险树洞列表">
          <div v-if="loadingCases" class="loading-block">
            <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
          </div>
          <button
            v-for="item in cases"
            :key="item.id"
            class="case-row"
            :class="{
              selected: selectedPostId === item.postId,
              urgent: isUrgent(item.post?.treeholeRiskLevel),
            }"
            type="button"
            :disabled="loadingDetail"
            @click="selectCase(item)"
          >
            <div class="case-row-top">
              <span class="risk-badge" :class="riskClass(item.post?.treeholeRiskLevel)">
                {{ item.post?.treeholeRiskLevel || '未知' }}
              </span>
              <span class="case-status">{{ statusText(item.status) }}</span>
            </div>
            <div class="case-title">{{ item.post?.title || '未命名树洞' }}</div>
            <div class="case-meta">
              {{ reviewStatusText(item.post?.treeholeReviewStatus) }} ·
              {{ expectedText(item.post?.treeholeExpectedVisibility) }}
            </div>
          </button>
          <BasePlaceholder v-if="!loadingCases && cases.length === 0" text="暂无待复审树洞" />
        </section>

        <section v-if="detail" class="detail-panel">
          <div class="detail-head" :class="{ urgent: isUrgent(detail.post?.treeholeRiskLevel) }">
            <div>
              <div class="detail-label">树洞 #{{ detail.post?.id }}</div>
              <h2>{{ detail.post?.title }}</h2>
            </div>
            <span class="risk-badge large" :class="riskClass(detail.post?.treeholeRiskLevel)">
              {{ detail.post?.treeholeRiskLevel || '未知' }}
            </span>
          </div>

          <div class="detail-grid">
            <div>
              <span>用户期望</span>
              <strong>{{ expectedText(detail.post?.treeholeExpectedVisibility) }}</strong>
            </div>
            <div>
              <span>公开状态</span>
              <strong>{{ reviewStatusText(detail.post?.treeholeReviewStatus) }}</strong>
            </div>
            <div>
              <span>帖子状态</span>
              <strong>{{ detail.post?.status }}</strong>
            </div>
            <div>
              <span>处理状态</span>
              <strong>{{ statusText(detail.caseInfo?.status) }}</strong>
            </div>
          </div>

          <div class="content-section">
            <h3>树洞原文</h3>
            <p class="treehole-content">{{ detail.post?.content }}</p>
          </div>

          <div class="content-section">
            <h3>AI 风险评估</h3>
            <p>{{ detail.post?.treeholeRiskReason || '暂无风险原因' }}</p>
            <p class="recommended-action">
              {{ detail.post?.treeholeRecommendedAction || '暂无建议动作' }}
            </p>
          </div>

          <div class="identity-section">
            <div class="section-title-row">
              <h3>真实身份查看</h3>
              <span>查看会自动留痕</span>
            </div>
            <div class="identity-form">
              <input
                v-model="revealReason"
                placeholder="填写查看原因"
                :disabled="revealingAuthor"
              />
              <button
                type="button"
                :disabled="revealingAuthor || !revealReason.trim()"
                @click="revealAuthor"
              >
                <user-icon class="button-icon" />
                {{ revealingAuthor ? '查看中...' : '查看真实身份' }}
              </button>
            </div>
            <div v-if="identity" class="identity-result">
              <span>ID: {{ identity.id }}</span>
              <span>用户名: {{ identity.username }}</span>
              <span>邮箱: {{ identity.email }}</span>
              <span>校园身份: {{ identity.campusPersonType || '未知' }}</span>
              <span>院系: {{ identity.department || '未知' }}</span>
              <span>校园认证: {{ identity.campusVerified ? '是' : '否' }}</span>
              <span>校园 ID Hash: {{ identity.campusIdHash || '无' }}</span>
            </div>
          </div>

          <div class="actions-section">
            <textarea v-model="actionNote" placeholder="填写处理备注"></textarea>
            <div class="action-buttons">
              <button
                v-for="action in actionButtons"
                :key="action.id"
                type="button"
                :disabled="isClosed || submittingAction"
                :class="action.className"
                @click="executeAction(action.id)"
              >
                <check-correct v-if="action.icon === 'check'" class="button-icon" />
                <close-icon v-else-if="action.icon === 'close'" class="button-icon" />
                <message-one v-else class="button-icon" />
                {{ action.label }}
              </button>
            </div>
          </div>

          <div class="records-section">
            <h3>处理记录</h3>
            <div v-for="record in detail.records" :key="record.id" class="record-item">
              <div class="record-main">
                <span>{{ actionText(record.action) }}</span>
                <time>{{ formatTime(record.createdAt) }}</time>
              </div>
              <div class="record-meta">
                {{ record.adminUsername || '系统' }} · {{ statusText(record.fromStatus) }} →
                {{ statusText(record.toStatus) }} ·
                {{ reviewStatusText(record.fromReviewStatus) }} →
                {{ reviewStatusText(record.toReviewStatus) }}
              </div>
              <p v-if="record.note">{{ record.note }}</p>
            </div>
          </div>
        </section>

        <section v-else class="detail-panel empty-detail">
          <BasePlaceholder text="请选择一个风险树洞查看详情" />
        </section>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useToast } from 'vue-toastification'
import { authState, getToken, loadCurrentUser } from '~/utils/auth'

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl
const toast = useToast()

definePageMeta({ middleware: ['auth-required'] })

const cases = ref([])
const detail = ref(null)
const identity = ref(null)
const selectedPostId = ref(null)
const loadingCases = ref(false)
const loadingDetail = ref(false)
const submittingAction = ref(false)
const revealingAuthor = ref(false)
const riskLevel = ref('')
const caseStatus = ref('')
const actionNote = ref('')
const revealReason = ref('')

const isAdmin = computed(() => authState.role === 'ADMIN')
const isClosed = computed(() => detail.value?.caseInfo?.status === 'CLOSED')

const actionButtons = Object.freeze([
  { id: 'MARK_FALSE_POSITIVE', label: '标记误报', icon: 'check', className: 'positive' },
  { id: 'ALLOW_PUBLIC', label: '允许公开', icon: 'check', className: 'positive' },
  { id: 'RESTRICT_PUBLIC', label: '限制公开', icon: 'close', className: 'danger' },
  { id: 'MARK_CONTACTED', label: '已联系学生', icon: 'message', className: 'neutral' },
  {
    id: 'MARK_TRANSFERRED',
    label: '已转交心理中心或辅导员',
    icon: 'message',
    className: 'neutral',
  },
  { id: 'UPDATE_NOTE', label: '保存备注', icon: 'message', className: 'neutral' },
  { id: 'CLOSE', label: '关闭处理', icon: 'close', className: 'danger' },
])

const authHeaders = () => {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const loadCases = async (reset = false) => {
  if (!isAdmin.value || loadingCases.value) return
  loadingCases.value = true
  try {
    const url = new URL(`${API_BASE_URL}/api/admin/treeholes/risk`)
    url.searchParams.set('page', '0')
    url.searchParams.set('pageSize', '50')
    if (riskLevel.value) url.searchParams.set('riskLevel', riskLevel.value)
    if (caseStatus.value) url.searchParams.set('status', caseStatus.value)
    const res = await fetch(url, { headers: authHeaders() })
    if (!res.ok) throw new Error(await readError(res))
    cases.value = await res.json()
    if (reset || !selectedPostId.value) {
      const first = cases.value[0]
      if (first) {
        await selectCase(first)
      } else {
        detail.value = null
        selectedPostId.value = null
      }
    }
  } catch (e) {
    toast.error(e.message || '加载树洞复审列表失败')
  } finally {
    loadingCases.value = false
  }
}

const selectCase = async (item) => {
  if (!item?.postId || loadingDetail.value) return
  selectedPostId.value = item.postId
  identity.value = null
  revealReason.value = ''
  loadingDetail.value = true
  try {
    const res = await fetch(`${API_BASE_URL}/api/admin/treeholes/${item.postId}`, {
      headers: authHeaders(),
    })
    if (!res.ok) throw new Error(await readError(res))
    detail.value = await res.json()
    actionNote.value = detail.value?.caseInfo?.note || ''
  } catch (e) {
    toast.error(e.message || '加载树洞复审详情失败')
  } finally {
    loadingDetail.value = false
  }
}

const executeAction = async (action) => {
  if (!selectedPostId.value || submittingAction.value) return
  submittingAction.value = true
  try {
    const res = await fetch(`${API_BASE_URL}/api/admin/treeholes/${selectedPostId.value}/actions`, {
      method: 'POST',
      headers: { ...authHeaders(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ action, note: actionNote.value }),
    })
    if (!res.ok) throw new Error(await readError(res))
    detail.value = await res.json()
    actionNote.value = detail.value?.caseInfo?.note || ''
    toast.success('处理记录已保存')
    await loadCases(false)
  } catch (e) {
    toast.error(e.message || '处理失败')
  } finally {
    submittingAction.value = false
  }
}

const revealAuthor = async () => {
  if (!selectedPostId.value || revealingAuthor.value) return
  if (!revealReason.value.trim()) {
    toast.error('请先填写查看原因')
    return
  }
  revealingAuthor.value = true
  try {
    const res = await fetch(
      `${API_BASE_URL}/api/admin/treeholes/${selectedPostId.value}/reveal-author`,
      {
        method: 'POST',
        headers: { ...authHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason: revealReason.value }),
      },
    )
    if (!res.ok) throw new Error(await readError(res))
    identity.value = await res.json()
    toast.success('真实身份已记录查看日志')
    await selectCase({ postId: selectedPostId.value })
  } catch (e) {
    toast.error(e.message || '查看真实身份失败')
  } finally {
    revealingAuthor.value = false
  }
}

const readError = async (res) => {
  try {
    const body = await res.json()
    return body.error || '请求失败'
  } catch {
    return '请求失败'
  }
}

const isUrgent = (level) => level === 'L3' || level === 'L4'
const riskClass = (level) => `risk-${String(level || 'unknown').toLowerCase()}`

const expectedText = (value) =>
  ({
    PUBLIC: '公开到树洞广场',
    ONLY_ME: '仅自己可见',
  })[value] || '未知'

const reviewStatusText = (value) =>
  ({
    AI_REVIEWING: 'AI 审核中',
    PUBLIC: '已公开',
    PRIVATE: '仅自己可见',
    ADMIN_REVIEWING: '管理员复核中',
    PUBLIC_RESTRICTED: '限制公开',
    REPORTED: '已上报管理员',
  })[value] || '未知'

const statusText = (value) =>
  ({
    OPEN: '待处理',
    IN_PROGRESS: '处理中',
    CONTACTED: '已联系学生',
    TRANSFERRED: '已转交',
    CLOSED: '已关闭',
  })[value] || '未建单'

const actionText = (value) =>
  ({
    CASE_CREATED: '创建处置单',
    VIEW_REAL_IDENTITY: '查看真实身份',
    MARK_FALSE_POSITIVE: '标记误报',
    ALLOW_PUBLIC: '允许公开',
    RESTRICT_PUBLIC: '限制公开',
    MARK_CONTACTED: '标记已联系学生',
    MARK_TRANSFERRED: '标记已转交',
    UPDATE_NOTE: '更新备注',
    CLOSE: '关闭处理',
  })[value] || value

const formatTime = (value) => {
  if (!value) return ''
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

watch([riskLevel, caseStatus], () => loadCases(true))

onMounted(async () => {
  await loadCurrentUser()
  if (isAdmin.value) {
    await loadCases(true)
  }
})
</script>

<style scoped>
.treehole-admin-page {
  padding: 24px;
  color: var(--text-color);
}

.admin-header,
.filters,
.workspace,
.detail-head,
.section-title-row,
.identity-form,
.action-buttons,
.record-main {
  display: flex;
  gap: 12px;
}

.admin-header {
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18px;
}

.admin-kicker,
.detail-label,
.case-meta,
.record-meta,
.section-title-row span {
  color: var(--text-color-secondary);
  font-size: 13px;
}

.admin-header h1,
.detail-head h2 {
  margin: 4px 0 6px;
}

.admin-header p {
  margin: 0;
  color: var(--text-color-secondary);
}

.refresh-button,
.identity-form button,
.action-buttons button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 9px 12px;
  background: var(--background-color-secondary);
  color: var(--text-color);
  cursor: pointer;
}

.refresh-button:disabled,
.identity-form button:disabled,
.case-row:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.button-icon {
  width: 16px;
  height: 16px;
}

.filters {
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filters label {
  display: grid;
  gap: 6px;
  font-size: 13px;
  color: var(--text-color-secondary);
}

.filters select,
.identity-form input,
.actions-section textarea {
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--background-color-secondary);
  color: var(--text-color);
}

.filters select {
  min-width: 150px;
  padding: 8px 10px;
}

.workspace {
  align-items: stretch;
}

.case-list {
  width: 330px;
  display: grid;
  align-content: start;
  gap: 10px;
}

.case-row {
  text-align: left;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 12px;
  background: var(--background-color-secondary);
  color: var(--text-color);
  cursor: pointer;
}

.case-row.selected {
  border-color: var(--primary-color);
}

.case-row.urgent {
  border-color: #f97316;
}

.case-row-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.case-title {
  font-weight: 700;
  margin-bottom: 6px;
  word-break: break-word;
}

.case-status {
  font-size: 12px;
  color: var(--text-color-secondary);
}

.risk-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  border-radius: 999px;
  padding: 3px 8px;
  font-size: 12px;
  font-weight: 700;
}

.risk-badge.large {
  min-width: 52px;
  padding: 6px 10px;
}

.risk-l2 {
  background: #fef3c7;
  color: #92400e;
}

.risk-l3 {
  background: #fed7aa;
  color: #9a3412;
}

.risk-l4 {
  background: #fecaca;
  color: #991b1b;
}

.risk-unknown {
  background: var(--background-color);
  color: var(--text-color-secondary);
}

.detail-panel {
  flex: 1;
  min-width: 0;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 18px;
  background: var(--background-color-secondary);
}

.detail-head {
  justify-content: space-between;
  align-items: flex-start;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 14px;
}

.detail-head.urgent {
  border-bottom-color: #f97316;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 16px 0;
}

.detail-grid div,
.identity-result {
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 10px;
  background: var(--background-color);
}

.detail-grid span,
.detail-grid strong {
  display: block;
}

.detail-grid span {
  font-size: 12px;
  color: var(--text-color-secondary);
  margin-bottom: 4px;
}

.content-section,
.identity-section,
.actions-section,
.records-section {
  margin-top: 18px;
}

.content-section h3,
.identity-section h3,
.records-section h3 {
  margin: 0 0 8px;
  font-size: 16px;
}

.treehole-content,
.content-section p {
  white-space: pre-wrap;
  line-height: 1.7;
  margin: 0;
}

.recommended-action {
  margin-top: 8px;
  color: var(--primary-color);
}

.section-title-row {
  justify-content: space-between;
  align-items: center;
}

.identity-form {
  margin-top: 8px;
}

.identity-form input {
  flex: 1;
  padding: 9px 10px;
}

.identity-result {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.actions-section textarea {
  width: 100%;
  min-height: 86px;
  resize: vertical;
  padding: 10px;
  box-sizing: border-box;
}

.action-buttons {
  flex-wrap: wrap;
  margin-top: 10px;
}

.action-buttons button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.action-buttons .positive {
  border-color: #16a34a;
  color: #166534;
}

.action-buttons .danger {
  border-color: #dc2626;
  color: #991b1b;
}

.record-item {
  border-left: 3px solid var(--border-color);
  padding: 0 0 14px 12px;
}

.record-main {
  justify-content: space-between;
  font-weight: 700;
}

.record-main time,
.record-meta {
  color: var(--text-color-secondary);
  font-size: 12px;
}

.record-item p {
  margin: 6px 0 0;
  white-space: pre-wrap;
}

.loading-block,
.access-denied,
.empty-detail {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 260px;
}

.access-denied {
  gap: 10px;
  color: var(--text-color-secondary);
}

.access-icon {
  width: 22px;
  height: 22px;
}

@media (max-width: 960px) {
  .treehole-admin-page {
    padding: 16px;
  }

  .admin-header,
  .workspace {
    flex-direction: column;
  }

  .case-list {
    width: 100%;
  }

  .detail-grid,
  .identity-result {
    grid-template-columns: 1fr;
  }
}
</style>
