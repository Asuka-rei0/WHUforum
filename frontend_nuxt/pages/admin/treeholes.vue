<template>
  <div class="treehole-admin-page">
    <header class="admin-header">
      <div class="admin-header-copy">
        <div class="admin-kicker">管理员后台</div>
        <h1>树洞复审与干预</h1>
        <p>处理 AI 上报的中高风险树洞，记录每一次复审、身份查看和干预动作。</p>
      </div>
      <button
        class="text-button"
        type="button"
        :disabled="loadingCases"
        @click="loadCases(true)"
      >
        <message-one class="button-icon" />
        刷新
      </button>
    </header>

    <section class="filter-panel" aria-label="树洞复审筛选">
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
        <div class="queue-count">
          <strong>{{ cases.length }}</strong>
          <span>条记录</span>
        </div>
      </section>

      <div class="review-workspace">
        <section class="case-list-panel" aria-label="风险树洞列表">
          <div class="panel-title-row">
            <h2>风险队列</h2>
            <span>{{ loadingCases ? '加载中' : '按更新时间排序' }}</span>
          </div>

          <div v-if="loadingCases" class="loading-block">
            <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
          </div>

          <div v-else class="case-list">
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
                <span class="status-pill" :class="statusClass(item.status)">
                  {{ statusText(item.status) }}
                </span>
              </div>
              <div class="case-title">{{ item.post?.title || '未命名树洞' }}</div>
              <div class="case-reason">
                {{ truncateText(item.post?.treeholeRiskReason, 58) }}
              </div>
              <div class="case-meta">
                树洞 #{{ item.postId }} ·
                {{ reviewStatusText(item.post?.treeholeReviewStatus) }} ·
                {{ expectedText(item.post?.treeholeExpectedVisibility) }}
              </div>
            </button>
            <BasePlaceholder v-if="cases.length === 0" text="暂无待复审树洞" />
          </div>
        </section>

        <section v-if="detail" class="detail-panel">
          <div class="detail-head" :class="{ urgent: isUrgent(detail.post?.treeholeRiskLevel) }">
            <div class="detail-heading">
              <div class="detail-label">树洞 #{{ detail.post?.id }}</div>
              <h2>{{ detail.post?.title || '未命名树洞' }}</h2>
              <div class="detail-subline">
                <span>{{ reviewStatusText(detail.post?.treeholeReviewStatus) }}</span>
                <span>{{ statusText(detail.caseInfo?.status) }}</span>
              </div>
            </div>
            <span class="risk-badge large" :class="riskClass(detail.post?.treeholeRiskLevel)">
              {{ detail.post?.treeholeRiskLevel || '未知' }}
            </span>
          </div>

          <div class="status-grid">
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
              <strong>{{ postStatusText(detail.post?.status) }}</strong>
            </div>
            <div>
              <span>处理状态</span>
              <strong>{{ statusText(detail.caseInfo?.status) }}</strong>
            </div>
          </div>

          <section class="info-section">
            <h3>树洞原文</h3>
            <p class="treehole-content">{{ detail.post?.content || '暂无内容' }}</p>
          </section>

          <section class="info-section risk-section">
            <div class="section-title-row">
              <h3>AI 风险评估</h3>
              <span>{{ detail.post?.treeholeReviewedAt ? formatTime(detail.post.treeholeReviewedAt) : '' }}</span>
            </div>
            <p>{{ detail.post?.treeholeRiskReason || '暂无风险原因' }}</p>
            <p class="recommended-action">
              {{ detail.post?.treeholeRecommendedAction || '暂无建议动作' }}
            </p>
          </section>

          <section class="info-section identity-section">
            <div class="section-title-row">
              <h3>真实身份</h3>
              <span>查看会自动留痕</span>
            </div>
            <div class="identity-form">
              <input
                v-model="revealReason"
                placeholder="填写查看原因"
                :disabled="revealingAuthor"
              />
              <button
                class="text-button primary"
                type="button"
                :disabled="revealingAuthor || !revealReason.trim()"
                @click="revealAuthor"
              >
                <user-icon class="button-icon" />
                {{ revealingAuthor ? '查看中...' : '查看真实身份' }}
              </button>
            </div>

            <div v-if="identity" class="identity-result">
              <div class="identity-summary">
                <div class="identity-avatar">
                  <user-icon />
                </div>
                <div class="identity-main">
                  <strong>{{ identity.username || '未知用户' }}</strong>
                  <span>{{ identity.email || '未登记邮箱' }}</span>
                </div>
                <div class="identity-actions">
                  <button
                    class="text-button primary"
                    type="button"
                    :disabled="messagingAuthor"
                    @click="messageAuthor"
                  >
                    <message-one class="button-icon" />
                    {{ messagingAuthor ? '打开中...' : '发私信' }}
                  </button>
                  <button
                    v-if="identity.email"
                    class="text-button"
                    type="button"
                    @click="copyIdentityEmail"
                  >
                    复制邮箱
                  </button>
                </div>
              </div>
              <dl class="identity-fields">
                <div>
                  <dt>用户 ID</dt>
                  <dd>{{ identity.id }}</dd>
                </div>
                <div>
                  <dt>注册邮箱</dt>
                  <dd>{{ identity.email || '未登记' }}</dd>
                </div>
                <div>
                  <dt>校园身份</dt>
                  <dd>{{ identity.campusPersonType || '未知' }}</dd>
                </div>
                <div>
                  <dt>院系</dt>
                  <dd>{{ identity.department || '未知' }}</dd>
                </div>
                <div>
                  <dt>校园认证</dt>
                  <dd>{{ identity.campusVerified ? '已认证' : '未认证' }}</dd>
                </div>
                <div>
                  <dt>校园 ID Hash</dt>
                  <dd>{{ identity.campusIdHash || '无' }}</dd>
                </div>
              </dl>
            </div>
          </section>

          <section class="info-section actions-section">
            <div class="section-title-row">
              <h3>处置动作</h3>
              <span v-if="isClosed">该处置单已关闭</span>
            </div>
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
          </section>

          <section class="info-section records-section">
            <h3>处理记录</h3>
            <div v-if="detail.records?.length" class="record-list">
              <div v-for="record in detail.records" :key="record.id" class="record-item">
                <div class="record-main">
                  <span>{{ actionText(record.action) }}</span>
                  <time>{{ formatTime(record.createdAt) }}</time>
                </div>
                <div class="record-meta">
                  {{ record.adminUsername || '系统' }} · {{ statusText(record.fromStatus) }} ->
                  {{ statusText(record.toStatus) }} ·
                  {{ reviewStatusText(record.fromReviewStatus) }} ->
                  {{ reviewStatusText(record.toReviewStatus) }}
                </div>
                <p v-if="record.note">{{ record.note }}</p>
              </div>
            </div>
            <BasePlaceholder v-else text="暂无处理记录" />
          </section>
        </section>

        <section v-else class="detail-panel empty-detail">
          <BasePlaceholder text="请选择一个风险树洞查看详情" />
        </section>
      </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useToast } from 'vue-toastification'
