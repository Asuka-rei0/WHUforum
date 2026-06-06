<template>
  <div class="profile-page">
    <div v-if="isLoading" class="loading-page">
      <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)" />
    </div>

    <div v-else>
      <div class="profile-page-header">
        <div class="profile-page-header-avatar">
          <BaseUserAvatar
            :src="user.avatar"
            :user-id="user.id"
            alt="avatar"
            class="profile-page-header-avatar-img"
          />
        </div>
        <div class="profile-page-header-user-info">
          <div class="profile-page-header-user-info-name">{{ user.username }}</div>
          <div class="profile-page-header-user-info-description">{{ user.introduction }}</div>
          <div class="profile-page-header-user-info-buttons">
            <div
              v-if="!isMine && !subscribed"
              class="profile-page-header-subscribe-button"
              @click="subscribeUser"
            >
              <add-user />
              关注
            </div>
            <div
              v-if="!isMine && subscribed"
              class="profile-page-header-unsubscribe-button"
              @click="unsubscribeUser"
            >
              <reduce-user />
              取消关注
            </div>
            <div v-if="!isMine" class="profile-page-header-send-mail-button" @click="sendMessage">
              <message-one />
              发私信
            </div>
          </div>
          <LevelProgress
            :exp="levelInfo.exp"
            :current-level="levelInfo.currentLevel"
            :next-exp="levelInfo.nextExp"
          />
          <div class="profile-level-target">
            目标 Lv.{{ levelInfo.currentLevel + 1 }}
            <ToolTip
              content="经验值可通过发帖、评论等操作获得，达到目标后即可提升等级，解锁更多功能。"
              placement="bottom"
            >
              <info-icon class="profile-exp-info" />
            </ToolTip>
          </div>
        </div>
      </div>

      <div class="profile-info">
        <div class="profile-info-item">
          <div class="profile-info-item-label">加入时间:</div>
          <div class="profile-info-item-value">{{ formatDate(user.createdAt) }}</div>
        </div>
        <div class="profile-info-item">
          <div class="profile-info-item-label">最后发帖时间:</div>
          <div class="profile-info-item-value">
            {{ user.lastPostTime != null ? formatDate(user.lastPostTime) : '暂无帖子' }}
          </div>
        </div>
        <div class="profile-info-item">
          <div class="profile-info-item-label">最后评论时间:</div>
          <div class="profile-info-item-value">
            {{ user.lastCommentTime != null ? formatDate(user.lastCommentTime) : '暂无评论' }}
          </div>
        </div>
        <div class="profile-info-item">
          <div class="profile-info-item-label">浏览量:</div>
          <div class="profile-info-item-value">{{ user.totalViews }}</div>
        </div>
      </div>

      <BaseTabs v-model="selectedTab" :tabs="tabs">
        <div v-if="tabLoading" class="tab-loading">
          <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)" />
        </div>
        <template v-else>
          <div v-if="selectedTab === 'summary'" class="profile-summary">
            <div class="total-summary">
              <div class="summary-title">统计信息</div>
              <div class="total-summary-content">
                <div class="total-summary-item">
                  <div class="total-summary-item-label">访问天数</div>
                  <div class="total-summary-item-value">{{ user.visitedDays }}</div>
                </div>
                <div class="total-summary-item">
                  <div class="total-summary-item-label">已读帖子</div>
                  <div class="total-summary-item-value">{{ user.readPosts }}</div>
                </div>
                <div class="total-summary-item">
                  <div class="total-summary-item-label">已送出的💗</div>
                  <div class="total-summary-item-value">{{ user.likesSent }}</div>
                </div>
                <div class="total-summary-item">
                  <div class="total-summary-item-label">已收到的💗</div>
                  <div class="total-summary-item-value">{{ user.likesReceived }}</div>
                </div>
              </div>
            </div>
            <div class="summary-divider">
              <div class="hot-reply">
                <div class="summary-title">热门回复</div>
                <div class="summary-content" v-if="hotReplies.length > 0">
                  <BaseTimeline :items="hotReplies">
                    <template #item="{ item }">
                      在
                      <NuxtLink :to="`/posts/${item.comment.post.id}`" class="timeline-link">
                        {{ item.comment.post.title }}
                      </NuxtLink>
                      <template v-if="item.comment.parentComment">
                        <NuxtLink
                          :to="`/posts/${item.comment.post.id}#comment-${item.comment.parentComment.id}`"
                          class="timeline-comment-link"
                        >
                          {{ stripMarkdownLength(item.comment.parentComment.content, 200) }}
                        </NuxtLink>
                        <next class="reply-icon" /> 回复了
                      </template>
                      <template v-else> 下评论了 </template>
                      <NuxtLink
                        :to="`/posts/${item.comment.post.id}#comment-${item.comment.id}`"
                        class="timeline-comment-link"
                      >
                        {{ stripMarkdownLength(item.comment.content, 200) }}
                      </NuxtLink>
                      <div class="timeline-date">
                        {{ formatDate(item.comment.createdAt) }}
                      </div>
                    </template>
                  </BaseTimeline>
                </div>
                <div v-else>
                  <div class="summary-empty">暂无热门回复</div>
                </div>
              </div>
              <div class="hot-topic">
                <div class="summary-title">热门话题</div>
                <div class="summary-content" v-if="hotPosts.length > 0">
                  <BaseTimeline :items="hotPosts">
                    <template #item="{ item }">
                      <TimelinePostItem :item="item" />
                    </template>
                  </BaseTimeline>
                </div>
                <div v-else>
                  <div class="summary-empty">暂无热门话题</div>
                </div>
              </div>
              <div class="hot-tag">
                <div class="summary-title">TA创建的tag</div>
                <div class="summary-content" v-if="hotTags.length > 0">
                  <BaseTimeline :items="hotTags">
                    <template #item="{ item }">
                      <TimelineTagItem :item="item" />
                    </template>
                  </BaseTimeline>
                </div>
                <div v-else>
                  <div class="summary-empty">暂无标签</div>
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="selectedTab === 'timeline'" class="profile-timeline">
            <div class="timeline-tabs">
              <div
                :class="['timeline-tab-item', { selected: timelineFilter === 'all' }]"
                @click="timelineFilter = 'all'"
              >
                全部
              </div>
              <div
                :class="['timeline-tab-item', { selected: timelineFilter === 'articles' }]"
                @click="timelineFilter = 'articles'"
              >
                文章
              </div>
              <div
                :class="['timeline-tab-item', { selected: timelineFilter === 'comments' }]"
                @click="timelineFilter = 'comments'"
              >
                评论和回复
              </div>
              <div
                v-if="isMine"
                :class="['timeline-tab-item', { selected: timelineFilter === 'reads' }]"
                @click="timelineFilter = 'reads'"
              >
                浏览记录
              </div>
            </div>
            <BasePlaceholder
              v-if="
                timelineFilter === 'reads'
                  ? readPosts.length === 0
                  : filteredTimelineItems.length === 0
              "
              :text="timelineFilter === 'reads' ? '暂无浏览记录' : '暂无时间线'"
              icon="inbox"
            />
            <div class="timeline-list">
              <BaseTimeline v-if="timelineFilter !== 'reads'" :items="filteredTimelineItems">
                <template #item="{ item }">
                  <template v-if="item.type === 'post'">
                    <TimelinePostItem :item="item" />
                  </template>
                  <template v-else-if="item.type === 'comment'">
                    <TimelineCommentGroup :item="item" />
                  </template>
                  <template v-else-if="item.type === 'reply'">
                    <TimelineCommentGroup :item="item" />
                  </template>
                  <template v-else-if="item.type === 'tag'">
                    <TimelineTagItem :item="item" />
                  </template>
                </template>
              </BaseTimeline>
              <BaseTimeline v-else :items="readPosts">
                <template #item="{ item }">
                  <TimelineReadItem :item="item" />
                </template>
              </BaseTimeline>
            </div>
          </div>

          <div v-else-if="selectedTab === 'following'" class="follow-container">
            <div class="follow-tabs">
              <div
                :class="['follow-tab-item', { selected: followTab === 'followers' }]"
                @click="followTab = 'followers'"
              >
                关注者
              </div>
              <div
                :class="['follow-tab-item', { selected: followTab === 'following' }]"
                @click="followTab = 'following'"
              >
                正在关注
              </div>
            </div>
            <div class="follow-list">
              <UserList v-if="followTab === 'followers'" :users="followers" />
              <UserList v-else :users="followings" />
            </div>
          </div>

          <div v-else-if="selectedTab === 'favorites'" class="favorites-container">
            <div v-if="favoritePosts.length > 0">
              <BaseTimeline :items="favoritePosts">
                <template #item="{ item }">
                  <NuxtLink :to="`/posts/${item.post.id}`" class="timeline-link">
                    {{ item.post.title }}
                  </NuxtLink>
                  <div class="timeline-snippet">
                    {{ stripMarkdown(item.post.snippet) }}
                  </div>
                  <div class="timeline-date">{{ formatDate(item.post.createdAt) }}</div>
                </template>
              </BaseTimeline>
            </div>
            <div v-else>
              <BasePlaceholder text="暂无收藏文章" icon="inbox" />
            </div>
          </div>

          <div v-else-if="selectedTab === 'achievements'" class="achievements-container">
            <AchievementList :medals="medals" :can-select="isMine" />
          </div>
        </template>
      </BaseTabs>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AchievementList from '~/components/AchievementList.vue'
