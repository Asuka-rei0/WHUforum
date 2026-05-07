<template>
  <BasePopup :visible="visible" @close="close">
    <div class="message-popup">
      <div class="message-popup-title">📨 站内信上线啦</div>
      <div class="message-popup-text">现在可以在右上角使用站内信功能</div>
      <div class="message-popup-actions">
        <div class="message-popup-close" @click="close">知道了</div>
        <div class="message-popup-button" @click="gotoMessage">去看看</div>
      </div>
    </div>
  </BasePopup>
</template>

<script setup>
import BasePopup from '~/components/BasePopup.vue'

defineProps({
  visible: { type: Boolean, default: false },
})
const emit = defineEmits(['close'])

const gotoMessage = () => {
  emit('close')
  navigateTo('/message-box', { replace: true })
}
const close = () => emit('close')
</script>

<style scoped>
.message-popup {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 10px;
  min-width: 200px;
}

.message-popup-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
}

.message-popup-actions {
  margin-top: 10px;
  display: flex;
  flex-direction: row;
  gap: 20px;
}

.message-popup-button {
  background-color: var(--primary-color);
  color: #fff;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
}

.message-popup-button:hover {
  background-color: var(--primary-color-hover);
}

.message-popup-close {
  cursor: pointer;
  color: var(--primary-color);
  display: flex;
  align-items: center;
}

.message-popup-close:hover {
  text-decoration: underline;
}
</style>
