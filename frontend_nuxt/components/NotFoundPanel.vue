<template>
  <div class="not-found-panel" :class="{ standalone }">
    <div class="not-found-card">
      <img class="not-found-emblem" src="/whu-emblem.webp" alt="武汉大学校园论坛" />
      <div class="not-found-code" aria-hidden="true">404</div>
      <h1 class="not-found-title">页面走丢了</h1>
      <p class="not-found-desc">
        <template v-if="displayPath">
          你访问的路径 <code>{{ displayPath }}</code> 不存在，或内容已被删除。
        </template>
        <template v-else>你访问的页面不存在，或内容已被删除。</template>
      </p>
      <p class="not-found-hint">可以试试返回首页，或从下方快捷入口继续浏览。</p>

      <div class="not-found-actions">
        <NuxtLink class="primary-link" to="/">返回首页</NuxtLink>
        <button v-if="canGoBack" class="secondary-link" type="button" @click="goBack">
          返回上一页
        </button>
      </div>

      <nav class="quick-nav" aria-label="快捷导航">
        <NuxtLink v-for="item in quickLinks" :key="item.to" class="quick-link" :to="item.to">
          <component :is="item.icon" class="quick-link-icon" />
          <span>{{ item.label }}</span>
        </NuxtLink>
      </nav>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  path: { type: String, default: '' },
  standalone: { type: Boolean, default: false },
})

const route = useRoute()
const router = useRouter()

const displayPath = computed(() => {
  const value = (props.path || route.path || '').trim()
  if (!value || value === '/404') return ''
  return value
})

const canGoBack = computed(() => import.meta.client && window.history.length > 1)

const quickLinks = [
  { to: '/', label: '话题', icon: 'hashtag-key' },
  { to: '/treehole', label: '树洞', icon: 'message-one' },
  { to: '/activities', label: '活动', icon: 'gift' },
  { to: '/about', label: '关于', icon: 'info-icon' },
]

const goBack = () => {
  if (import.meta.client) router.back()
}
</script>

<style scoped>
.not-found-panel {
  box-sizing: border-box;
  display: flex;
  justify-content: center;
  min-height: calc(100vh - var(--header-height) - 48px);
  padding: 32px 18px 48px;
  width: 100%;
}

.not-found-panel.standalone {
  align-items: center;
  background: var(--login-background-color);
  min-height: 100vh;
  padding-top: 48px;
}

.not-found-card {
  align-items: center;
  display: flex;
  flex-direction: column;
  max-width: 560px;
  text-align: center;
  width: 100%;
}

.not-found-emblem {
  height: 72px;
  margin-bottom: 8px;
  object-fit: contain;
  width: 72px;
}

.not-found-code {
  background: linear-gradient(135deg, var(--primary-color), #005c46);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  font-size: clamp(72px, 18vw, 112px);
  font-weight: 800;
  letter-spacing: -0.04em;
  line-height: 1;
  margin: 0;
}

.not-found-title {
  color: var(--text-color);
  font-size: 28px;
  font-weight: 800;
  margin: 12px 0 10px;
}

.not-found-desc,
.not-found-hint {
  color: var(--menu-text-color);
  font-size: 15px;
  line-height: 1.7;
  margin: 0;
}

.not-found-desc {
  margin-top: 4px;
}

.not-found-desc code {
  background: var(--normal-light-background-color);
  border-radius: 6px;
  color: var(--text-color);
  font-family: inherit;
  font-size: 14px;
  padding: 2px 8px;
  word-break: break-all;
}

.not-found-hint {
  font-size: 13px;
  margin-top: 10px;
}

.not-found-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  margin-top: 28px;
  width: 100%;
}

.primary-link,
.secondary-link {
  align-items: center;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  font-size: 15px;
  font-weight: 700;
  justify-content: center;
  min-height: 44px;
  min-width: 140px;
  padding: 0 18px;
  text-decoration: none;
}

.primary-link {
  background: #005c46;
  border: none;
  color: #fff;
}

.primary-link:hover {
  background: #003f72;
}

.secondary-link {
  background: transparent;
  border: 1px solid var(--normal-border-color);
  color: var(--text-color);
}

.secondary-link:hover {
  background: var(--secondary-color-hover);
}

.quick-nav {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 28px;
  width: 100%;
}

.quick-link {
  align-items: center;
  background: var(--background-color);
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  color: var(--text-color);
  display: flex;
  gap: 8px;
  justify-content: center;
  min-height: 44px;
  padding: 0 12px;
  text-decoration: none;
  transition: background 0.15s ease;
}

.quick-link:hover {
  background: var(--secondary-color-hover);
}

.quick-link-icon {
  color: var(--primary-color);
  font-size: 18px;
}

@media (min-width: 520px) {
  .quick-nav {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .not-found-panel {
    min-height: calc(100vh - var(--header-height) - 24px);
    padding-top: 20px;
  }
}
</style>
