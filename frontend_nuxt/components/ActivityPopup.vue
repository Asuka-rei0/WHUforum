<template>
  <BasePopup :visible="visible" @close="close">
    <div class="activity-popup">
      <BaseImage v-if="icon" :src="icon" class="activity-popup-icon" alt="activity icon" />
      <div class="activity-popup-text">{{ text }}</div>
      <div class="activity-popup-actions">
        <div class="activity-popup-button" @click="gotoActivity">立即前往</div>
        <div class="activity-popup-close" @click="close">稍后再说</div>
      </div>
    </div>
  </BasePopup>
</template>

<script setup>
import BasePopup from '~/components/BasePopup.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  icon: String,
  text: String,
})
const emit = defineEmits(['close'])
const gotoActivity = async () => {
  emit('close')
  await navigateTo('/activities', { replace: true })
}
const close = () => emit('close')
</script>

<style scoped>
.activity-popup {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 10px;
  min-width: 200px;
}

.activity-popup-icon {
  width: 100px;
  height: 100px;
  object-fit: contain;
}
.activity-popup-actions {
  margin-top: 10px;
  display: flex;
  flex-direction: row;
  gap: 20px;
}
.activity-popup-button {
  background-color: var(--primary-color);
  color: #fff;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
}
.activity-popup-button:hover {
  background-color: var(--primary-color-hover);
}
.activity-popup-close {
  cursor: pointer;
  color: var(--primary-color);
  display: flex;
  align-items: center;
}
.activity-popup-close:hover {
  text-decoration: underline;
}
</style>
