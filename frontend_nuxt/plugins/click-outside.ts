import { defineNuxtPlugin } from 'nuxt/app'
import ClickOutside from '~/directives/clickOutside.js'

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.directive('click-outside', ClickOutside)
})
