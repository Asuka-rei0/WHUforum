import { ref, onMounted, onBeforeUnmount } from 'vue'

/**
 * 通用倒计时 composable
 * @param endTime 截止时间字符串或 Date 对象
 * @returns { countdown, isEnded }
 */
export function useCountdown(endTime?: string | Date) {
  const countdown = ref('')
  const isEnded = ref(false)
  let timer: ReturnType<typeof setInterval> | null = null

  const update = () => {
    if (!endTime) {
      countdown.value = ''
      isEnded.value = true
      return
    }
    const diff = new Date(endTime).getTime() - Date.now()
    if (diff <= 0) {
      countdown.value = '已结束'
      isEnded.value = true
      if (timer) clearInterval(timer)
      return
    }
    // 计算天、时、分、秒
    const days = Math.floor(diff / (24 * 3600 * 1000))
    const hours = Math.floor((diff % (24 * 3600 * 1000)) / 3600000)
    const minutes = Math.floor((diff % 3600000) / 60000)
    const seconds = Math.floor((diff % 60000) / 1000)

    if (days > 0) {
      countdown.value = `${days}天 ${hours}小时 ${minutes}分 ${seconds}秒`
    } else if (hours > 0) {
      countdown.value = `${hours}小时 ${minutes}分 ${seconds}秒`
    } else {
      countdown.value = `${minutes}分 ${seconds}秒`
    }
  }

  onMounted(() => {
    update()
    timer = setInterval(update, 1000)
  })

  onBeforeUnmount(() => {
    if (timer) clearInterval(timer)
  })

  return { countdown, isEnded }
}
