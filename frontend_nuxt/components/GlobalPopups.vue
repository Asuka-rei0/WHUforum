<template>
  <div>
    <ActivityPopup
      :visible="showMilkTeaPopup"
      :icon="milkTeaIcon"
      text="建站送奶茶活动火热进行中，快来参与吧！"
      @close="closeMilkTeaPopup"
    />
    <NotificationSettingPopup :visible="showNotificationPopup" @close="closeNotificationPopup" />
    <MessagePopup :visible="showMessagePopup" @close="closeMessagePopup" />
    <MedalPopup :visible="showMedalPopup" :medals="newMedals" @close="closeMedalPopup" />

    <ActivityPopup
      :visible="showInviteCodePopup"
      :icon="inviteCodeIcon"
      text="邀请码活动开始了，速来参与大伙们🔥🔥🔥"
      @close="closeInviteCodePopup"
    />
  </div>
</template>

<script setup>
import ActivityPopup from '~/components/ActivityPopup.vue'
import MedalPopup from '~/components/MedalPopup.vue'
import NotificationSettingPopup from '~/components/NotificationSettingPopup.vue'
import MessagePopup from '~/components/MessagePopup.vue'
import { authState } from '~/utils/auth'

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const showMilkTeaPopup = ref(false)
const showInviteCodePopup = ref(false)
const milkTeaIcon = ref('')
const inviteCodeIcon = ref('')

const showNotificationPopup = ref(false)
const showMessagePopup = ref(false)
const showMedalPopup = ref(false)
const newMedals = ref([])

onMounted(async () => {
  await checkMilkTeaActivity()
  if (showMilkTeaPopup.value) return

  await checkInviteCodeActivity()
  if (showInviteCodePopup.value) return

  await checkMessageFeature()
  if (showMessagePopup.value) return

  await checkNotificationSetting()
  if (showNotificationPopup.value) return

  await checkNewMedals()
})

const checkMilkTeaActivity = async () => {
  if (!import.meta.client) return
  if (localStorage.getItem('milkTeaActivityPopupShown')) return
  try {
    const res = await fetch(`${API_BASE_URL}/api/activities`)
    if (res.ok) {
      const list = await res.json()
      const a = list.find((i) => i.type === 'MILK_TEA' && !i.ended)
      if (a) {
        milkTeaIcon.value = a.icon
        showMilkTeaPopup.value = true
      }
    }
  } catch (e) {
    // ignore network errors
  }
}

const checkInviteCodeActivity = async () => {
  if (!import.meta.client) return
  if (localStorage.getItem('inviteCodeActivityPopupShown')) return
  try {
    const res = await fetch(`${API_BASE_URL}/api/activities`)
    if (res.ok) {
      const list = await res.json()
      const a = list.find((i) => i.type === 'INVITE_POINTS' && !i.ended)
      if (a) {
        inviteCodeIcon.value = a.icon
        showInviteCodePopup.value = true
      }
    }
  } catch (e) {
    // ignore network errors
  }
}

const closeInviteCodePopup = () => {
  if (!import.meta.client) return
  localStorage.setItem('inviteCodeActivityPopupShown', 'true')
  showInviteCodePopup.value = false
}

const closeMilkTeaPopup = () => {
  if (!import.meta.client) return
  localStorage.setItem('milkTeaActivityPopupShown', 'true')
  showMilkTeaPopup.value = false
}

const checkMessageFeature = async () => {
  if (!import.meta.client) return
  if (!authState.loggedIn) return
  if (localStorage.getItem('messageFeaturePopupShown')) return
  showMessagePopup.value = true
}
const closeMessagePopup = () => {
  if (!import.meta.client) return
  localStorage.setItem('messageFeaturePopupShown', 'true')
  showMessagePopup.value = false
}

const checkNotificationSetting = async () => {
  if (!import.meta.client) return
  if (!authState.loggedIn) return
  if (localStorage.getItem('notificationSettingPopupShown')) return
  showNotificationPopup.value = true
}
const closeNotificationPopup = () => {
  if (!import.meta.client) return
  localStorage.setItem('notificationSettingPopupShown', 'true')
  showNotificationPopup.value = false
}
const checkNewMedals = async () => {
  if (!import.meta.client) return
  if (!authState.loggedIn || !authState.userId) return
  try {
    const res = await fetch(`${API_BASE_URL}/api/medals?userId=${authState.userId}`)
    if (res.ok) {
      const medals = await res.json()
      const seen = JSON.parse(localStorage.getItem('seenMedals') || '[]')
      const m = medals.filter((i) => i.completed && !seen.includes(i.type))
      if (m.length > 0) {
        newMedals.value = m
        showMedalPopup.value = true
      }
    }
  } catch (e) {
    // ignore errors
  }
}
const closeMedalPopup = () => {
  if (!import.meta.client) return
  const seen = new Set(JSON.parse(localStorage.getItem('seenMedals') || '[]'))
  newMedals.value.forEach((m) => seen.add(m.type))
  localStorage.setItem('seenMedals', JSON.stringify([...seen]))
  showMedalPopup.value = false
}
</script>
