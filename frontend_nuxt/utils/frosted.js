import { reactive } from 'vue'

const FROSTED_KEY = 'frosted-glass'

export const frostedState = reactive({
  enabled: true,
})

function apply() {
  if (!import.meta.client) return
  document.documentElement.dataset.frosted = frostedState.enabled ? 'on' : 'off'
}

export function initFrosted() {
  if (!import.meta.client) return
  const saved = localStorage.getItem(FROSTED_KEY)
  frostedState.enabled = saved !== 'false'
  apply()
}

export function setFrosted(enabled) {
  if (!import.meta.client) return
  frostedState.enabled = enabled
  localStorage.setItem(FROSTED_KEY, enabled ? 'true' : 'false')
  apply()
}
