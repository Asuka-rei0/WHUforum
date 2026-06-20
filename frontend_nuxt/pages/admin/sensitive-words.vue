<template>
  <div class="sensitive-words-page">
    <header class="page-header">
      <div>
        <div class="page-kicker">管理员后台</div>
        <h1>敏感词管理</h1>
        <p class="page-desc">
          管理数据库中的审核敏感词。普通词会直接拦截发帖/评论；危机词会标记风险并进入复审流程；禁用后保留记录但不参与匹配。
          环境变量中的词不在此页管理。
        </p>
      </div>
      <button class="text-button" type="button" :disabled="loading" @click="loadWords">
        刷新
      </button>
    </header>

    <div v-if="!isAdmin" class="access-denied">仅管理员可以管理敏感词</div>

    <template v-else>
      <section class="form-panel">
        <h2>{{ editingWord ? '编辑敏感词' : '添加敏感词' }}</h2>
        <form class="word-form" @submit.prevent="saveWord">
          <label>
            <span>词文本</span>
            <input v-model.trim="form.word" type="text" maxlength="64" placeholder="输入敏感词" required />
          </label>
          <label class="checkbox-field">
            <input v-model="form.crisis" type="checkbox" />
            <span>危机词（不硬拦截，进入复审）</span>
          </label>
          <label class="checkbox-field">
            <input v-model="form.enabled" type="checkbox" />
            <span>启用</span>
          </label>
          <div class="form-actions">
            <button class="primary" type="submit" :disabled="saving || !form.word">
              {{ saving ? '保存中…' : '保存' }}
            </button>
            <button v-if="editingWord" class="text-button" type="button" @click="resetForm">
              取消编辑
            </button>
          </div>
        </form>
      </section>

      <section class="filter-panel">
        <label>
          <span>筛选</span>
          <select v-model="filter">
            <option value="all">全部</option>
            <option value="enabled">已启用</option>
            <option value="disabled">已禁用</option>
            <option value="crisis">危机词</option>
            <option value="normal">普通词</option>
          </select>
        </label>
        <span>{{ filteredWords.length }} 条</span>
      </section>

      <section class="word-list">
        <div v-if="loading" class="loading-block">
          <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
        </div>
        <BasePlaceholder v-else-if="filteredWords.length === 0" text="暂无敏感词" />
        <template v-else>
          <article v-for="item in filteredWords" :key="item.id" class="word-item">
            <div class="word-topline">
              <strong class="word-text">{{ item.word }}</strong>
              <span class="status-pill" :class="item.crisis ? 'pill-crisis' : 'pill-normal'">
                {{ item.crisis ? '危机词' : '普通词' }}
              </span>
              <span class="status-pill" :class="item.enabled ? 'pill-enabled' : 'pill-disabled'">
                {{ item.enabled ? '已启用' : '已禁用' }}
              </span>
              <time>{{ formatTime(item.createdAt) }}</time>
            </div>
            <div class="word-actions">
              <button type="button" @click="startEdit(item)">编辑</button>
              <button type="button" class="danger" @click="removeWord(item)">删除</button>
            </div>
          </article>
        </template>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useToast } from 'vue-toastification'
import BasePlaceholder from '~/components/BasePlaceholder.vue'
import { authState, getToken, loadCurrentUser } from '~/utils/auth'
import { getApiErrorMessage } from '~/utils/apiError'

definePageMeta({ middleware: ['auth-required'] })

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl
const toast = useToast()

const words = ref([])
const filter = ref('all')
const loading = ref(false)
const saving = ref(false)
const editingWord = ref(null)
const form = ref({ word: '', crisis: false, enabled: true })
const isAdmin = computed(() => authState.role === 'ADMIN')

const authHeaders = () => {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const filteredWords = computed(() => {
  const list = words.value
  switch (filter.value) {
    case 'enabled':
      return list.filter((item) => item.enabled)
    case 'disabled':
      return list.filter((item) => !item.enabled)
    case 'crisis':
      return list.filter((item) => item.crisis)
    case 'normal':
      return list.filter((item) => !item.crisis)
    default:
      return list
  }
})

const resetForm = () => {
  editingWord.value = null
  form.value = { word: '', crisis: false, enabled: true }
}

const loadWords = async () => {
  if (!isAdmin.value || loading.value) return
  loading.value = true
  try {
    const res = await fetch(`${API_BASE_URL}/api/admin/sensitive-words`, { headers: authHeaders() })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) throw new Error(getApiErrorMessage(data, '加载敏感词失败'))
    words.value = Array.isArray(data) ? data : []
  } catch (e) {
    toast.error(e.message || '加载敏感词失败')
  } finally {
    loading.value = false
  }
}

