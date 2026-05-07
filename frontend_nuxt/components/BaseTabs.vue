<template>
  <div class="base-tabs">
    <div class="base-tabs-header">
      <div class="base-tabs-items">
        <div
          v-for="tab in tabs"
          :key="tab.key"
          :class="['base-tabs-item', { selected: modelValue === tab.key }]"
          @click="$emit('update:modelValue', tab.key)"
        >
          <component
            v-if="tab.icon && (typeof tab.icon !== 'string' || !tab.icon.includes(' '))"
            :is="tab.icon"
            class="base-tabs-item-icon"
          />
          <div class="base-tabs-item-label">{{ tab.label }}</div>
        </div>
      </div>
      <div class="base-tabs-header-right">
        <slot name="right"></slot>
      </div>
    </div>
    <div class="base-tabs-content" @touchstart="onTouchStart" @touchend="onTouchEnd">
      <slot></slot>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: String, required: true },
  tabs: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:modelValue'])

let touchStartX = 0

function onTouchStart(e) {
  touchStartX = e.touches[0].clientX
}

function onTouchEnd(e) {
  const diffX = e.changedTouches[0].clientX - touchStartX
  if (Math.abs(diffX) > 50) {
    const index = props.tabs.findIndex((t) => t.key === props.modelValue)
    if (diffX < 0 && index < props.tabs.length - 1) {
      emit('update:modelValue', props.tabs[index + 1].key)
    } else if (diffX > 0 && index > 0) {
      emit('update:modelValue', props.tabs[index - 1].key)
    }
  }
}
</script>

<style scoped>
.base-tabs-header {
  display: flex;
  border-bottom: 1px solid var(--normal-border-color);
  align-items: center;
  flex-direction: row;
}

.base-tabs-items {
  display: flex;
  overflow-x: auto;
  scrollbar-width: none;
  flex: 1;
}

.base-tabs-item {
  padding: 10px 20px;
  cursor: pointer;
  white-space: nowrap;
  display: flex;
  align-items: center;
}

.base-tabs-item-icon,
.base-tabs-item i {
  margin-right: 6px;
}

.base-tabs-item.selected {
  color: var(--primary-color);
  border-bottom: 2px solid var(--primary-color);
}

.base-tabs-header-right {
  display: flex;
  flex-shrink: 0;
}

.base-tabs-content {
  width: 100%;
}
</style>
