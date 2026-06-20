<template>
  <transition name="slide">
    <nav v-if="visible" class="menu">
      <div class="menu-content">
        <div class="menu-nav-groups">
          <div class="menu-group">
            <div class="menu-group-title">浏览</div>
            <NuxtLink class="menu-item" exact-active-class="selected" to="/" @click="handleItemClick">
              <hashtag-key class="menu-item-icon" />
              <span class="menu-item-text">话题</span>
            </NuxtLink>
          </div>

          <div class="menu-group">
            <div class="menu-group-title">参与</div>
            <NuxtLink
              class="menu-item"
              exact-active-class="selected"
              to="/new-post"
              @click="handleItemClick"
            >
              <edit class="menu-item-icon" />
              <span class="menu-item-text">发帖</span>
            </NuxtLink>
            <NuxtLink
              class="menu-item"
              exact-active-class="selected"
              to="/treehole"
              @click="handleItemClick"
            >
              <message-one class="menu-item-icon" />
              <span class="menu-item-text">匿名树洞</span>
            </NuxtLink>
            <NuxtLink
              class="menu-item"
              exact-active-class="selected"
              to="/message"
              @click="handleItemClick"
            >
              <remind class="menu-item-icon" />
              <span class="menu-item-text">我的消息</span>
              <span v-if="unreadCount > 0" class="unread-container">
                <span class="unread"> {{ showUnreadCount }} </span>
              </span>
            </NuxtLink>
            <NuxtLink
              v-if="authState.loggedIn"
              class="menu-item"
              exact-active-class="selected"
              to="/points"
              @click="handleItemClick"
            >
              <finance class="menu-item-icon" />
              <span class="menu-item-text">
                积分商城
                <span v-if="myPoint !== null" class="point-count">{{ myPoint }}</span>
              </span>
            </NuxtLink>
          </div>

          <div class="menu-group">
            <div class="menu-group-title">发现</div>
            <NuxtLink
              class="menu-item"
              exact-active-class="selected"
              to="/activities"
              @click="handleItemClick"
            >
              <gift class="menu-item-icon" />
              <span class="menu-item-text">活动</span>
            </NuxtLink>
            <NuxtLink
              class="menu-item"
              exact-active-class="selected"
              to="/about"
              @click="handleItemClick"
            >
              <info-icon class="menu-item-icon" />
              <span class="menu-item-text">关于</span>
            </NuxtLink>
          </div>

          <div v-if="shouldShowStats" class="menu-section admin-section">
            <div class="section-header" @click="adminOpen = !adminOpen">
              <span>管理后台</span>
              <up v-if="adminOpen" class="menu-item-icon" />
              <down v-else class="menu-item-icon" />
            </div>
            <div v-if="adminOpen" class="section-items">
              <NuxtLink
                class="menu-item"
                exact-active-class="selected"
                to="/admin"
                @click="handleItemClick"
              >
                <dashboard-one class="menu-item-icon" />
                <span class="menu-item-text">管理首页</span>
              </NuxtLink>
              <NuxtLink
                class="menu-item"
                exact-active-class="selected"
                to="/admin/posts/pending"
                @click="handleItemClick"
              >
                <file-text class="menu-item-icon" />
                <span class="menu-item-text">待审核帖子</span>
              </NuxtLink>
              <NuxtLink
                class="menu-item"
                exact-active-class="selected"
                to="/admin/reports"
                @click="handleItemClick"
              >
                <report class="menu-item-icon" />
                <span class="menu-item-text">内容举报</span>
              </NuxtLink>
              <NuxtLink
                class="menu-item"
                exact-active-class="selected"
                to="/admin/treeholes"
                @click="handleItemClick"
              >
                <audit class="menu-item-icon" />
                <span class="menu-item-text">树洞复审</span>
              </NuxtLink>
              <NuxtLink
                class="menu-item"
                exact-active-class="selected"
                to="/admin/sensitive-words"
                @click="handleItemClick"
              >
                <protection class="menu-item-icon" />
                <span class="menu-item-text">敏感词管理</span>
              </NuxtLink>
              <NuxtLink
                class="menu-item"
                exact-active-class="selected"
                to="/about/stats"
                @click="handleItemClick"
              >
                <chart-line class="menu-item-icon" />
                <span class="menu-item-text">站点统计</span>
              </NuxtLink>
            </div>
          </div>
        </div>

        <div class="menu-filter-block">
          <div class="menu-group-title">筛选</div>
          <div class="menu-section">
            <div class="section-header" @click="categoryOpen = !categoryOpen">
              <span>类别</span>
              <up v-if="categoryOpen" class="menu-item-icon" />
              <down v-else class="menu-item-icon" />
            </div>
            <div v-if="categoryOpen" class="section-items">
            <div v-if="isLoadingCategory" class="menu-loading-container">
              <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
            </div>
            <div
              v-else
              v-for="c in categoryData"
              :key="c.id"
              class="section-item"
              :class="{ selected: isCategorySelected(c.id) }"
              @click="gotoCategory(c)"
            >
              <template v-if="c.smallIcon || c.icon">
                <BaseImage
                  v-if="isImageIcon(c.smallIcon || c.icon)"
                  :src="c.smallIcon || c.icon"
                  class="section-item-icon"
                  :alt="c.name"
                />
                <component v-else :is="c.smallIcon || c.icon" class="section-item-icon" />
              </template>
              <span class="section-item-text">
                {{ c.name }}
                <span class="section-item-text-count" v-if="c.count >= 0">x {{ c.count }}</span>
              </span>
            </div>
            </div>
          </div>

          <div class="menu-section">
            <div class="section-header" @click="tagOpen = !tagOpen">
            <span>标签</span>
            <up v-if="tagOpen" class="menu-item-icon" />
            <down v-else class="menu-item-icon" />
          </div>
          <div v-if="tagOpen" class="section-items">
            <div v-if="isLoadingTag" class="menu-loading-container">
              <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
            </div>
            <template v-else>
              <div
                v-for="t in filteredTagData"
                :key="t.id"
                class="section-item"
                :class="{ selected: isTagSelected(t.id) }"
                @click="gotoTag(t)"
              >
                <BaseImage
                  v-if="isImageIcon(t.smallIcon || t.icon)"
                  :src="t.smallIcon || t.icon"
                  class="section-item-icon"
                  :alt="t.name"
                />
                <component
                  v-else-if="t.smallIcon || t.icon"
                  :is="t.smallIcon || t.icon"
                  class="section-item-icon"
                />
                <tag-one v-else class="section-item-icon" />
                <span class="section-item-text"
                  >{{ t.name }} <span class="section-item-text-count">x {{ t.count }}</span></span
                >
              </div>
            </template>
          </div>
          </div>
        </div>
      </div>

      <!-- 解决动态样式的水合错误 -->
      <ClientOnly v-if="!isMobile">
        <div class="menu-footer">
          <div class="menu-footer-btn" @click="cycleTheme">
            <component :is="iconClass" class="menu-item-icon" />
          </div>
        </div>
      </ClientOnly>
    </nav>
  </transition>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { authState, fetchCurrentUser } from '~/utils/auth'
