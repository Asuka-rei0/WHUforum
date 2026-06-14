<template>
  <div class="treehole-list-page">
    <div class="treehole-list-header">
      <div>
        <h1>树洞广场</h1>
        <p>公开树洞会在审核通过后展示；你自己的公开树洞审核中也会出现在这里。</p>
      </div>
      <div class="treehole-header-actions">
        <NuxtLink class="treehole-header-action" to="/treehole/new">
          <edit class="treehole-header-icon" />
          发布树洞
        </NuxtLink>
        <NuxtLink v-if="isLogin" class="treehole-header-action secondary" to="/treehole/mine">
          我的树洞
        </NuxtLink>
      </div>
    </div>

    <TreeholePostList :posts="posts" :pending="pendingFirst" empty-text="树洞广场暂时没有内容" />
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
import { authState, getToken } from '~/utils/auth'

useHead({ title: '树洞广场 - 珞珈论坛' })

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl
const pageSize = 10
const page = ref(0)
const posts = ref([])
const isLogin = computed(() => authState.loggedIn)

const tokenHeader = computed(() => {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const buildUrl = (pageNo) => {
  const url = new URL(`${API_BASE_URL}/api/treeholes/square`)
  url.searchParams.set('page', pageNo)
  url.searchParams.set('pageSize', pageSize)
  return url.toString()
}

const {
  data: firstPage,
  pending: pendingFirst,
  refresh: refreshFirst,
} = await useAsyncData(
  'treehole-square:first',
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

watch(isLogin, () => refreshFirst())

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

const ioKey = computed(() => `treehole-square-${isLogin.value}`)
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
