<template>
  <div class="lottery-section">
    <AvatarCropper
      :src="data.tempPrizeIcon"
      :show="data.showPrizeCropper"
      @close="data.showPrizeCropper = false"
      @crop="onPrizeCropped"
    />
    <div class="prize-row">
      <span class="prize-row-title">奖品图片</span>
      <label class="prize-container">
        <BaseImage v-if="data.prizeIcon" :src="data.prizeIcon" class="prize-preview" alt="prize" />
        <image-files v-else class="default-prize-icon" />
        <div class="prize-overlay">上传奖品图片</div>
        <input type="file" class="prize-input" accept="image/*" @change="onPrizeIconChange" />
      </label>
    </div>
    <div class="prize-name-row">
      <span class="prize-row-title">奖品描述</span>
      <BaseInput v-model="data.prizeDescription" placeholder="奖品描述" />
    </div>
    <div class="prize-count-row">
      <span class="prize-row-title">奖品数量</span>
      <div class="prize-count-input">
        <input
          class="prize-count-input-field"
          type="number"
          v-model.number="data.prizeCount"
          min="1"
        />
      </div>
    </div>
    <div class="prize-point-row">
      <span class="prize-row-title">参与所需积分</span>
      <div class="prize-count-input">
        <input
          class="prize-count-input-field"
          type="number"
          v-model.number="data.pointCost"
          min="0"
          max="100"
        />
      </div>
    </div>
    <div class="prize-time-row">
      <span class="prize-row-title">抽奖结束时间</span>
      <client-only>
        <flat-pickr v-model="data.endTime" :config="dateConfig" class="time-picker" />
      </client-only>
    </div>
  </div>
</template>

<script setup>
import 'flatpickr/dist/flatpickr.css'
import FlatPickr from 'vue-flatpickr-component'
import AvatarCropper from '~/components/AvatarCropper.vue'
import BaseImage from '~/components/BaseImage.vue'
import BaseInput from '~/components/BaseInput.vue'
import { watch } from 'vue'

const props = defineProps({
  data: {
    type: Object,
    required: true,
  },
})

const dateConfig = { enableTime: true, time_24hr: true, dateFormat: 'Y-m-d H:i' }

const onPrizeIconChange = (e) => {
  const file = e.target.files[0]
  if (file) {
    const reader = new FileReader()
    reader.onload = () => {
      props.data.tempPrizeIcon = reader.result
      props.data.showPrizeCropper = true
    }
    reader.readAsDataURL(file)
  }
}

const onPrizeCropped = ({ file, url }) => {
  props.data.prizeIconFile = file
  props.data.prizeIcon = url
}

watch(
  () => props.data.prizeCount,
  (val) => {
    if (!val || val < 1) props.data.prizeCount = 1
  },
)

watch(
  () => props.data.pointCost,
  (val) => {
    if (val === undefined || val === null || val < 0) props.data.pointCost = 0
    if (val > 100) props.data.pointCost = 100
  },
)
</script>

<style scoped>
.lottery-section {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 200px;
}
.prize-row-title {
  font-size: 16px;
  color: var(--text-color);
  font-weight: bold;
  margin-bottom: 10px;
}
.prize-row,
.prize-name-row,
.prize-count-row,
.prize-point-row,
.prize-time-row {
  display: flex;
  flex-direction: column;
}
.prize-container {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  background-color: var(--lottery-background-color);
  display: flex;
  align-items: center;
  justify-content: center;
}
.default-prize-icon {
  font-size: 30px;
  opacity: 0.1;
  color: var(--text-color);
}
.prize-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.prize-input {
  display: none;
}
.prize-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.4);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}
.prize-container:hover .prize-overlay {
  opacity: 1;
}
.prize-count-input {
  display: flex;
  align-items: center;
}
.prize-count-input-field {
  width: 50px;
  height: 30px;
  border-radius: 5px;
  border: 1px solid var(--border-color);
  padding: 0 10px;
  font-size: 16px;
  color: var(--text-color);
  background-color: var(--lottery-background-color);
}
.time-picker {
  max-width: 200px;
  height: 30px;
  background-color: var(--lottery-background-color);
  border-radius: 5px;
  border: 1px solid var(--border-color);
}
</style>