import { fetchUnreadCount, notificationState } from '~/utils/notification'
import { useIsMobile } from '~/utils/screen'
import { cycleTheme, ThemeMode, themeState } from '~/utils/theme'
import { selectedCategoryGlobal, selectedTagsGlobal } from '~/composables/postFilter'
import {
  filterCampusTaxonomy,
  resolveCategoryName,
  sortTagsByCategory,
} from '~/utils/campusTaxonomy'

const isMobile = useIsMobile()
// 判断当前分类是否被选中
const isCategorySelected = (id) => {
  return String(id ?? '') === String(selectedCategoryGlobal.value ?? '')
}

// 判断当前标签是否被选中
const isTagSelected = (id) => {
  const selected = Array.isArray(selectedTagsGlobal.value)
    ? selectedTagsGlobal.value
    : [selectedTagsGlobal.value]
  return selected.some((value) => String(value) === String(id))
}

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const props = defineProps({
  visible: { type: Boolean, default: true },
})
const emit = defineEmits(['item-click'])

const categoryOpen = ref(false)
const tagOpen = ref(false)
const adminOpen = ref(false)
const myPoint = ref(null)

const route = useRoute()

const isAdminRoute = (path) => path.startsWith('/admin') || path.startsWith('/about/stats')

watch(
  () => route.path,
  (path) => {
    if (isAdminRoute(path)) {
      adminOpen.value = true
    }
  },
  { immediate: true },
)