import { authState, getToken } from '~/utils/auth'

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl
const toast = useToast()

definePageMeta({ middleware: ['admin-required'] })

const cases = ref([])
const detail = ref(null)
const identity = ref(null)
const selectedPostId = ref(null)
const loadingCases = ref(false)
const loadingDetail = ref(false)
const submittingAction = ref(false)
const revealingAuthor = ref(false)
const messagingAuthor = ref(false)
const riskLevel = ref('')
const caseStatus = ref('')
const actionNote = ref('')
const revealReason = ref('')

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
  if (loadingCases.value) return
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
        identity.value = null
        selectedPostId.value = null
      }
    }
  } catch (e) {
    toast.error(e.message || '加载树洞复审列表失败')
  } finally {
    loadingCases.value = false
  }
}

const loadDetail = async (postId, options = {}) => {
  if (!postId || loadingDetail.value) return
  const preserveIdentity = options.preserveIdentity === true
  const currentIdentity = preserveIdentity ? identity.value : null
  if (!preserveIdentity) {
    identity.value = null
    revealReason.value = ''
  }
  loadingDetail.value = true
  try {
    const res = await fetch(`${API_BASE_URL}/api/admin/treeholes/${postId}`, {
      headers: authHeaders(),
    })
    if (!res.ok) throw new Error(await readError(res))
    detail.value = await res.json()
    if (preserveIdentity) {
      identity.value = currentIdentity
    }
    actionNote.value = detail.value?.caseInfo?.note || ''
  } catch (e) {
    toast.error(e.message || '加载树洞复审详情失败')
  } finally {
    loadingDetail.value = false
  }
}

