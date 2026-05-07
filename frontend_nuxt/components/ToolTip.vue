<template>
  <div class="tooltip-wrapper" ref="wrapperRef">
    <!-- 触发器 -->
    <div
      class="tooltip-trigger"
      :tabindex="focusable ? 0 : -1"
      :aria-describedby="visible ? ariaId : undefined"
      @mouseenter="onTriggerMouseEnter"
      @mouseleave="onTriggerMouseLeave"
      @click="onTriggerClick"
      @focus="onTriggerFocus"
      @blur="onTriggerBlur"
    >
      <slot />
    </div>

    <!-- 提示内容（Teleport 到 body） -->
    <Teleport to="body" v-if="mounted">
      <Transition name="tooltip-fade">
        <div
          v-show="visible"
          :id="ariaId"
          ref="tooltipRef"
          class="tooltip-content"
          :class="[
            `tooltip-${currentPlacement}`,
            dark ? 'tooltip-dark' : 'tooltip-light',
            props.trigger === 'hover' ? 'tooltip-noninteractive' : '',
          ]"
          :style="tooltipInlineStyle"
          role="tooltip"
        >
          <div class="tooltip-inner">
            <slot name="content">
              {{ content }}
            </slot>
          </div>

          <!-- 箭头 -->
          <div
            class="tooltip-arrow"
            :class="`tooltip-arrow-${currentPlacement}`"
            :style="arrowStyle"
          ></div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import {
  ref,
  computed,
  onMounted,
  onBeforeUnmount,
  nextTick,
  watch,
  defineEmits,
  defineOptions,
  useId,
} from 'vue'

defineOptions({ name: 'Tooltip' })

type Trigger = 'hover' | 'click' | 'focus' | 'manual'
type Placement = 'top' | 'bottom' | 'left' | 'right'

const props = defineProps({
  content: { type: String, default: '' },
  trigger: {
    type: String as () => Trigger,
    default: 'hover',
    validator: (v: string) => ['hover', 'click', 'focus', 'manual'].includes(v),
  },
  placement: {
    type: String as () => Placement,
    default: 'top',
    validator: (v: string) => ['top', 'bottom', 'left', 'right'].includes(v),
  },
  dark: { type: Boolean, default: false },
  delay: { type: Number, default: 100 },
  disabled: { type: Boolean, default: false },
  focusable: { type: Boolean, default: true },
  offset: { type: Number, default: 8 },
  maxWidth: { type: [String, Number], default: '200px' },
  /** 隐藏延时（毫秒），hover 离开后等待一点点以防抖 */
  hideDelay: { type: Number, default: 80 },
})

const emit = defineEmits<{
  (e: 'show'): void
  (e: 'hide'): void
}>()

const wrapperRef = ref<HTMLElement | null>(null)
const tooltipRef = ref<HTMLElement | null>(null)
const visible = ref(false)
const currentPlacement = ref<Placement>(props.placement)
const ariaId = ref(`tooltip-${useId()}`)
const mounted = ref(false)

let showTimer: number | null = null
let hideTimer: number | null = null
let ro: ResizeObserver | null = null
let rafId: number | null = null

const maxWidthValue = computed(() => {
  return typeof props.maxWidth === 'number' ? `${props.maxWidth}px` : props.maxWidth
})

const tooltipTransform = ref('translate3d(-9999px, -9999px, 0)')

const tooltipInlineStyle = computed(() => ({
  position: 'fixed',
  top: '0px',
  left: '0px',
  zIndex: 2000,
  maxWidth: maxWidthValue.value,
  transform: tooltipTransform.value,
}))

const arrowStyle = ref<Record<string, string>>({})

const clearTimers = () => {
  if (showTimer) {
    window.clearTimeout(showTimer)
    showTimer = null
  }
  if (hideTimer) {
    window.clearTimeout(hideTimer)
    hideTimer = null
  }
}

const show = async () => {
  if (props.disabled) return
  clearTimers()
  showTimer = window.setTimeout(async () => {
    visible.value = true
    emit('show')
    await nextTick()
    updatePosition()
  }, props.delay)
}