/** ✅ 用 useAsyncData 替换原生 fetch，避免 SSR+CSR 二次请求 */
const {
  data: categoryData,
  pending: isLoadingCategory,
  error: categoryError,
  refresh: refreshCategoryData,
} = await useAsyncData(
  // 稳定 key：避免 hydration 期误判
  'menu:categories',
  () => $fetch(`${API_BASE_URL}/api/categories`).then(filterCampusTaxonomy),
  {
    server: true, // SSR 预取
    default: () => [], // 初始默认值，减少空判断
    // 5 分钟内复用缓存，避免路由往返重复请求
    staleTime: 5 * 60 * 1000,
  },
)

const buildTagUrl = () => {
  const base = API_BASE_URL || (import.meta.client ? window.location.origin : '')
  const url = new URL('/api/tags', base)
  return url.toString()
}

const fetchTags = async () => {
  try {
    return filterCampusTaxonomy(await $fetch(buildTagUrl()))
  } catch (e) {
    console.error('Failed to fetch tags', e)
    return []
  }
}

const {
  data: tagData,
  pending: isLoadingTag,
  error: tagError,
  refresh: refreshTagData,
} = await useAsyncData('menu:tags', fetchTags, {
  server: true,
  default: () => [],
  staleTime: 5 * 60 * 1000,
})

const selectedCategoryName = computed(() =>
  resolveCategoryName(selectedCategoryGlobal.value, categoryData.value),
)
const filteredTagData = computed(() =>
  sortTagsByCategory(tagData.value, selectedCategoryName.value, false),
)

/** 其余逻辑保持不变 */
const iconClass = computed(() => {
  switch (themeState.mode) {
    case ThemeMode.DARK:
      return 'Moon'
    case ThemeMode.LIGHT:
      return 'SunOne'
    default:
      return 'ComputerOne'
  }
})

const unreadCount = computed(() => notificationState.unreadCount)
const showUnreadCount = computed(() => (unreadCount.value > 99 ? '99+' : unreadCount.value))
const shouldShowStats = computed(() => authState.role === 'ADMIN')

const loadPoint = async () => {
  if (authState.loggedIn) {
    const user = await fetchCurrentUser()
    myPoint.value = user ? user.point : null
  } else {
    myPoint.value = null
  }
}

const updateCount = async () => {
  if (authState.loggedIn) {
    await fetchUnreadCount()
  } else {
    notificationState.unreadCount = 0
  }
}

const refreshMenuTaxonomy = () => {
  Promise.all([refreshCategoryData(), refreshTagData()]).catch((e) => {
    console.error('Failed to refresh menu taxonomy', e)
  })
}

onMounted(async () => {
  await Promise.all([updateCount(), loadPoint()])
  window.addEventListener('refresh-home', refreshMenuTaxonomy)
  // 登录态变化时再拉一次未读数和积分；与 useAsyncData 无关
  watch(
    () => authState.loggedIn,
    () => {
      updateCount()
      loadPoint()
    },
  )
})

onBeforeUnmount(() => {
  window.removeEventListener('refresh-home', refreshMenuTaxonomy)
})

const handleItemClick = () => {
  if (window.innerWidth <= 768) emit('item-click')
}

const isImageIcon = (icon) => {
  if (!icon) return false
  return /^https?:\/\//.test(icon) || icon.startsWith('/')
}

const gotoCategory = (c) => {
  const categoryId = c.id ?? c.name
  selectedCategoryGlobal.value = categoryId
  selectedTagsGlobal.value = []
  const value = encodeURIComponent(categoryId)
  navigateTo({ path: '/', query: { category: value } })
  handleItemClick()
}

const gotoTag = (t) => {
  const tagId = t.id ?? t.name
  selectedTagsGlobal.value = [tagId]
  const value = encodeURIComponent(tagId)
  const query = { tags: value }
  if (selectedCategoryGlobal.value !== '' && selectedCategoryGlobal.value != null) {
    query.category = encodeURIComponent(String(selectedCategoryGlobal.value))
  }
  navigateTo({ path: '/', query })
  handleItemClick()
}
</script>