import BasePlaceholder from '~/components/BasePlaceholder.vue'
import BaseTimeline from '~/components/BaseTimeline.vue'
import BaseTabs from '~/components/BaseTabs.vue'
import LevelProgress from '~/components/LevelProgress.vue'
import TimelineCommentGroup from '~/components/TimelineCommentGroup.vue'
import TimelinePostItem from '~/components/TimelinePostItem.vue'
import TimelineReadItem from '~/components/TimelineReadItem.vue'
import TimelineTagItem from '~/components/TimelineTagItem.vue'
import BaseUserAvatar from '~/components/BaseUserAvatar.vue'
import UserList from '~/components/UserList.vue'
import { toast } from '~/main'
import { authState, getToken } from '~/utils/auth'
import { prevLevelExp } from '~/utils/level'
import { stripMarkdown, stripMarkdownLength } from '~/utils/markdown'
import TimeManager from '~/utils/time'
const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

definePageMeta({
  alias: ['/users/:id/'],
  middleware: ['auth-required'],
})
const route = useRoute()
const username = route.params.id

const user = ref({})
const hotPosts = ref([])
const hotReplies = ref([])
const hotTags = ref([])
const favoritePosts = ref([])
const timelineItems = ref([])
const readPosts = ref([])
const timelineFilter = ref('all')
const filteredTimelineItems = computed(() => {
  if (timelineFilter.value === 'articles') {
    return timelineItems.value.filter((item) => item.type === 'post')
  } else if (timelineFilter.value === 'comments') {
    return timelineItems.value.filter((item) => item.type === 'comment' || item.type === 'reply')
  } else if (timelineFilter.value === 'reads') {
    return []
  }
  return timelineItems.value
})
const followers = ref([])
const followings = ref([])
const medals = ref([])
const subscribed = ref(false)
const isLoading = ref(true)
const tabLoading = ref(false)
const selectedTab = ref(
  ['summary', 'timeline', 'following', 'favorites', 'achievements'].includes(route.query.tab)
    ? route.query.tab
    : 'summary',
)
const tabs = [
  { key: 'summary', label: '总结', icon: 'chart-line' },
  { key: 'timeline', label: '时间线', icon: 'alarm-clock' },
  { key: 'following', label: '关注', icon: 'add-user' },
  { key: 'favorites', label: '收藏', icon: 'bookmark' },
  { key: 'achievements', label: '勋章', icon: 'medal-one' },
]
const followTab = ref('followers')

