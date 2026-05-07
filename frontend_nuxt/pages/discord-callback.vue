<!-- pages/discord-callback.vue -->
<template>
  <CallbackPage />
</template>

<script setup>
import CallbackPage from '~/components/CallbackPage.vue'
import { discordExchange } from '~/utils/discord'

onMounted(async () => {
  const url = new URL(window.location.href)
  const code = url.searchParams.get('code') || ''
  const stateStr = url.searchParams.get('state') || ''

  // 从 state 解析 invite_token；兜底支持 query ?invite_token=
  let inviteToken = ''
  if (stateStr) {
    try {
      const s = new URLSearchParams(stateStr)
      inviteToken = s.get('invite_token') || s.get('invitetoken') || ''
    } catch {}
  }
  // if (!inviteToken) {
  //   inviteToken =
  //     url.searchParams.get('invite_token') ||
  //     url.searchParams.get('invitetoken') ||
  //     ''
  // }

  if (!code) {
    navigateTo('/login', { replace: true })
    return
  }

  const result = await discordExchange(code, inviteToken, '')

  if (result.needReason) {
    navigateTo(`/signup-reason?token=${result.token}`, { replace: true })
  } else {
    navigateTo('/', { replace: true })
  }
})
</script>
