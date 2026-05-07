<template>
  <BasePopup :visible="visible" @close="onClose">
    <div class="redeem-dialog-content">
      <BaseInput
        textarea
        rows="5"
        v-model="innerContact"
        placeholder="联系方式 (手机号/Email/微信/instagram/telegram等, 务必注明来源)"
      />
      <div class="redeem-actions">
        <div class="redeem-submit-button" @click="submit" :disabled="loading">提交</div>
        <div class="redeem-cancel-button" @click="onClose">取消</div>
      </div>
    </div>
  </BasePopup>
</template>

<script setup>
import { ref, watch } from 'vue'
import BaseInput from '~/components/BaseInput.vue'
import BasePopup from '~/components/BasePopup.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  modelValue: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue', 'submit', 'close'])

const innerContact = ref(props.modelValue)
watch(
  () => props.modelValue,
  (v) => {
    innerContact.value = v
  },
)
watch(innerContact, (v) => emit('update:modelValue', v))

const submit = () => {
  emit('submit')
}
const onClose = () => {
  emit('close')
}
</script>

<style scoped>
.redeem-dialog-content {
  position: relative;
  z-index: 2;
  background-color: var(--background-color);
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 400px;
}

.redeem-actions {
  margin-top: 10px;
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  gap: 20px;
  align-items: center;
}

.redeem-submit-button {
  background-color: var(--primary-color);
  color: #fff;
  padding: 10px 20px;
  border-radius: 10px;
  cursor: pointer;
}

.redeem-submit-button:disabled {
  background-color: var(--primary-color-disabled);
  cursor: not-allowed;
}

.redeem-submit-button:hover {
  background-color: var(--primary-color-hover);
}

.redeem-submit-button:disabled:hover {
  background-color: var(--primary-color-disabled);
}

.redeem-cancel-button {
  color: var(--primary-color);
  border-radius: 10px;
  cursor: pointer;
}

.redeem-cancel-button:hover {
  text-decoration: underline;
}

@media screen and (max-width: 768px) {
  .redeem-dialog-content {
    min-width: 300px;
  }
}
</style>