const levelInfo = computed(() => {
  const exp = user.value.experience || 0
  const currentLevel = user.value.currentLevel || 0
  const nextExp = user.value.nextLevelExp || 0
  const prevExp = prevLevelExp(currentLevel)
  const total = nextExp - prevExp
  const ratio = total > 0 ? (exp - prevExp) / total : 1
  const percent = Math.max(0, Math.min(1, ratio)) * 100
  return { exp, currentLevel, nextExp, percent }
})

const isMine = computed(function () {
  return authState.username === username || String(authState.userId) === username
})

const formatDate = (d) => {
  if (!d) return ''
  return TimeManager.format(d)
}

const authHeaders = () => {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const fetchUser = async () => {
  const res = await fetch(`${API_BASE_URL}/api/users/${username}`, { headers: authHeaders() })
  if (res.ok) {
    const data = await res.json()
    user.value = data
    subscribed.value = !!data.subscribed
  } else if (res.status === 404) {
    navigateTo('/404')
  }
}

const fetchSummary = async () => {
  const headers = authHeaders()
  const postsRes = await fetch(`${API_BASE_URL}/api/users/${username}/hot-posts`, { headers })
  if (postsRes.ok) {
    const data = await postsRes.json()
    hotPosts.value = data.map((p) => ({
      icon: 'file-text',
      type: 'post',
      post: p,
      createdAt: p.createdAt,
    }))
  }

  const repliesRes = await fetch(`${API_BASE_URL}/api/users/${username}/hot-replies`, { headers })
  if (repliesRes.ok) {
    const data = await repliesRes.json()
    hotReplies.value = data.map((c) => ({ icon: 'comment-icon', comment: c }))
  }

  const tagsRes = await fetch(`${API_BASE_URL}/api/users/${username}/hot-tags`, { headers })
  if (tagsRes.ok) {
    const data = await tagsRes.json()
    hotTags.value = data.map((t) => ({
      icon: 'tag-one',
      type: 'tag',
      tag: t,
      createdAt: t.createdAt,
    }))
  }
}

const isDiscussionItem = (item) => item && (item.type === 'comment' || item.type === 'reply')

const toDateKey = (value) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

const combineDiscussionItems = (items) => {
  const result = []
  items.forEach((item) => {
    if (!isDiscussionItem(item)) {
      result.push(item)
      return
    }

    const dateKey = toDateKey(item.createdAt)
    const last = result[result.length - 1]
    if (last && isDiscussionItem(last) && last.dateKey === dateKey) {
      last.entries.push({
        type: item.type,
        comment: item.comment,
        createdAt: item.createdAt,
      })
      if (item.type === 'comment' && last.type === 'reply') {
        last.type = 'comment'
      }
      if (new Date(item.createdAt) > new Date(last.createdAt)) {
        last.createdAt = item.createdAt
      }
    } else {
      result.push({
        type: item.type,
        icon: item.icon,
        createdAt: item.createdAt,
        dateKey,
        entries: [
          {
            type: item.type,
            comment: item.comment,
            createdAt: item.createdAt,
          },
        ],
      })
    }
  })
  return result
}

const fetchTimeline = async () => {
  const headers = authHeaders()
  const [postsRes, repliesRes, tagsRes] = await Promise.all([
    fetch(`${API_BASE_URL}/api/users/${username}/posts?limit=50`, { headers }),
    fetch(`${API_BASE_URL}/api/users/${username}/replies?limit=50`, { headers }),
    fetch(`${API_BASE_URL}/api/users/${username}/tags?limit=50`, { headers }),
  ])
  const posts = postsRes.ok ? await postsRes.json() : []
  const replies = repliesRes.ok ? await repliesRes.json() : []
  const tags = tagsRes.ok ? await tagsRes.json() : []
  const mapped = [
    ...posts.map((p) => ({
      type: 'post',
      icon: 'file-text',
      post: p,
      createdAt: p.createdAt,
    })),
    ...replies.map((r) => ({
      type: r.parentComment ? 'reply' : 'comment',
      icon: 'comment-icon',
      comment: r,
      createdAt: r.createdAt,
    })),
    ...tags.map((t) => ({
      type: 'tag',
      icon: 'tag-one',
      tag: t,
      createdAt: t.createdAt,
    })),
  ]
  mapped.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  timelineItems.value = combineDiscussionItems(mapped)
}

const fetchReadHistory = async () => {
  if (!isMine.value) {
    readPosts.value = []
    return
  }
  const token = getToken()
  if (!token) {
    readPosts.value = []
    return
  }
  const res = await fetch(`${API_BASE_URL}/api/users/${username}/read-posts?limit=50`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    const data = await res.json()
    readPosts.value = data.map((r) => ({ ...r, icon: 'file-text' }))
  } else {
    readPosts.value = []
  }
}

const fetchFollowUsers = async () => {
  const headers = authHeaders()
  const [followerRes, followingRes] = await Promise.all([
    fetch(`${API_BASE_URL}/api/users/${username}/followers`, { headers }),
    fetch(`${API_BASE_URL}/api/users/${username}/following`, { headers }),
  ])
  followers.value = followerRes.ok ? await followerRes.json() : []
  followings.value = followingRes.ok ? await followingRes.json() : []
}

const fetchFavorites = async () => {
  const res = await fetch(`${API_BASE_URL}/api/users/${username}/subscribed-posts`, {
    headers: authHeaders(),
  })
  if (res.ok) {
    const data = await res.json()
    favoritePosts.value = data.map((p) => ({ icon: 'bookmark', post: p }))
  } else {
    favoritePosts.value = []
  }
}

const loadSummary = async () => {
  tabLoading.value = true
  await fetchSummary()
  tabLoading.value = false
}

const loadTimeline = async () => {
  tabLoading.value = true
  await fetchTimeline()
  tabLoading.value = false
}

const loadReadHistory = async () => {
  tabLoading.value = true
  await fetchReadHistory()
  tabLoading.value = false
}

const loadFollow = async () => {
  tabLoading.value = true
  await fetchFollowUsers()
  tabLoading.value = false
}

const loadFavorites = async () => {
  tabLoading.value = true
  await fetchFavorites()
  tabLoading.value = false
}

const fetchAchievements = async () => {
  const res = await fetch(`${API_BASE_URL}/api/medals?userId=${user.value.id}`)
  if (res.ok) {
    medals.value = await res.json()
  } else {
    medals.value = []
    toast.error('获取成就失败')
  }
}

const loadAchievements = async () => {
  tabLoading.value = true
  await fetchAchievements()
  tabLoading.value = false
}

const subscribeUser = async () => {
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }
  const res = await fetch(`${API_BASE_URL}/api/subscriptions/users/${username}`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    subscribed.value = true
    toast.success('已关注')
  } else {
    toast.error('操作失败')
  }
}