<style scoped>
.menu {
  position: sticky;
  top: var(--header-height);
  width: 220px;
  background-color: var(--app-menu-background-color);
  height: calc(100vh - var(--header-height));
  border-right: 1px solid var(--menu-border-color);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  scrollbar-width: none;
  backdrop-filter: var(--blur-10);
}

.menu-content {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  box-sizing: border-box;
  padding: 10px 10px 0 10px;
}

.menu-content::-webkit-scrollbar {
  display: none;
}

.menu-content {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.menu-nav-groups {
  border-bottom: 1px solid var(--menu-border-color);
  margin-bottom: 4px;
  padding-bottom: 4px;
}

.menu-group {
  padding-bottom: 2px;
}

.menu-group-title {
  color: var(--menu-text-color);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  opacity: 0.55;
  padding: 8px 12px 4px;
}

.menu-filter-block {
  padding-top: 2px;
}

.menu-filter-block > .menu-group-title {
  padding-top: 6px;
}

.admin-section {
  margin-top: 4px;
}

.admin-section .section-items .menu-item {
  margin-left: 0;
}

.menu-item {
  padding: 6px 12px;
  text-decoration: none;
  color: var(--menu-text-color);
  border-radius: 10px;
  display: flex;
  align-items: center;
  transition: background-color 0.5s ease;
}

.menu-item:hover {
  background-color: var(--menu-selected-background-color-hover);
}

.menu-item.selected {
  font-weight: bold;
  background-color: var(--menu-selected-background-color);
}

.menu-item-text {
  font-size: 14px;
  text-decoration: none;
  color: var(--menu-text-color);
}

.unread-container {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: rgb(255, 102, 102);
  margin-left: 15px;
  width: 18px;
  height: 18px;
  text-align: center;
}

.unread {
  color: white;
  font-size: 9px;
  font-weight: bold;
}

.point-count {
  margin-left: 4px;
  font-size: 12px;
  color: var(--primary-color);
}

.menu-item-icon {
  margin-right: 10px;
  opacity: 0.5;
  font-weight: bold;
}

.menu-footer {
  position: relation;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.menu-footer-btn {
  width: 30px;
  height: 30px;
  margin-right: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.menu-section {
  border-bottom: 1px solid var(--menu-border-color);
  padding-bottom: 5px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding: 6px 12px 0 12px;
  color: var(--menu-text-color);
  cursor: pointer;
}

.section-items {
  color: var(--menu-text-color);
  display: flex;
  flex-direction: column;
  margin-top: 4px;
}

.section-item {
  padding: 6px 12px;
  display: flex;
  align-items: center;
  gap: 5px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.5s ease;
}

.more-item {
  justify-content: center;
}

.more-link {
  color: var(--primary-color);
  text-decoration: none;
  font-size: 14px;
  cursor: pointer;
}

.more-link:hover {
  text-decoration: underline;
}

.more-loading {
  font-size: 13px;
  color: var(--menu-text-color);
  opacity: 0.7;
}

.section-item:hover {
  background-color: var(--menu-selected-background-color-hover);
}
.section-item.selected {
  font-weight: bold;
  background-color: var(--menu-selected-background-color);
}

.section-item-text-count {
  font-size: 12px;
  color: var(--menu-text-color);
  opacity: 0.5;
  font-weight: bold;
}

.section-item-text {
  font-size: 14px;
  text-decoration: none;
  color: var(--menu-text-color);
}

.section-item-icon {
  width: 16px;
  height: 16px;
  margin-right: 5px;
  opacity: 0.7;
}

.menu-loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
}

@media (max-width: 768px) {
  .menu {
    position: fixed;
    z-index: 1000;
    box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.1);
    left: 10px;
    border-radius: 20px;
    border-right: none;
    max-height: calc(100vh - var(--header-height) - 20px);
    height: auto;
    top: calc(var(--header-height) + 10px);
    padding-top: 10px;
    background-color: var(--background-color-blur);
  }

  .menu-content {
    border-radius: 20px;
  }

  .slide-enter-active,
  .slide-leave-active {
    transition:
      transform 0.3s ease,
      opacity 0.3s ease,
      width 0.3s ease;
  }

  .slide-enter-from,
  .slide-leave-to {
    transform: translateX(-100%);
    opacity: 0;
  }

  .slide-enter-to,
  .slide-leave-from {
    transform: translateX(0);
    opacity: 1;
  }
}
</style>
