<template>
  <div class="point-mall-page">
    <BaseTabs v-model="selectedTab" :tabs="tabs">
      <template v-if="selectedTab === 'mall'">
        <div class="point-mall-page-content">
          <section class="rules">
            <div class="section-title">🎉 积分规则</div>
            <div class="section-content">
              <div class="section-item" v-for="(rule, idx) in pointRules" :key="idx">
                {{ rule }}
              </div>
            </div>
          </section>

          <section class="trend" v-if="trendOption">
            <div class="section-title">积分走势</div>
            <ClientOnly>
              <VChart :option="trendOption" :autoresize="true" style="height: 300px" />
            </ClientOnly>
          </section>

          <div class="loading-points-container" v-if="isLoading">
            <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
          </div>

          <div class="point-info">
            <p v-if="authState.loggedIn && point !== null">
              <span><paper-money-two class="coin-icon" /></span>我的积分：<span
                class="point-value"
                >{{ point }}</span
              >
            </p>
          </div>

          <section class="goods">
            <div class="goods-item" v-for="(good, idx) in displayGoods" :key="idx">
              <BaseImage class="goods-item-image" :src="good.image" :alt="good.name" />
              <div class="goods-item-name">{{ good.name }}</div>
              <div class="goods-item-cost">
                <paper-money-two />
                {{ good.cost }} 积分
              </div>
              <div
                class="goods-item-button"
                :class="{ disabled: !good.id || !authState.loggedIn || point === null || point < good.cost }"
                @click="openRedeem(good)"
              >
                兑换
              </div>
            </div>
          </section>
          <RedeemPopup
            :visible="dialogVisible"
            v-model="contact"
            :loading="loading"
            @close="closeRedeem"
            @submit="submitRedeem"
          />
        </div>
      </template>

      <template v-else>
        <div class="loading-points-container" v-if="historyLoading">
          <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
        </div>
        <BasePlaceholder v-else-if="histories.length === 0" text="暂无积分记录" icon="inbox" />
        <div class="timeline-container" v-else>
          <BaseTimeline :items="histories">
            <template #item="{ item }">
              <div class="history-content">
                <template v-if="item.type === 'POST'">
                  发送帖子
                  <NuxtLink :to="`/posts/${item.postId}`" class="timeline-link">{{
                    item.postTitle
                  }}</NuxtLink>
                  ，获得{{ item.amount }}积分
                </template>
                <template v-else-if="item.type === 'COMMENT'">
                  在文章
                  <NuxtLink :to="`/posts/${item.postId}`" class="timeline-link">{{
                    item.postTitle
                  }}</NuxtLink>
                  中
                  <template v-if="!item.fromUserId">
                    发送评论
                    <NuxtLink
                      :to="`/posts/${item.postId}#comment-${item.commentId}`"
                      class="timeline-link"
                      >{{ stripMarkdownLength(item.commentContent, 100) }}</NuxtLink
                    >
                    ，获得{{ item.amount }}积分
                  </template>
                  <template v-else>
                    被评论
                    <NuxtLink
                      :to="`/posts/${item.postId}#comment-${item.commentId}`"
                      class="timeline-link"
                      >{{ stripMarkdownLength(item.commentContent, 100) }}</NuxtLink
                    >
                    ，获得{{ item.amount }}积分
                  </template>
                </template>
                <template v-else-if="item.type === 'POST_LIKE_CANCELLED' && item.fromUserId">
                  你的帖子
                  <NuxtLink :to="`/posts/${item.postId}`" class="timeline-link">
                    {{ item.postTitle }}
                  </NuxtLink>
                  被
                  <NuxtLink :to="`/users/${item.fromUserId}`" class="timeline-link">
                    {{ item.fromUserName }}
                  </NuxtLink>
                  取消点赞，扣除{{ -item.amount }}积分
                </template>
                <template v-else-if="item.type === 'COMMENT_LIKE_CANCELLED' && item.fromUserId">
                  你的评论
                  <NuxtLink
                    :to="`/posts/${item.postId}#comment-${item.commentId}`"
                    class="timeline-link"
                  >
                    {{ stripMarkdownLength(item.commentContent, 100) }}
                  </NuxtLink>
                  被
                  <NuxtLink :to="`/users/${item.fromUserId}`" class="timeline-link">
                    {{ item.fromUserName }}
                  </NuxtLink>
                  取消点赞，扣除{{ -item.amount }}积分
                </template>
                <template v-else-if="item.type === 'POST_LIKED' && item.fromUserId">
                  帖子
                  <NuxtLink :to="`/posts/${item.postId}`" class="timeline-link">{{
                    item.postTitle
                  }}</NuxtLink>
                  被
                  <NuxtLink :to="`/users/${item.fromUserId}`" class="timeline-link">{{
                    item.fromUserName
                  }}</NuxtLink>
                  按赞，获得{{ item.amount }}积分
                </template>
                <template v-else-if="item.type === 'COMMENT_LIKED' && item.fromUserId">
                  评论
                  <NuxtLink
                    :to="`/posts/${item.postId}#comment-${item.commentId}`"
                    class="timeline-link"
                    >{{ stripMarkdownLength(item.commentContent, 100) }}</NuxtLink
                  >
                  被
                  <NuxtLink :to="`/users/${item.fromUserId}`" class="timeline-link">{{
                    item.fromUserName
                  }}</NuxtLink>
                  按赞，获得{{ item.amount }}积分
                </template>
                <template v-else-if="item.type === 'INVITE' && item.fromUserId">
                  社区奖励
                  <NuxtLink :to="`/users/${item.fromUserId}`" class="timeline-link">{{
                    item.fromUserName
                  }}</NuxtLink>
                  加入社区，获得 {{ item.amount }} 积分
                </template>
                <template v-else-if="item.type === 'FEATURE'">
                  文章
                  <NuxtLink :to="`/posts/${item.postId}`" class="timeline-link">{{
                    item.postTitle
                  }}</NuxtLink>
                  被收录为精选，获得 {{ item.amount }} 积分
                </template>
                <template v-else-if="item.type === 'REDEEM'">
                  兑换商品，消耗 {{ -item.amount }} 积分
                </template>
                <template v-else-if="item.type === 'LOTTERY_JOIN'">
                  参与抽奖帖
                  <NuxtLink :to="`/posts/${item.postId}`" class="timeline-link">{{
                    item.postTitle
                  }}</NuxtLink>
                  ，消耗 {{ -item.amount }} 积分
                </template>
                <template v-else-if="item.type === 'LOTTERY_REWARD'">
                  你的抽奖帖
                  <NuxtLink :to="`/posts/${item.postId}`" class="timeline-link">{{
                    item.postTitle
                  }}</NuxtLink>
                  被
                  <NuxtLink :to="`/users/${item.fromUserId}`" class="timeline-link">{{
                    item.fromUserName
                  }}</NuxtLink>
                  参与，获得 {{ item.amount }} 积分
                </template>
                <template v-else-if="item.type === 'DONATE_SENT'">
                  你在文章
                  <NuxtLink :to="`/posts/${item.postId}`" class="timeline-link">{{
                    item.postTitle
                  }}</NuxtLink>
                  中打赏了
                  <NuxtLink :to="`/users/${item.fromUserId}`" class="timeline-link">{{
                    item.fromUserName
                  }}</NuxtLink>
                  ，消耗 {{ -item.amount }} 积分
                </template>
                <template v-else-if="item.type === 'DONATE_RECEIVED'">
                  <NuxtLink :to="`/users/${item.fromUserId}`" class="timeline-link">{{
                    item.fromUserName
                  }}</NuxtLink>
                  在文章
                  <NuxtLink :to="`/posts/${item.postId}`" class="timeline-link">{{
                    item.postTitle
                  }}</NuxtLink>
                  中打赏了你，获得 {{ item.amount }} 积分
                </template>
                <template v-else-if="item.type === 'SYSTEM_ONLINE'"> 积分历史系统上线 </template>
                <paper-money-two /> 你目前的积分是 {{ item.balance }}
              </div>
              <div class="history-time">{{ TimeManager.format(item.createdAt) }}</div>
            </template>
          </BaseTimeline>
        </div>
      </template>
    </BaseTabs>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { authState, fetchCurrentUser, getToken } from '~/utils/auth'