const unsubscribeUser = async () => {
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }
  const res = await fetch(`${API_BASE_URL}/api/subscriptions/users/${username}`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    subscribed.value = false
    toast.success('已取消关注')
  } else {
    toast.error('操作失败')
  }
}

const sendMessage = async () => {
  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }
  try {
    const response = await fetch(`${API_BASE_URL}/api/messages/conversations`, {
      method: 'POST',
      body: JSON.stringify({
        recipientId: user.value.id,
      }),
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    })
    const result = await response.json()
    navigateTo(`/message-box/${result.conversationId}`)
  } catch (e) {
    toast.error('无法发起私信')
    console.error(e)
  }
}

const gotoTag = (tag) => {
  const value = encodeURIComponent(tag.id ?? tag.name)
  navigateTo({ path: '/', query: { tags: value } })
}

const init = async () => {
  try {
    await fetchUser()
    if (selectedTab.value === 'summary') {
      await loadSummary()
    } else if (selectedTab.value === 'timeline') {
      await loadTimeline()
    } else if (selectedTab.value === 'following') {
      await loadFollow()
    } else if (selectedTab.value === 'favorites') {
      await loadFavorites()
    } else if (selectedTab.value === 'achievements') {
      await loadAchievements()
    }
  } catch (e) {
    console.error(e)
  } finally {
    isLoading.value = false
  }
}

