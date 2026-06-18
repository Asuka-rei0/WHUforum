<template>
  <Teleport to="body">
    <div v-if="modelValue" class="report-dialog-backdrop" @click.self="close">
      <form class="report-dialog" @submit.prevent="submit">
        <header class="report-dialog-header">
          <div>
            <div class="report-dialog-kicker">内容举报</div>
            <h2>举报{{ targetLabel }}</h2>
          </div>
          <button type="button" class="icon-button" aria-label="关闭" @click="close">
            <close-small />
          </button>
        </header>

        <label class="report-field">
          <span>举报原因</span>
          <select v-model="reason">
            <option
              v-for="option in CONTENT_REPORT_REASON_OPTIONS"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </option>
          </select>
        </label>

        <label class="report-field">
          <span>具体说明</span>
          <textarea
            v-model="detail"
            maxlength="1000"
            placeholder="补充违规位置、上下文或其他需要管理员了解的信息"
          ></textarea>
        </label>
        <div class="report-count">{{ detail.length }}/1000</div>

        <div class="report-actions">
          <button type="button" @click="close">取消</button>
          <button type="submit" class="primary" :disabled="submitting">
            {{ submitting ? '提交中' : '提交举报' }}
          </button>
        </div>
      </form>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { CloseSmall } from '@icon-park/vue-next'
import {
  CONTENT_REPORT_REASON_OPTIONS,
  getContentReportTargetText,
} from '~/utils/contentReport'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  targetType: {
    type: String,
    required: true,
  },
  targetId: {
    type: [Number, String],
    required: true,
  },
  submitting: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue', 'submit'])

const reason = ref('OTHER')
const detail = ref('')
const targetLabel = computed(() => getContentReportTargetText(props.targetType))

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      reason.value = 'OTHER'
      detail.value = ''
    }
  },
)

const close = () => {
  if (props.submitting) return
  emit('update:modelValue', false)
}

const submit = () => {
  if (props.submitting) return
  emit('submit', {
    targetType: props.targetType,
    targetId: Number(props.targetId),
    reason: reason.value,
    detail: detail.value.trim(),
  })
}
</script>

<style scoped>
.report-dialog-backdrop {
  align-items: center;
  background: rgba(0, 0, 0, 0.42);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: 18px;
  position: fixed;
  z-index: 2000;
}

.report-dialog {
  background: var(--background-color);
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.18);
  box-sizing: border-box;
  display: grid;
  gap: 14px;
  max-width: 520px;
  padding: 18px;
  width: min(520px, 100%);
}

.report-dialog-header {
  align-items: flex-start;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.report-dialog-kicker {
  color: var(--primary-color);
  font-size: 13px;
  font-weight: 700;
}

.report-dialog h2 {
  font-size: 20px;
  margin: 4px 0 0;
}

.icon-button {
  align-items: center;
  background: transparent;
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  color: var(--text-color);
  cursor: pointer;
  display: inline-flex;
  height: 34px;
  justify-content: center;
  padding: 0;
  width: 34px;
}

.report-field {
  display: grid;
  gap: 8px;
  font-size: 14px;
}

.report-field select,
.report-field textarea {
  background: var(--background-color);
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  box-sizing: border-box;
  color: var(--text-color);
  font: inherit;
  width: 100%;
}

.report-field select {
  min-height: 40px;
  padding: 8px 10px;
}

.report-field textarea {
  line-height: 1.6;
  min-height: 120px;
  padding: 10px;
  resize: vertical;
}

.report-count {
  color: var(--menu-text-color);
  font-size: 12px;
  margin-top: -8px;
  text-align: right;
}

.report-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.report-actions button {
  background: var(--background-color);
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  color: var(--text-color);
  cursor: pointer;
  min-height: 38px;
  padding: 0 14px;
}

.report-actions .primary {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: #fff;
}

.report-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}
</style>
