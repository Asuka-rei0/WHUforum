<template>
  <div class="treehole-list-page">
    <div class="treehole-list-header">
      <div>
        <h1>我的树洞</h1>
        <p>这里会保留你发布过的公开、私密、审核中和限制公开树洞。</p>
      </div>
      <div class="treehole-header-actions">
        <NuxtLink class="treehole-header-action" to="/treehole/new">
          <edit class="treehole-header-icon" />
          发布树洞
        </NuxtLink>
        <NuxtLink class="treehole-header-action secondary" to="/treehole/square">
          树洞广场
        </NuxtLink>
      </div>
    </div>

    <TreeholePostList :posts="posts" :pending="pendingFirst" empty-text="你还没有发布过树洞" />
    <InfiniteLoadMore
      v-if="posts.length > 0"
      :key="ioKey"
      :on-load="fetchNextPage"
      :pause="pendingFirst"
      root-margin="200px 0px"
    />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import InfiniteLoadMore from '~/components/InfiniteLoadMore.vue'
import TreeholePostList from '~/components/TreeholePostList.vue'
import { getToken } from '~/utils/auth'

definePageMeta({ middleware: ['auth-required'] })
useHead({ title: '我的树洞 - 珞珈论坛' })

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl
const pageSize = 10
const page = ref(0)
const posts = ref([])

const tokenHeader = computed(() => {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const buildUrl = (pageNo) => {
  const url = new URL(`${API_BASE_URL}/api/treeholes/me`)
  url.searchParams.set('page', pageNo)
  url.searchParams.set('pageSize', pageSize)
  return url.toString()
}

const { data: firstPage, pending: pendingFirst } = await useAsyncData(
  'treehole-mine:first',
  async () => {
    try {
      const data = await $fetch(buildUrl(0), { headers: tokenHeader.value })
      return Array.isArray(data) ? data : []
    } catch {
      return []
    }
  },
  {
    server: false,
    default: () => [],
  },
)

watch(
  firstPage,
  (data) => {
    page.value = 0
    posts.value = [...(data || [])]
  },
  { immediate: true },
)

const fetchNextPage = async () => {
  if (pendingFirst.value) return false
  const nextPage = page.value + 1
  let data = []
  try {
    const res = await $fetch(buildUrl(nextPage), { headers: tokenHeader.value })
    data = Array.isArray(res) ? res : []
  } catch {
    return true
  }
  posts.value.push(...data)
  const done = data.length < pageSize
  if (!done) page.value = nextPage
  return done
}

const ioKey = computed(() => 'treehole-mine')
</script>

<style scoped>
.treehole-list-page {
  max-width: 980px;
  margin: 0 auto;
  padding: 24px 18px 48px;
}

.treehole-list-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  border-bottom: 1px solid var(--normal-border-color);
  margin-bottom: 16px;
  padding-bottom: 18px;
}

.treehole-list-header h1 {
  color: var(--text-color);
  font-size: 24px;
  margin: 0;
}

.treehole-list-header p {
  color: var(--text-color-secondary);
  line-height: 1.7;
  margin: 6px 0 0;
}

.treehole-header-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.treehole-header-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 38px;
  border: 1px solid var(--primary-color);
  border-radius: 8px;
  background: var(--primary-color);
  color: white;
  text-decoration: none;
  padding: 0 13px;
}

.treehole-header-action.secondary {
  background: var(--background-color);
  color: var(--text-color);
  border-color: var(--normal-border-color);
}

.treehole-header-icon {
  font-size: 17px;
}

@media (max-width: 720px) {
  .treehole-list-header,
  .treehole-header-actions {
    flex-direction: column;
  }

  .treehole-header-actions,
  .treehole-header-action {
    width: 100%;
  }
}
</style>