onMounted(init)

watch(selectedTab, async (val) => {
  // navigateTo({ query: { ...route.query, tab: val } }, { replace: true })
  if (val === 'timeline') {
    if (timelineFilter.value === 'reads') {
      if (readPosts.value.length === 0) {
        await loadReadHistory()
      }
    } else if (timelineItems.value.length === 0) {
      await loadTimeline()
    }
  } else if (val === 'following' && followers.value.length === 0 && followings.value.length === 0) {
    await loadFollow()
  } else if (val === 'favorites' && favoritePosts.value.length === 0) {
    await loadFavorites()
  } else if (val === 'achievements' && medals.value.length === 0) {
    await loadAchievements()
  }
})

watch(timelineFilter, async (val) => {
  if (selectedTab.value !== 'timeline') return
  if (val === 'reads') {
    if (readPosts.value.length === 0) {
      await loadReadHistory()
    }
  } else if (timelineItems.value.length === 0) {
    await loadTimeline()
  }
})

watch(isMine, (val) => {
  if (!val && timelineFilter.value === 'reads') {
    timelineFilter.value = 'all'
  }
})
</script>

<style scoped>
.loading-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.profile-page {
  background-color: var(--background-color);
}

.profile-page-header {
  display: flex;
  align-items: center;
  padding: 20px;
}