import { toast } from '~/main'
import RedeemPopup from '~/components/RedeemPopup.vue'
import BaseTimeline from '~/components/BaseTimeline.vue'
import BasePlaceholder from '~/components/BasePlaceholder.vue'
import { stripMarkdownLength } from '~/utils/markdown'
import TimeManager from '~/utils/time'
import BaseTabs from '~/components/BaseTabs.vue'

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const selectedTab = ref('mall')
const tabs = [
  { key: 'mall', label: '积分兑换' },
  { key: 'history', label: '积分历史' },
]
const point = ref(null)
const isLoading = ref(false)
const histories = ref([])
const historyLoading = ref(false)
const historyLoaded = ref(false)
const trendOption = ref(null)

const pointRules = [
  '发帖：每天前两次，每次 30 积分',
  '评论：每天前四条评论可获 10 积分，你的帖子被评论也可获 10 积分',
  '帖子被点赞：每次 10 积分',
  '评论被点赞：每次 10 积分',
  '文章被收录至精选：每次 500 积分',
]

const goods = ref([])
const dialogVisible = ref(false)
const contact = ref('')
const loading = ref(false)
const selectedGood = ref(null)

const iconMap = {
  POST: 'file-text',
  COMMENT: 'comment-icon',
  POST_LIKED: 'like',
  COMMENT_LIKED: 'like',
  INVITE: 'add-user',
  SYSTEM_ONLINE: 'history-icon',
  REDEEM: 'gift',
  FEATURE: 'star',
  LOTTERY_JOIN: 'medal-one',
  LOTTERY_REWARD: 'fireworks',
  DONATE_SENT: 'paper-money-two',
  DONATE_RECEIVED: 'paper-money-two',
  POST_LIKE_CANCELLED: 'clear-icon',
  COMMENT_LIKE_CANCELLED: 'clear-icon',
}

