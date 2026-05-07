<template>
  <div class="base-placeholder">
    <component :is="icon" class="base-placeholder-icon" theme="outline" />
    <div class="base-placeholder-text">
      <slot>{{ text }}</slot>
    </div>
  </div>
</template>

<script>
export default {
  name: 'BasePlaceholder',
  props: {
    text: { type: String, default: '' },
    icon: { type: String, default: 'inbox' },
  },
}
</script>

<style scoped>
.base-placeholder {
  display: flex;
  flex-direction: row;
  gap: 10px;
  justify-content: center;
  align-items: center;
  height: 300px;
  opacity: 0.5;
}
.base-placeholder-icon,
.base-placeholder-text {
  font-size: 16px;
  color: var(--text-color);
}
</style>
