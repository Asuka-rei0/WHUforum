<template>
  <div class="pending-posts-page">
    <header class="page-header">
      <div>
        <div class="page-kicker">管理员后台</div>
        <h1>待审核帖子</h1>
        <p class="page-desc">审核用户提交的待发布帖子，通过后将公开显示，驳回后作者可见拒绝状态。</p>
      </div>
      <button class="text-button" type="button" :disabled="loading" @click="loadPosts">
        刷新
      </button>
    </header>

    <section class="summary-bar">
      <span>{{ posts.length }} 条待审核</span>
      <NuxtLink class="back-link" to="/admin">返回管理首页</NuxtLink>
    </section>

    <section class="post-list">
      <div v-if="loading" class="loading-block">
        <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
      </div>
      <BasePlaceholder v-else-if="posts.length === 0" text="暂无待审核帖子" />
      <template v-else>
        <article v-for="post in posts" :key="post.id" class="post-item">
          <div class="post-topline">
            <span class="type-pill">{{ postTypeText(post.type) }}</span>
            <time>{{ formatTime(post.createdAt) }}</time>
          </div>
          <h2>
            <NuxtLink :to="`/posts/${post.id}`" class="post-title-link">
              {{ post.title || '无标题' }}
            </NuxtLink>
          </h2>
          <p class="post-excerpt">{{ excerpt(post.content) }}</p>
          <div class="post-meta">
            <span>作者：{{ post.author?.username || '未知' }}</span>
            <span v-if="post.category?.name">分类：{{ post.category.name }}</span>
          </div>
          <div class="post-actions">
            <button type="button" class="positive" :disabled="actingId === post.id" @click="approve(post)">
              {{ actingId === post.id ? '处理中…' : '通过审核' }}
            </button>
            <button type="button" class="danger" :disabled="actingId === post.id" @click="reject(post)">
              驳回
            </button>
            <NuxtLink class="view-link" :to="`/posts/${post.id}`">查看详情</NuxtLink>
          </div>
        </article>
      </template>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useToast } from 'vue-toastification'
import BasePlaceholder from '~/components/BasePlaceholder.vue'
import { getToken } from '~/utils/auth'
import { getApiErrorMessage } from '~/utils/apiError'
import { useConfirm } from '~/composables/useConfirm'

definePageMeta({ middleware: ['admin-required'] })

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl
const toast = useToast()
const { confirm } = useConfirm()

const posts = ref([])
const loading = ref(false)
const actingId = ref(null)

const authHeaders = () => {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const postTypeText = (type) =>
  ({
    POST: '普通帖',
    TREEHOLE: '树洞',
    FLEA_MARKET: '跳蚤市场',
  })[type] || type || '帖子'

const excerpt = (content) => {
  if (!content) return '暂无摘要'
  const text = String(content).replace(/\s+/g, ' ').trim()
  return text.length > 160 ? `${text.slice(0, 160)}…` : text
}

const formatTime = (value) => {
  if (!value) return ''
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

const loadPosts = async () => {
  loading.value = true
  try {
    const res = await fetch(`${API_BASE_URL}/api/admin/posts/pending`, { headers: authHeaders() })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) throw new Error(getApiErrorMessage(data, '加载待审核帖子失败'))
    posts.value = Array.isArray(data) ? data : []
  } catch (e) {
    toast.error(e.message || '加载待审核帖子失败')
  } finally {
    loading.value = false
  }
}

const approve = async (post) => {
  actingId.value = post.id
  try {
    const res = await fetch(`${API_BASE_URL}/api/admin/posts/${post.id}/approve`, {
      method: 'POST',
      headers: authHeaders(),
    })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) throw new Error(getApiErrorMessage(data, '通过审核失败'))
    toast.success('帖子已通过审核')
    posts.value = posts.value.filter((item) => item.id !== post.id)
  } catch (e) {
    toast.error(e.message || '通过审核失败')
  } finally {
    actingId.value = null
  }
}

const reject = async (post) => {
  const ok = await confirm('驳回帖子', `确认驳回「${post.title || '无标题'}」？`)
  if (!ok) return

  actingId.value = post.id
  try {
    const res = await fetch(`${API_BASE_URL}/api/admin/posts/${post.id}/reject`, {
      method: 'POST',
      headers: authHeaders(),
    })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) throw new Error(getApiErrorMessage(data, '驳回失败'))
    toast.success('帖子已驳回')
    posts.value = posts.value.filter((item) => item.id !== post.id)
  } catch (e) {
    toast.error(e.message || '驳回失败')
  } finally {
    actingId.value = null
  }
}

onMounted(loadPosts)
</script>

<style scoped>
.pending-posts-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 18px 48px;
}

.page-header {
  align-items: center;
  border-bottom: 1px solid var(--normal-border-color);
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 16px;
}

.page-kicker {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: 700;
}

.page-header h1 {
  font-size: 26px;
  margin: 6px 0 0;
}

.page-desc {
  color: var(--menu-text-color);
  font-size: 14px;
  line-height: 1.6;
  margin: 8px 0 0;
}

.text-button,
.post-actions button {
  background: var(--background-color);
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  color: var(--text-color);
  cursor: pointer;
  min-height: 36px;
  padding: 0 12px;
}

.summary-bar {
  align-items: center;
  display: flex;
  justify-content: space-between;
  color: var(--menu-text-color);
  font-size: 14px;
  margin-bottom: 14px;
}

.back-link {
  color: var(--primary-color);
  font-weight: 700;
  text-decoration: none;
}

.post-list {
  display: grid;
  gap: 12px;
}

.post-item {
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  padding: 14px;
}

.post-topline {
  align-items: center;
  color: var(--menu-text-color);
  display: flex;
  font-size: 13px;
  gap: 10px;
  justify-content: space-between;
}

.type-pill {
  background: var(--normal-light-background-color);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 9px;
}

.post-item h2 {
  font-size: 18px;
  margin: 10px 0 6px;
}

.post-title-link {
  color: var(--text-color);
  text-decoration: none;
}

.post-title-link:hover {
  color: var(--primary-color);
}

.post-excerpt {
  line-height: 1.7;
  margin: 0 0 10px;
  overflow-wrap: anywhere;
}

.post-meta {
  color: var(--menu-text-color);
  display: flex;
  flex-wrap: wrap;
  font-size: 13px;
  gap: 12px;
}

.post-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.post-actions .positive {
  color: #047857;
}

.post-actions .danger {
  color: #b42318;
}

.view-link {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
}

.loading-block {
  align-items: center;
  display: flex;
  justify-content: center;
  min-height: 220px;
}
</style>