const campusGoods = [
  {
    name: '校园网网费 1 个月',
    cost: 3000,
    image: 'https://img.icons8.com/color/240/wifi--v1.png',
  },
  {
    name: '食堂餐券 20 元',
    cost: 2000,
    image: 'https://img.icons8.com/color/240/meal.png',
  },
  {
    name: '图书馆打印券 50 页',
    cost: 1200,
    image: 'https://img.icons8.com/color/240/print.png',
  },
]

const legacyGoodNames = new Set(['GPT Plus 1 个月', '奶茶'])
const displayGoods = computed(() => {
  const visibleGoods = goods.value.filter((good) => !legacyGoodNames.has(good.name))
  if (visibleGoods.length > 0) return visibleGoods
  return campusGoods.map((good, index) => ({ id: null, ...good, fallbackKey: `campus-${index}` }))
})

const loadTrend = async () => {
  if (!authState.loggedIn) return
  const token = getToken()
  const res = await fetch(`${API_BASE_URL}/api/point-histories/trend?days=30`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    const data = await res.json()
    const dates = data.map((d) => d.date)
    const values = data.map((d) => d.value)
    trendOption.value = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: dates, boundaryGap: false },
      yAxis: { type: 'value' },
      series: [{ type: 'line', areaStyle: {}, smooth: true, data: values }],
      dataZoom: [{ type: 'slider', start: 80 }, { type: 'inside' }],
    }
  }
}

onMounted(async () => {
  isLoading.value = true
  if (authState.loggedIn) {
    const user = await fetchCurrentUser()
    point.value = user ? user.point : null
    await Promise.all([loadGoods(), loadTrend()])
  } else {
    await loadGoods()
  }
  isLoading.value = false
})