const hide = () => {
  clearTimers()
  hideTimer = window.setTimeout(() => {
    visible.value = false
    emit('hide')
  }, props.hideDelay)
}

const showImmediately = async () => {
  if (props.disabled) return
  clearTimers()
  visible.value = true
  emit('show')
  await nextTick()
  updatePosition()
}
const hideImmediately = () => {
  clearTimers()
  visible.value = false
  emit('hide')
}

// 触发器事件
const onTriggerMouseEnter = () => {
  if (props.trigger === 'hover') show()
}
const onTriggerMouseLeave = () => {
  // 关键修改：hover 模式下，离开触发区即开始隐藏计时，不再保持可交互
  if (props.trigger === 'hover') hide()
}
const onTriggerClick = () => {
  if (props.trigger !== 'click') return
  visible.value ? hideImmediately() : showImmediately()
}
const onTriggerFocus = () => {
  if (props.trigger === 'focus') showImmediately()
}
const onTriggerBlur = () => {
  if (props.trigger === 'focus') hideImmediately()
}

// 点击外部关闭（只对 click 模式）
const onClickOutside = (e: MouseEvent) => {
  if (props.trigger !== 'click') return
  const w = wrapperRef.value
  const t = tooltipRef.value
  const target = e.target as Node
  if (w && !w.contains(target) && t && !t.contains(target)) {
    hideImmediately()
  }
}

// 定位算法
function clamp(n: number, min: number, max: number) {
  return Math.max(min, Math.min(max, n))
}

function computeBasePosition(
  placement: Placement,
  triggerRect: DOMRect,
  tooltipRect: DOMRect,
  offset: number,
) {
  const centerX = triggerRect.left + triggerRect.width / 2
  const centerY = triggerRect.top + triggerRect.height / 2

  switch (placement) {
    case 'top':
      return {
        top: triggerRect.top - tooltipRect.height - offset,
        left: centerX - tooltipRect.width / 2,
      }
    case 'bottom':
      return {
        top: triggerRect.bottom + offset,
        left: centerX - tooltipRect.width / 2,
      }
    case 'left':
      return {
        top: centerY - tooltipRect.height / 2,
        left: triggerRect.left - tooltipRect.width - offset,
      }
    case 'right':
      return {
        top: centerY - tooltipRect.height / 2,
        left: triggerRect.right + offset,
      }
  }
}

function positionWithSmartFlip(
  preferred: Placement,
  triggerRect: DOMRect,
  tooltipRect: DOMRect,
  offset: number,
) {
  const padding = 8
  const vw = window.innerWidth
  const vh = window.innerHeight

  let placement: Placement = preferred
  let { top, left } = computeBasePosition(placement, triggerRect, tooltipRect, offset)!

  const outTop = top < padding
  const outBottom = top + tooltipRect.height > vh - padding
  const outLeft = left < padding
  const outRight = left + tooltipRect.width > vw - padding

  if (
    placement === 'top' &&
    outTop &&
    triggerRect.bottom + offset + tooltipRect.height <= vh - padding
  ) {
    placement = 'bottom'
    ;({ top, left } = computeBasePosition(placement, triggerRect, tooltipRect, offset)!)
  } else if (
    placement === 'bottom' &&
    outBottom &&
    triggerRect.top - offset - tooltipRect.height >= padding
  ) {
    placement = 'top'
    ;({ top, left } = computeBasePosition(placement, triggerRect, tooltipRect, offset)!)
  } else if (
    placement === 'left' &&
    outLeft &&
    triggerRect.right + offset + tooltipRect.width <= vw - padding
  ) {
    placement = 'right'
    ;({ top, left } = computeBasePosition(placement, triggerRect, tooltipRect, offset)!)
  } else if (
    placement === 'right' &&
    outRight &&
    triggerRect.left - offset - tooltipRect.width >= padding
  ) {
    placement = 'left'
    ;({ top, left } = computeBasePosition(placement, triggerRect, tooltipRect, offset)!)
  }

  top = clamp(top, padding, vh - tooltipRect.height - padding)
  left = clamp(left, padding, vw - tooltipRect.width - padding)

  const triggerCenterX = triggerRect.left + triggerRect.width / 2
  const triggerCenterY = triggerRect.top + triggerRect.height / 2
  const arrowLeft = clamp(triggerCenterX - left, 10, tooltipRect.width - 10)
  const arrowTop = clamp(triggerCenterY - top, 10, tooltipRect.height - 10)

  return { placement, top, left, arrowLeft, arrowTop }
}

