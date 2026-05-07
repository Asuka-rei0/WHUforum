<template>
  <CallbackPage />
</template>

<script setup>
import CallbackPage from '~/components/CallbackPage.vue'
import { twitterExchange } from '~/utils/twitter'

onMounted(async () => {
  const url = new URL(window.location.href)
  const code = url.searchParams.get('code')
  const state = url.searchParams.get('state')
  const result = await twitterExchange(code, state, '')

  if (result.needReason) {
    navigateTo(`/signup-reason?token=${result.token}`, { replace: true })
  } else {
    navigateTo('/', { replace: true })
  }
})
</script>