const selectCase = async (item) => {
  if (!item?.postId || loadingDetail.value) return
  selectedPostId.value = item.postId
  await loadDetail(item.postId)
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
        body: JSON.stringify({ reason: revealReason.value.trim() }),
      },
    )
    if (!res.ok) throw new Error(await readError(res))
    identity.value = await res.json()
    toast.success('已获取发布者账号，查看记录已留痕')
    await loadDetail(selectedPostId.value, { preserveIdentity: true })
  } catch (e) {
    toast.error(e.message || '查看真实身份失败')
  } finally {
    revealingAuthor.value = false
  }
}

const messageAuthor = async () => {
  if (!identity.value?.id || messagingAuthor.value) return
  if (Number(identity.value.id) === Number(authState.userId)) {
    toast.error('不能给自己发私信')
    return
  }
  messagingAuthor.value = true
  try {
    const res = await fetch(`${API_BASE_URL}/api/messages/conversations`, {
      method: 'POST',
      headers: { ...authHeaders(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ recipientId: identity.value.id }),
    })
    if (!res.ok) throw new Error(await readError(res))
    const result = await res.json()
    await navigateTo(`/message-box/${result.conversationId}`)
  } catch (e) {
    toast.error(e.message || '无法发起私信')
  } finally {
    messagingAuthor.value = false
  }
}

const copyIdentityEmail = async () => {
  if (!identity.value?.email || !import.meta.client || !navigator?.clipboard) return
  try {
    await navigator.clipboard.writeText(identity.value.email)
    toast.success('邮箱已复制')
  } catch {
    toast.error('复制失败')
  }
}

const readError = async (res) => {
  try {
    const body = await res.json()
    return body.error || body.message || '请求失败'
  } catch {
    return '请求失败'
  }
}

const truncateText = (value, maxLength = 60) => {
  const text = String(value || '').replace(/\s+/g, ' ').trim()
  if (!text) return '暂无风险原因'
  return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text
}

const isUrgent = (level) => level === 'L3' || level === 'L4'
const riskClass = (level) => `risk-${String(level || 'unknown').toLowerCase()}`
const statusClass = (value) => `status-${String(value || 'none').toLowerCase()}`

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

const postStatusText = (value) =>
  ({
    PUBLISHED: '已发布',
    PENDING: '待处理',
    DELETED: '已删除',
    REJECTED: '已拒绝',
  })[value] || value || '未知'

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

onMounted(() => {
  loadCases(true)
})
</script>

<style scoped>
.treehole-admin-page {
  --admin-muted: var(--menu-text-color);
  --admin-border: var(--normal-border-color);
  --admin-panel: var(--background-color);
  --admin-soft: var(--normal-light-background-color);
  --admin-success: #047857;
  --admin-warning: #9a3412;
  --admin-danger: #b42318;

  max-width: 1180px;
  margin: 0 auto;
  padding: 24px 18px 48px;
  color: var(--text-color);
}

.admin-header,
.filter-panel,
.filters,
.review-workspace,
.panel-title-row,
.case-row-top,
.detail-head,
.detail-subline,
.section-title-row,
.identity-form,
.identity-summary,
.identity-actions,
.action-buttons,
.record-main {
  display: flex;
}

.admin-header {
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--admin-border);
  margin-bottom: 16px;
  padding-bottom: 18px;
}

.admin-header-copy {
  min-width: 0;
}

.admin-kicker {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 8px;
}

.admin-header h1 {
  color: var(--text-color);
  font-size: 26px;
  line-height: 1.25;
  margin: 0;
}

.admin-header p {
  color: var(--admin-muted);
  line-height: 1.7;
  margin: 8px 0 0;
}

.text-button,
.action-buttons button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 36px;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  background: var(--admin-panel);
  color: var(--text-color);
  cursor: pointer;
  font: inherit;
  padding: 0 12px;
  text-decoration: none;
}

.text-button.primary {
  border-color: var(--primary-color);
  background: var(--primary-color);
  color: white;
}

.text-button:hover:not(:disabled),
.action-buttons button:hover:not(:disabled) {
  border-color: var(--primary-color);
}

.text-button:disabled,
.action-buttons button:disabled,
.case-row:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.button-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.filter-panel {
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  background: var(--admin-panel);
  margin-bottom: 16px;
  padding: 14px;
}

