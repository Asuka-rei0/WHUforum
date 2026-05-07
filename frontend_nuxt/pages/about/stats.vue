<template>
  <div class="site-stats-page">
    <div v-if="isLoading" class="loading-message">
      <l-hatch size="28" stroke="4" speed="3.5" color="var(--primary-color)"></l-hatch>
    </div>
    <ClientOnly>
      <VChart
        v-if="dauOption"
        :option="dauOption"
        :autoresize="true"
        style="height: 400px; margin-bottom: 30px"
      />
      <VChart
        v-if="newUserOption"
        :option="newUserOption"
        :autoresize="true"
        style="height: 400px; margin-bottom: 30px"
      />
      <VChart
        v-if="postOption"
        :option="postOption"
        :autoresize="true"
        style="height: 400px; margin-bottom: 30px"
      />
      <VChart
        v-if="commentOption"
        :option="commentOption"
        :autoresize="true"
        style="height: 400px"
      />
    </ClientOnly>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getToken } from '~/utils/auth'
const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const dauOption = ref(null)
const newUserOption = ref(null)
const postOption = ref(null)
const commentOption = ref(null)
const isLoading = ref(false)

async function loadData() {
  isLoading.value = true
  const token = getToken()
  const headers = { Authorization: `Bearer ${token}` }

  const [dauRes, newUserRes, postRes, commentRes] = await Promise.all([
    fetch(`${API_BASE_URL}/api/stats/dau-range?days=30`, { headers }),
    fetch(`${API_BASE_URL}/api/stats/new-users-range?days=30`, { headers }),
    fetch(`${API_BASE_URL}/api/stats/posts-range?days=30`, { headers }),
    fetch(`${API_BASE_URL}/api/stats/comments-range?days=30`, { headers }),
  ])

  function toOption(title, data) {
    data.sort((a, b) => new Date(a.date) - new Date(b.date))
    const dates = data.map((d) => d.date)
    const values = data.map((d) => d.value)
    return {
      title: { text: title },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: dates },
      yAxis: { type: 'value' },
      dataZoom: [{ type: 'slider', start: 80 }, { type: 'inside' }],
      series: [{ type: 'line', areaStyle: {}, smooth: true, data: values }],
    }
  }

  if (dauRes.ok) {
    const data = await dauRes.json()
    dauOption.value = toOption('站点 DAU', data)
  }
  if (newUserRes.ok) {
    const data = await newUserRes.json()
    newUserOption.value = toOption('每日新增用户', data)
  }
  if (postRes.ok) {
    const data = await postRes.json()
    postOption.value = toOption('每日发帖量', data)
  }
  if (commentRes.ok) {
    const data = await commentRes.json()
    commentOption.value = toOption('每日回贴量', data)
  }
  isLoading.value = false
}

onMounted(loadData)
</script>

<style scoped>
.site-stats-page {
  padding: 20px;
  max-width: var(--page-max-width);
  background-color: var(--background-color);
  margin: 0 auto;
}

.loading-message {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
}
</style>
