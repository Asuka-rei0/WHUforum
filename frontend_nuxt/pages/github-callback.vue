<!-- pages/github-callback.vue -->
<template>
  <CallbackPage />
</template>

<script setup>
import CallbackPage from '~/components/CallbackPage.vue'
import { githubExchange } from '~/utils/github'

onMounted(async () => {
  const url = new URL(window.location.href)
  const code = url.searchParams.get('code') || ''
  const state = url.searchParams.get('state') || ''

  // 从 state 中解析 invite_token（githubAuthorize 已把它放进 state）
  let inviteToken = ''
  if (state) {
    try {
      const s = new URLSearchParams(state)
      inviteToken = s.get('invite_token') || s.get('invitetoken') || ''
    } catch {}
  }
  // 兜底：也支持直接跟在回调URL的查询参数上
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

  const result = await githubExchange(code, inviteToken, '')

  if (result.needReason) {
    navigateTo(`/signup-reason?token=${result.token}`, { replace: true })
  } else {
    navigateTo('/', { replace: true })
  }
})
</script>
