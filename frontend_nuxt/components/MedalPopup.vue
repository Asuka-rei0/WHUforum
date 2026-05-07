<template>
  <BasePopup :visible="visible" @close="close">
    <div class="medal-popup">
      <div class="medal-popup-title">恭喜你获得以下勋章</div>
      <div class="medal-popup-list">
        <div v-for="medal in medals" :key="medal.type" class="medal-popup-item">
          <BaseImage :src="medal.icon" :alt="medal.title" class="medal-popup-item-icon" />
          <div class="medal-popup-item-title">{{ medal.title }}</div>
        </div>
      </div>
      <div class="medal-popup-actions">
        <div class="medal-popup-close" @click="close">知道了</div>
        <div class="medal-popup-button" @click="gotoMedals">去看看</div>
      </div>
    </div>
  </BasePopup>
</template>

<script setup>
import BasePopup from '~/components/BasePopup.vue'
import { authState } from '~/utils/auth'

defineProps({
  visible: { type: Boolean, default: false },
  medals: { type: Array, default: () => [] },
})
const emit = defineEmits(['close'])

const gotoMedals = () => {
  emit('close')
  if (authState.username) {
    navigateTo(`/users/${authState.username}?tab=achievements`, { replace: true })
  } else {
    navigateTo('/', { replace: true })
  }
}
const close = () => emit('close')
</script>

<style scoped>
.medal-popup {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 10px;
  min-width: 200px;
}

.medal-popup-title {
  font-size: 18px;
  font-weight: bold;
}

.medal-popup-list {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.medal-popup-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.medal-popup-item-icon {
  width: 100px;
  height: 100px;
  object-fit: contain;
}

.medal-popup-actions {
  margin-top: 10px;
  display: flex;
  flex-direction: row;
  gap: 20px;
}

.medal-popup-button {
  background-color: var(--primary-color);
  color: #fff;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
}

.medal-popup-button:hover {
  background-color: var(--primary-color-hover);
}

.medal-popup-close {
  cursor: pointer;
  color: var(--primary-color);
  display: flex;
  align-items: center;
}

.medal-popup-close:hover {
  text-decoration: underline;
}
</style>