const updatePosition = () => {
  if (!wrapperRef.value || !tooltipRef.value || !visible.value) return
  if (rafId) cancelAnimationFrame(rafId)
  rafId = requestAnimationFrame(() => {
    const triggerEl = wrapperRef.value!.querySelector('.tooltip-trigger') as HTMLElement | null
    const tooltipEl = tooltipRef.value!
    if (!triggerEl) return

    const triggerRect = triggerEl.getBoundingClientRect()
    const tooltipRect = tooltipEl.getBoundingClientRect()
    const { placement, top, left, arrowLeft, arrowTop } = positionWithSmartFlip(
      props.placement,
      triggerRect,
      tooltipRect,
      props.offset,
    )

    currentPlacement.value = placement
    tooltipTransform.value = `translate3d(${Math.round(left)}px, ${Math.round(top)}px, 0)`
    if (placement === 'top' || placement === 'bottom') {
      arrowStyle.value = { '--arrow-left': `${Math.round(arrowLeft)}px` } as any
    } else {
      arrowStyle.value = { '--arrow-top': `${Math.round(arrowTop)}px` } as any
    }
  })
}

const onEnvChanged = () => {
  if (visible.value) updatePosition()
}

watch(
  () => props.disabled,
  (v) => {
    if (v && visible.value) hideImmediately()
  },
)
watch(
  () => props.placement,
  () => {
    if (visible.value) nextTick(updatePosition)
  },
)
watch(visible, (v) => {
  if (!mounted.value) return
  if (v) {
    if ('ResizeObserver' in window && !ro) {
      ro = new ResizeObserver(() => updatePosition())
      if (tooltipRef.value) ro.observe(tooltipRef.value)
      const triggerEl = wrapperRef.value?.querySelector('.tooltip-trigger') as HTMLElement | null
      if (triggerEl) ro.observe(triggerEl)
    }
    updatePosition()
  } else {
    if (ro) {
      ro.disconnect()
      ro = null
    }
  }
})

onMounted(() => {
  mounted.value = true
  window.addEventListener('resize', onEnvChanged, { passive: true })
  window.addEventListener('scroll', onEnvChanged, { passive: true, capture: true })
  document.addEventListener('click', onClickOutside, true)
})

onBeforeUnmount(() => {
  clearTimers()
  if (rafId) cancelAnimationFrame(rafId)
  if (ro) {
    ro.disconnect()
    ro = null
  }
  document.removeEventListener('click', onClickOutside, true)
  window.removeEventListener('resize', onEnvChanged)
  window.removeEventListener('scroll', onEnvChanged, true)
})

// 暴露给父组件（manual 可用）
defineExpose({ show: showImmediately, hide: hideImmediately, updatePosition })
</script>

<style scoped>
.tooltip-wrapper {
  position: relative;
  display: inline-block;
}
.tooltip-trigger {
  display: inline-block;
  outline: none;
}
.tooltip-trigger:focus-visible {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
  border-radius: 4px;
}

.tooltip-content {
  will-change: transform;
  pointer-events: auto; /* 默认允许交互（click/focus 模式） */
}
.tooltip-noninteractive {
  /* hover 模式下禁用指针事件，避免移入浮层导致保持显示 */
  pointer-events: none;
}

