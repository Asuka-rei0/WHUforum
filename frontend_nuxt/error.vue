<template>
  <div class="error-page">
    <header v-if="!isNotFound" class="error-header">
      <NuxtLink class="error-brand" to="/">
        <img src="/whu-emblem.webp" alt="珞珈论坛" />
        <span>珞珈论坛</span>
      </NuxtLink>
    </header>

    <NotFoundPanel v-if="isNotFound" standalone />
    <div v-else class="error-fallback">
      <img class="error-emblem" src="/whu-emblem.webp" alt="珞珈论坛" />
      <h1>出了点问题</h1>
      <p>{{ errorMessage }}</p>
      <div class="error-actions">
        <button class="primary-link" type="button" @click="handleHome">返回首页</button>
        <button class="secondary-link" type="button" @click="handleRetry">重试</button>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  error: {
    type: Object,
    required: true,
  },
})

const isNotFound = computed(() => props.error?.statusCode === 404)

const errorMessage = computed(() => {
  const message = props.error?.statusMessage || props.error?.message
  if (message && message !== 'Page not found') return message
  return '页面暂时无法加载，请稍后再试。'
})

useHead({
  title: isNotFound.value ? '页面不存在 - 珞珈论坛' : '出错了 - 珞珈论坛',
})

const handleHome = () => clearError({ redirect: '/' })
const handleRetry = () => clearError()
</script>

<style scoped>
.error-page {
  background: var(--login-background-color);
  color: var(--text-color);
  min-height: 100vh;
}

.error-header {
  align-items: center;
  border-bottom: 1px solid var(--normal-border-color);
  display: flex;
  height: var(--header-height);
  justify-content: center;
  padding: 0 18px;
}

.error-brand {
  align-items: center;
  color: var(--text-color);
  display: inline-flex;
  font-size: 18px;
  font-weight: 800;
  gap: 10px;
  text-decoration: none;
}

.error-brand img {
  height: 32px;
  width: 32px;
}

.error-fallback {
  align-items: center;
  display: flex;
  flex-direction: column;
  margin: 0 auto;
  max-width: 520px;
  padding: 72px 18px 48px;
  text-align: center;
}

.error-emblem {
  height: 72px;
  margin-bottom: 16px;
  width: 72px;
}

.error-fallback h1 {
  font-size: 28px;
  margin: 0 0 10px;
}

.error-fallback p {
  color: var(--menu-text-color);
  line-height: 1.7;
  margin: 0;
}

.error-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  margin-top: 28px;
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
}

.primary-link {
  background: #005c46;
  border: none;
  color: #fff;
}

.secondary-link {
  background: transparent;
  border: 1px solid var(--normal-border-color);
  color: var(--text-color);
}
</style>