watch(selectedTab, (val) => {
  if (val === 'history' && !historyLoaded.value) {
    loadHistory()
  }
})

const loadGoods = async () => {
  const res = await fetch(`${API_BASE_URL}/api/point-goods`)
  if (res.ok) {
    goods.value = await res.json()
  }
}

const loadHistory = async () => {
  if (!authState.loggedIn) {
    historyLoaded.value = true
    return
  }
  historyLoading.value = true
  const token = getToken()
  const res = await fetch(`${API_BASE_URL}/api/point-histories`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.ok) {
    histories.value = (await res.json()).map((item) => ({
      ...item,
      icon: iconMap[item.type],
    }))
  }
  historyLoading.value = false
  historyLoaded.value = true
}

const openRedeem = (good) => {
  if (!good.id) {
    toast.error('该奖品暂未开放兑换')
    return
  }
  if (!authState.loggedIn || point.value === null || point.value < good.cost) {
    toast.error('积分不足')
    return
  }
  selectedGood.value = good
  dialogVisible.value = true
}

const closeRedeem = () => {
  dialogVisible.value = false
}

const submitRedeem = async () => {
  if (!selectedGood.value || !contact.value) return
  loading.value = true
  const token = getToken()
  const res = await fetch(`${API_BASE_URL}/api/point-goods/redeem`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ goodId: selectedGood.value.id, contact: contact.value }),
  })
  if (res.ok) {
    const data = await res.json()
    point.value = data.point
    toast.success('兑换成功！')
    dialogVisible.value = false
    contact.value = ''
  } else {
    toast.error('兑换失败')
  }
  loading.value = false
}
</script>

<style scoped>
.point-mall-page {
  max-width: var(--page-max-width);
  background-color: var(--background-color);
  margin: 0 auto;
}

.point-mall-page-content {
  padding: 0 20px;
}

:deep(.base-tabs-header) {
  display: flex;
  border-bottom: 1px solid var(--normal-border-color);
}

:deep(.base-tabs-item) {
  padding: 10px 15px;
  cursor: pointer;
}

:deep(.base-tabs-item.selected) {
  border-bottom: 2px solid var(--primary-color);
  color: var(--primary-color);
}

.timeline-container {
  padding: 10px 20px;
}

.timeline-link {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: bold;
}

.timeline-link:hover {
  text-decoration: underline;
}

.loading-points-container {
  margin-top: 100px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.point-info {
  font-size: 18px;
}

.point-value {
  font-weight: bold;
  color: var(--primary-color);
}

.coin-icon {
  margin-right: 5px;
}

.rules,
.goods,
.trend {
  margin-top: 20px;
}

.goods {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.goods-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  border: 1px solid var(--normal-border-color);
  border-radius: 10px;
  overflow: hidden;
}

.goods-item-name {
  font-size: 20px;
  font-weight: bold;
}

.goods-item-image {
  width: 200px;
  height: 200px;
  border-bottom: 1px solid var(--normal-border-color);
  object-fit: contain;
  background: var(--background-color);
}

.goods-item-cost {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  opacity: 0.7;
}

.goods-item-button {
  background-color: var(--primary-color);
  color: white;
  padding: 7px 10px;
  border-radius: 10px;
  width: calc(100% - 40px);
  text-align: center;
  cursor: pointer;
  margin-bottom: 10px;
}

.goods-item-button:hover {
  background-color: var(--primary-color-hover);
}

.goods-item-button.disabled,
.goods-item-button.disabled:hover {
  background-color: var(--primary-color-disabled);
  cursor: not-allowed;
}

.history-content {
  font-size: 14px;
  opacity: 0.8;
}

.history-time {
  font-size: 12px;
  color: var(--text-color);
  opacity: 0.7;
}

.section-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
}

.section-content {
  display: flex;
  flex-direction: column;
  font-size: 14px;
  opacity: 0.7;
}
</style>