.filters {
  flex-wrap: wrap;
  gap: 12px;
}

.filters label {
  display: grid;
  gap: 6px;
  color: var(--admin-muted);
  font-size: 13px;
}

.filters select,
.identity-form input,
.actions-section textarea {
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  background: var(--admin-panel);
  color: var(--text-color);
  font: inherit;
}

.filters select {
  min-width: 160px;
  padding: 8px 10px;
}

.queue-count {
  color: var(--admin-muted);
  display: grid;
  gap: 2px;
  justify-items: end;
  white-space: nowrap;
}

.queue-count strong {
  color: var(--primary-color);
  font-size: 22px;
  line-height: 1;
}

.queue-count span {
  font-size: 12px;
}

.review-workspace {
  align-items: flex-start;
  gap: 16px;
}

.case-list-panel,
.detail-panel {
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  background: var(--admin-panel);
}

.case-list-panel {
  flex: 0 0 330px;
  padding: 14px;
}

.panel-title-row {
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border-bottom: 1px solid var(--admin-border);
  margin-bottom: 12px;
  padding-bottom: 10px;
}

.panel-title-row h2 {
  color: var(--text-color);
  font-size: 16px;
  margin: 0;
}

.panel-title-row span,
.case-meta,
.case-reason,
.detail-label,
.detail-subline,
.section-title-row span,
.record-main time,
.record-meta {
  color: var(--admin-muted);
  font-size: 12px;
}

.case-list {
  display: grid;
  gap: 10px;
}

.case-row {
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  background: var(--admin-panel);
  color: var(--text-color);
  cursor: pointer;
  padding: 12px;
  text-align: left;
  transition:
    border-color 0.18s ease,
    transform 0.18s ease;
}

.case-row:hover:not(:disabled),
.case-row.selected {
  border-color: var(--primary-color);
}

.case-row:hover:not(:disabled) {
  transform: translateY(-1px);
}

.case-row.urgent {
  border-left-color: var(--admin-danger);
  border-left-width: 3px;
}

.case-row-top {
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 9px;
}

.case-title {
  font-size: 15px;
  font-weight: 700;
  line-height: 1.4;
  margin-bottom: 6px;
  overflow-wrap: anywhere;
}

.case-reason {
  line-height: 1.5;
  margin-bottom: 7px;
}

.risk-badge,
.status-pill {
  align-items: center;
  border-radius: 999px;
  display: inline-flex;
  font-size: 12px;
  font-weight: 700;
  justify-content: center;
  line-height: 1;
  min-height: 24px;
  padding: 0 9px;
  white-space: nowrap;
}

.risk-badge.large {
  min-height: 30px;
  min-width: 54px;
}

.risk-l2 {
  background: rgba(245, 158, 11, 0.14);
  color: #8a5a00;
}

.risk-l3 {
  background: rgba(249, 115, 22, 0.16);
  color: var(--admin-warning);
}

.risk-l4 {
  background: rgba(244, 63, 94, 0.16);
  color: var(--admin-danger);
}

.risk-unknown {
  background: var(--admin-soft);
  color: var(--admin-muted);
}

.status-pill {
  border: 1px solid var(--admin-border);
  color: var(--admin-muted);
  font-weight: 600;
}

.status-open,
.status-in_progress {
  color: #8a5a00;
}

.status-contacted,
.status-transferred {
  color: var(--primary-color);
}

.status-closed {
  color: var(--admin-success);
}

.detail-panel {
  flex: 1;
  min-width: 0;
  padding: 18px;
}

.detail-head {
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  border-bottom: 1px solid var(--admin-border);
  padding-bottom: 14px;
}

.detail-head.urgent {
  border-bottom-color: rgba(244, 63, 94, 0.5);
}

.detail-heading {
  min-width: 0;
}

.detail-label {
  margin-bottom: 5px;
}

.detail-head h2 {
  color: var(--text-color);
  font-size: 22px;
  line-height: 1.35;
  margin: 0;
  overflow-wrap: anywhere;
}

.detail-subline {
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.detail-subline span + span::before {
  content: '·';
  margin-right: 8px;
}

.status-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 16px 0 2px;
}

.status-grid div {
  border-left: 3px solid var(--admin-border);
  padding: 2px 10px;
}

.status-grid span,
.status-grid strong {
  display: block;
}