.profile-page-header-avatar-img {
  width: 200px;
  height: 200px;
  border-radius: 50%;
}

.profile-page-header-avatar-img :deep(.base-user-avatar-img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-page-header-user-info {
  margin-left: 20px;
}

.profile-page-header-user-info-name {
  font-size: 24px;
  font-weight: bold;
}

.profile-page-header-user-info-description {
  font-size: 20px;
  color: #666;
}

.reply-icon {
  color: var(--primary-color);
  margin-left: 5px;
}

.profile-page-header-user-info-buttons {
  display: flex;
  flex-direction: row;
  gap: 10px;
}

.profile-page-header-subscribe-button {
  display: flex;
  flex-direction: row;
  gap: 5px;
  align-items: center;
  font-size: 14px;
  border-radius: 8px;
  padding: 5px 10px;
  color: white;
  background-color: var(--primary-color);
  margin-top: 15px;
  width: fit-content;
  cursor: pointer;
}

.profile-page-header-subscribe-button:hover {
  background-color: var(--primary-color-hover);
}

.profile-page-header-unsubscribe-button {
  display: flex;
  flex-direction: row;
  gap: 5px;
  align-items: center;
  font-size: 14px;
  border-radius: 8px;
  padding: 5px 10px;
  color: var(--primary-color);
  background-color: var(--background-color);
  border: 1px solid var(--primary-color);
  margin-top: 15px;
  width: fit-content;
  cursor: pointer;
}

.profile-page-header-send-mail-button {
  display: flex;
  flex-direction: row;
  gap: 5px;
  align-items: center;
  font-size: 14px;
  border-radius: 8px;
  padding: 5px 10px;
  color: var(--primary-color);
  border: 1px solid var(--primary-color);
  margin-top: 15px;
  width: fit-content;
  cursor: pointer;
}

.profile-page-header-unsubscribe-button:hover,
.profile-page-header-send-mail-button:hover {
  background-color: var(--secondary-color-hover);
}

.profile-level-container {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 10px;
  font-size: 14px;
}

.profile-level-current {
  font-weight: bold;
}

.profile-level-bar {
  width: 200px;
  height: 8px;
  background-color: var(--normal-background-color);
  border-radius: 4px;
  overflow: hidden;
}

.profile-level-bar-inner {
  height: 100%;
  background-color: var(--primary-color);
}

.profile-level-info {
  display: flex;
  flex-direction: row;
  gap: 10px;
  align-items: center;
}

.profile-level-exp,
.profile-level-target {
  font-size: 12px;
  opacity: 0.8;
}

.profile-info {
  display: flex;
  flex-direction: row;
  padding: 0 20px;
  gap: 20px;
  border-top: 1px solid var(--normal-border-color);
  border-bottom: 1px solid var(--normal-border-color);
  scrollbar-width: none;
  overflow-x: auto;
}

.profile-info-item {
  display: flex;
  flex-direction: row;
  gap: 5px;
  align-items: center;
  padding: 10px 0;
  white-space: nowrap;
}

.profile-info-item-label {
  font-size: 14px;
  opacity: 0.7;
}

.profile-info-item-value {
  font-size: 14px;
}

:deep(.base-tabs-header) {
  position: sticky;
  top: calc(var(--header-height) + 1px);
  z-index: 200;
  background-color: var(--background-color-blur);
  display: flex;
  flex-direction: row;
  padding: 0 20px;
  border-bottom: 1px solid var(--normal-border-color);
  scrollbar-width: none;
  overflow-x: auto;
  backdrop-filter: var(--blur-10);
}

:deep(.base-tabs-item) {
  display: flex;
  flex: 0 0 auto;
  flex-direction: row;
  gap: 10px;
  align-items: center;
  justify-content: center;
  padding: 10px 0;
  width: 200px;
  cursor: pointer;
  white-space: nowrap;
}

:deep(.base-tabs-item.selected) {
  color: var(--primary-color);
  border-bottom: 2px solid var(--primary-color);
}

.profile-summary {
  display: flex;
  flex-direction: column;
  padding: 20px;
  row-gap: 40px; /* 行间距 */
  column-gap: 20px; /* 列间距 */
}

.summary-title {
  font-size: 20px;
  margin-bottom: 10px;
  font-weight: bold;
}

.total-summary {
  width: 100%;
}

.total-summary-content {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  row-gap: 0px;
  column-gap: 20px;
}

.total-summary-item {
  display: flex;
  flex-direction: row;
  gap: 10px;
  align-items: center;
}

.total-summary-item-label {
  font-size: 18px;
  opacity: 0.7;
}

.total-summary-item-value {
  font-size: 24px;
  font-weight: bold;
}

.summary-divider {
  display: flex;
  flex-direction: row;
  row-gap: 40px; /* 行间距 */
  column-gap: 20px; /* 列间距 */
  width: 100%;
  flex-wrap: wrap;
}

.hot-reply,
.hot-topic,
.hot-tag {
  width: 40%;
}

.timeline-tabs {
  display: flex;
  flex-direction: row;
  border-bottom: 1px solid var(--normal-border-color);
}

.timeline-list {
  padding: 10px 20px;
}

.timeline-tab-item {
  padding: 10px 20px;
  cursor: pointer;
}

.timeline-tab-item.selected {
  color: var(--primary-color);
  border-bottom: 2px solid var(--primary-color);
}

.timeline-date {
  font-size: 12px;
  color: gray;
  margin-top: 5px;
  white-space: nowrap;
}

.timeline-snippet {
  font-size: 14px;
  color: #666;
  margin-top: 5px;
}

.timeline-link {
  font-weight: bold;
  text-decoration: none;
  color: var(--text-color);
  word-break: break-word;
}

.timeline-link:hover {
  text-decoration: underline;
}

.summary-empty {
  margin-top: 10px;
  font-size: 14px;
  opacity: 0.5;
}

.summary-content {
  margin-top: 10px;
}

.tab-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 0;
}

