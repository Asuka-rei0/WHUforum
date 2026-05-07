<template>
  <CallbackPage />
</template>

<script setup>
import CallbackPage from '~/components/CallbackPage.vue'
import { googleAuthWithToken } from '~/utils/google'

onMounted(async () => {
  const hash = new URLSearchParams(window.location.hash.substring(1))
  const idToken = hash.get('id_token')

  // 优先从 state 中解析
  let inviteToken = ''
  const stateStr = hash.get('state') || ''
  if (stateStr) {
    const state = new URLSearchParams(stateStr)
    inviteToken = state.get('invite_token') || ''
  }

  // 兜底：如果之前把 invite_token 放在回调 URL 的查询参数中
  // if (!inviteToken) {
  //   const query = new URLSearchParams(window.location.search)
  //   inviteToken = query.get('invite_token') || ''
  // }

  if (idToken) {
    await googleAuthWithToken(
      idToken,
      () => {
        navigateTo('/', { replace: true })
      },
      (token) => {
        navigateTo(`/signup-reason?token=${token}`, { replace: true })
      },
      { inviteToken },
    )
  } else {
    navigateTo('/login', { replace: true })
  }
})
</script>