.status-grid span {
  color: var(--admin-muted);
  font-size: 12px;
  margin-bottom: 5px;
}

.status-grid strong {
  font-size: 14px;
  line-height: 1.35;
}

.info-section {
  border-top: 1px solid var(--admin-border);
  margin-top: 18px;
  padding-top: 16px;
}

.info-section h3 {
  color: var(--text-color);
  font-size: 16px;
  margin: 0 0 10px;
}

.treehole-content,
.info-section p {
  line-height: 1.75;
  margin: 0;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.risk-section {
  border-top-color: rgba(10, 110, 120, 0.28);
}

.recommended-action {
  color: var(--primary-color);
  margin-top: 8px !important;
}

.section-title-row {
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.identity-form {
  align-items: stretch;
  gap: 10px;
}

.identity-form input {
  flex: 1;
  min-width: 0;
  padding: 0 11px;
}

.identity-result {
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  margin-top: 12px;
  overflow: hidden;
}

.identity-summary {
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--admin-border);
  padding: 12px;
}

.identity-avatar {
  align-items: center;
  background: var(--admin-soft);
  border-radius: 50%;
  color: var(--primary-color);
  display: flex;
  flex: 0 0 38px;
  height: 38px;
  justify-content: center;
  width: 38px;
}

.identity-main {
  display: grid;
  flex: 1;
  gap: 3px;
  min-width: 0;
}

.identity-main strong,
.identity-main span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.identity-main span {
  color: var(--admin-muted);
  font-size: 13px;
}

.identity-actions {
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.identity-fields {
  display: grid;
  gap: 0;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 0;
}

.identity-fields div {
  border-bottom: 1px solid var(--admin-border);
  min-width: 0;
  padding: 10px 12px;
}

.identity-fields div:nth-last-child(-n + 2) {
  border-bottom: 0;
}

.identity-fields dt {
  color: var(--admin-muted);
  font-size: 12px;
  margin-bottom: 4px;
}

.identity-fields dd {
  margin: 0;
  overflow-wrap: anywhere;
}

.actions-section textarea {
  box-sizing: border-box;
  min-height: 84px;
  padding: 10px 11px;
  resize: vertical;
  width: 100%;
}

.action-buttons {
  flex-wrap: wrap;
  gap: 9px;
  margin-top: 10px;
}

.action-buttons .positive {
  border-color: rgba(4, 120, 87, 0.45);
  color: var(--admin-success);
}

.action-buttons .danger {
  border-color: rgba(180, 35, 24, 0.45);
  color: var(--admin-danger);
}

.record-list {
  display: grid;
  gap: 14px;
}

.record-item {
  border-left: 3px solid var(--admin-border);
  padding-left: 12px;
}

.record-main {
  align-items: baseline;
  gap: 10px;
  justify-content: space-between;
}

.record-main span {
  font-weight: 700;
}

.record-meta {
  line-height: 1.5;
  margin-top: 4px;
}

.record-item p {
  line-height: 1.7;
  margin: 6px 0 0;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.loading-block,
.empty-detail {
  align-items: center;
  display: flex;
  justify-content: center;
  min-height: 260px;
}

@media (max-width: 980px) {
  .review-workspace {
    flex-direction: column;
  }

  .case-list-panel {
    flex-basis: auto;
    width: 100%;
  }

  .detail-panel {
    box-sizing: border-box;
    width: 100%;
  }

  .status-grid,
  .identity-fields {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .treehole-admin-page {
    padding: 16px 14px 36px;
  }

  .admin-header,
  .filter-panel,
  .identity-summary {
    align-items: stretch;
    flex-direction: column;
  }

  .queue-count {
    justify-items: start;
  }

  .filters,
  .filters label,
  .filters select,
  .text-button,
  .identity-form,
  .identity-actions,
  .action-buttons button {
    width: 100%;
  }

  .identity-form {
    flex-direction: column;
  }

  .identity-form input {
    min-height: 38px;
  }

  .status-grid,
  .identity-fields {
    grid-template-columns: 1fr;
  }

  .identity-fields div:nth-last-child(-n + 2) {
    border-bottom: 1px solid var(--admin-border);
  }

  .identity-fields div:last-child {
    border-bottom: 0;
  }

  .record-main {
    align-items: flex-start;
    flex-direction: column;
    gap: 3px;
  }
}
</style>
