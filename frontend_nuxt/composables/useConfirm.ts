// composables/useConfirm.ts
import { ref } from 'vue'

// 全局单例（SPA 下即可；Nuxt/SSR 下见文末“SSR 提醒”）
const visible = ref(false)
const title = ref('')
const message = ref('')

let resolver: ((ok: boolean) => void) | null = null

function reset() {
  visible.value = false
  title.value = ''
  message.value = ''
  resolver = null
}

export function useConfirm() {
  /**
   * 打开确认框，返回 Promise<boolean>
   * - 确认 => resolve(true)
   * - 取消/关闭 => resolve(false)
   * 若并发调用，以最后一次为准（更简单直观）
   */
  const confirm = (t: string, m: string) => {
    title.value = t
    message.value = m
    visible.value = true
    return new Promise<boolean>((resolve) => {
      resolver = resolve
    })
  }

  const onConfirm = () => {
    resolver?.(true)
    reset()
  }

  const onCancel = () => {
    resolver?.(false)
    reset()
  }

  return {
    visible,
    title,
    message,
    confirm,
    onConfirm,
    onCancel,
  }
}