.tooltip-inner {
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.4;
  word-wrap: break-word;
  border: 1px solid transparent;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

/* 主题 */
.tooltip-light .tooltip-inner {
  background-color: var(--background-color);
  color: var(--text-color);
  border-color: var(--normal-border-color);
}
.tooltip-dark .tooltip-inner {
  background-color: rgba(0, 0, 0, 0.9);
  color: #fff;
}

/* 箭头（用 CSS 变量控制偏移） */
.tooltip-arrow {
  position: absolute;
  width: 0;
  height: 0;
  border-style: solid;
}

/* 顶部 */
.tooltip-top .tooltip-arrow-top {
  bottom: -6px;
  left: var(--arrow-left, 50%);
  transform: translateX(-50%);
  border-width: 6px 6px 0 6px;
}
.tooltip-light.tooltip-top .tooltip-arrow-top {
  border-color: var(--normal-border-color) transparent transparent transparent;
}
.tooltip-light.tooltip-top .tooltip-arrow-top::after {
  content: '';
  position: absolute;
  top: -7px;
  left: -6px;
  border-width: 6px 6px 0 6px;
  border-style: solid;
  border-color: var(--background-color) transparent transparent transparent;
}
.tooltip-dark.tooltip-top .tooltip-arrow-top {
  border-color: rgba(0, 0, 0, 0.9) transparent transparent transparent;
}

/* 底部 */
.tooltip-bottom .tooltip-arrow-bottom {
  top: -6px;
  left: var(--arrow-left, 50%);
  transform: translateX(-50%);
  border-width: 0 6px 6px 6px;
}
.tooltip-light.tooltip-bottom .tooltip-arrow-bottom {
  border-color: transparent transparent var(--normal-border-color) transparent;
}
.tooltip-light.tooltip-bottom .tooltip-arrow-bottom::after {
  content: '';
  position: absolute;
  top: 1px;
  left: -6px;
  border-width: 0 6px 6px 6px;
  border-style: solid;
  border-color: transparent transparent var(--background-color) transparent;
}
.tooltip-dark.tooltip-bottom .tooltip-arrow-bottom {
  border-color: transparent transparent rgba(0, 0, 0, 0.9) transparent;
}

/* 左侧 */
.tooltip-left .tooltip-arrow-left {
  right: -6px;
  top: var(--arrow-top, 50%);
  transform: translateY(-50%);
  border-width: 6px 0 6px 6px;
}
.tooltip-light.tooltip-left .tooltip-arrow-left {
  border-color: transparent transparent transparent var(--normal-border-color);
}
.tooltip-light.tooltip-left .tooltip-arrow-left::after {
  content: '';
  position: absolute;
  top: -6px;
  left: -7px;
  border-width: 6px 0 6px 6px;
  border-style: solid;
  border-color: transparent transparent transparent var(--background-color);
}
.tooltip-dark.tooltip-left .tooltip-arrow-left {
  border-color: transparent transparent transparent rgba(0, 0, 0, 0.9);
}

/* 右侧 */
.tooltip-right .tooltip-arrow-right {
  left: -6px;
  top: var(--arrow-top, 50%);
  transform: translateY(-50%);
  border-width: 6px 6px 6px 0;
}
.tooltip-light.tooltip-right .tooltip-arrow-right {
  border-color: transparent var(--normal-border-color) transparent transparent;
}
.tooltip-light.tooltip-right .tooltip-arrow-right::after {
  content: '';
  position: absolute;
  top: -6px;
  left: 1px;
  border-width: 6px 6px 6px 0;
  border-style: solid;
  border-color: transparent var(--background-color) transparent transparent;
}
.tooltip-dark.tooltip-right .tooltip-arrow-right {
  border-color: transparent rgba(0, 0, 0, 0.9) transparent transparent;
}

/* 过渡动画 */
.tooltip-fade-enter-active,
.tooltip-fade-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}
.tooltip-fade-enter-from,
.tooltip-fade-leave-to {
  opacity: 0;
  transform: translate3d(0, 4px, 0) scale(0.98);
}

/* 响应式微调 */
@media (max-width: 768px) {
  .tooltip-inner {
    padding: 6px 10px;
    font-size: 13px;
    max-width: 250px;
  }
}
</style>