.ttimeline-container {
  margin-top: 2px;
  padding-bottom: 30px;
}

.timeline-header {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}

.tags-container {
  display: flex;
  flex-direction: row;
  gap: 10px;
  padding-top: 5px;
  justify-content: space-between;
  align-items: center;
}

.tags-container-item {
  display: flex;
  flex-direction: row;
  gap: 5px;
  align-items: center;
}

.timeline-tag-title {
  font-size: 16px;
  font-weight: 600;
}

.comment-content {
  display: flex;
  flex-direction: column;
  margin-top: 10px;
  gap: 5px;
}

.comment-content-item-main {
  display: flex;
  flex-direction: row;
  gap: 5px;
  align-items: flex-start;
}

.comment-content-item-icon {
  width: 20px;
  height: 25px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.comment-content-item {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  justify-content: space-between;
}

.timeline-comment-link {
  color: var(--text-color);
  word-break: break-word;
  text-decoration: underline;
  margin-left: 5px;
}

.timeline-comment-link:hover {
  color: var(--primary-color);
}

.timeline-article-link {
  color: var(--text-color);
  font-weight: bold;
  font-size: 20px;
  word-break: break-word;
  text-decoration: underline;
}

.timeline-article-link:hover {
  color: var(--primary-color);
}

.article-container {
  padding: 20px;
  border-radius: 10px;
  border: 1px solid var(--normal-border-color);
  margin-top: 10px;
}

.follow-container {
}

.favorites-container {
  padding: 10px;
}

.follow-tabs {
  display: flex;
  flex-direction: row;
  border-bottom: 1px solid var(--normal-border-color);
  margin-bottom: 10px;
}

.follow-tab-item {
  padding: 10px 20px;
  cursor: pointer;
}

.follow-tab-item.selected {
  color: var(--primary-color);
  border-bottom: 2px solid var(--primary-color);
}

.follow-list {
  padding-left: 20px;
}

@media (max-width: 768px) {
  .profile-page {
    width: 100vw;
  }

  .profile-page-header-avatar-img {
    width: 100px;
    height: 100px;
    border-radius: 50%;
  }

  :deep(.base-tabs-item) {
    width: 100px;
  }

  .summary-divider {
    flex-direction: column;
  }

  .hot-reply,
  .hot-topic,
  .hot-tag {
    width: 100%;
  }
}
</style>
