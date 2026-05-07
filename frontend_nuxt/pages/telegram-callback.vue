<template>
  <CallbackPage />
</template>

<script setup>
import CallbackPage from '~/components/CallbackPage.vue'
import { telegramExchange } from '~/utils/telegram'

onMounted(async () => {
  const url = new URL(window.location.href)
  const inviteToken =
    url.searchParams.get('invite_token') || url.searchParams.get('invitetoken') || ''
  const hash = url.hash.startsWith('#tgAuthResult=') ? url.hash.slice('#tgAuthResult='.length) : ''
  if (!hash) {
    navigateTo('/login', { replace: true })
    return
  }
  let authData
  try {
    const decoded = atob(hash)
    const parsed = JSON.parse(decoded)
    authData = {
      id: String(parsed.id),
      firstName: parsed.first_name,
      lastName: parsed.last_name,
      username: parsed.username,
      photoUrl: parsed.photo_url,
      authDate: parsed.auth_date,
      hash: parsed.hash,
    }
  } catch (e) {
    navigateTo('/login', { replace: true })
    return
  }
  const result = await telegramExchange(authData, inviteToken, '')
  if (result.needReason) {
    navigateTo(`/signup-reason?token=${result.token}`, { replace: true })
  } else {
    navigateTo('/', { replace: true })
  }
})
</script>
