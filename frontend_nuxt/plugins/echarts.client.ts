// plugins/echarts.client.ts
import { defineNuxtPlugin } from 'nuxt/app'
import VueECharts from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  DataZoomComponent,
  TitleComponent,
} from 'echarts/components'

use([LineChart, TitleComponent, TooltipComponent, GridComponent, DataZoomComponent, CanvasRenderer])

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.component('VChart', VueECharts)
})
