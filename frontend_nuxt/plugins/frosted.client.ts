import { defineNuxtPlugin } from 'nuxt/app'
import { initFrosted } from '~/utils/frosted'

export default defineNuxtPlugin(() => {
  initFrosted()
})