const saveWord = async () => {
  if (!form.value.word || saving.value) return
  saving.value = true
  try {
    const res = await fetch(`${API_BASE_URL}/api/admin/sensitive-words`, {
      method: 'POST',
      headers: { ...authHeaders(), 'Content-Type': 'application/json' },
      body: JSON.stringify({
        word: form.value.word,
        crisis: form.value.crisis,
        enabled: form.value.enabled,
      }),
    })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) throw new Error(getApiErrorMessage(data, '保存敏感词失败'))
    toast.success(editingWord.value ? '敏感词已更新' : '敏感词已添加')
    resetForm()
    await loadWords()
  } catch (e) {
    toast.error(e.message || '保存敏感词失败')
  } finally {
    saving.value = false
  }
}

const startEdit = (item) => {
  editingWord.value = item
  form.value = {
    word: item.word,
    crisis: Boolean(item.crisis),
    enabled: item.enabled !== false,
  }
}

const removeWord = async (item) => {
  if (!window.confirm(`确定删除敏感词「${item.word}」吗？`)) return
  try {
    const res = await fetch(`${API_BASE_URL}/api/admin/sensitive-words/${item.id}`, {
      method: 'DELETE',
      headers: authHeaders(),
    })
    if (!res.ok) {
      const data = await res.json().catch(() => ({}))
      throw new Error(getApiErrorMessage(data, '删除敏感词失败'))
    }
    toast.success('敏感词已删除')
    if (editingWord.value?.id === item.id) resetForm()
    words.value = words.value.filter((row) => row.id !== item.id)
  } catch (e) {
    toast.error(e.message || '删除敏感词失败')
  }
}

const formatTime = (value) => {
  if (!value) return ''
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

onMounted(async () => {
  await loadCurrentUser()
  await loadWords()
})
</script>

<style scoped>
.sensitive-words-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 18px 48px;
}

.page-header,
.filter-panel,
.word-topline,
.form-actions,
.word-actions {
  display: flex;
}

.page-header {
  align-items: flex-start;
  justify-content: space-between;
  border-bottom: 1px solid var(--normal-border-color);
  margin-bottom: 16px;
  padding-bottom: 16px;
  gap: 16px;
}

.page-kicker {
  color: var(--primary-color);
  font-size: 14px;
  font-weight: 700;
}

.page-header h1 {
  margin: 6px 0 0;
  font-size: 26px;
}

.page-desc {
  color: var(--menu-text-color);
  font-size: 13px;
  line-height: 1.7;
  margin: 10px 0 0;
  max-width: 720px;
}

.text-button,
.form-actions button,
.word-actions button {
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  background: var(--background-color);
  color: var(--text-color);
  cursor: pointer;
  min-height: 36px;
  padding: 0 12px;
}

.form-actions .primary {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: #fff;
}

.form-actions .primary:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.form-panel {
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  margin-bottom: 14px;
  padding: 14px;
}

.form-panel h2 {
  font-size: 16px;
  margin: 0 0 12px;
}

.word-form {
  display: grid;
  gap: 12px;
}

.word-form label {
  display: grid;
  gap: 6px;
  font-size: 13px;
}

.word-form input[type='text'] {
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  background: var(--background-color);
  color: var(--text-color);
  min-height: 36px;
  padding: 0 10px;
}

.checkbox-field {
  align-items: center;
  display: flex !important;
  flex-direction: row;
  gap: 8px !important;
}

.form-actions,
.word-actions {
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
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

.filter-panel select {
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  background: var(--background-color);
  color: var(--text-color);
  padding: 8px 10px;
}

.word-list {
  display: grid;
  gap: 12px;
}

.word-item {
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  padding: 14px;
}

.word-topline {
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.word-text {
  font-size: 16px;
}

.word-topline time {
  color: var(--menu-text-color);
  font-size: 13px;
  margin-left: auto;
}

.word-actions {
  margin-top: 10px;
}

.word-actions .danger {
  color: #b42318;
}

.status-pill {
  border-radius: 999px;
  background: var(--normal-light-background-color);
  font-size: 12px;
  font-weight: 700;
  padding: 5px 9px;
}

.pill-crisis {
  color: #9a3412;
}

.pill-normal {
  color: var(--primary-color);
}

.pill-enabled {
  color: #047857;
}

.pill-disabled {
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
