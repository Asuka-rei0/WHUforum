<template>
  <div class="treehole-compose-page">
    <div class="treehole-compose-header">
      <button class="treehole-back" type="button" @click="navigateTo('/treehole')">
        <arrow-left />
        返回
      </button>
      <div>
        <h1>发布匿名树洞</h1>
        <p>提交成功后，除硬拦截内容外都会先保存，再进入审核流程。</p>
      </div>
    </div>

    <div class="treehole-compose-form">
      <input v-model="title" class="treehole-title-input" placeholder="标题" />
      <PostEditor v-model="content" :disabled="isSubmitting" />

      <div class="treehole-visibility">
        <button
          v-for="option in TREEHOLE_VISIBILITY_OPTIONS"
          :key="option.id"
          type="button"
          class="treehole-visibility-option"
          :class="{ selected: visibility === option.id }"
          @click="visibility = option.id"
        >
          <span class="treehole-visibility-title">{{ option.title }}</span>
          <span class="treehole-visibility-desc">{{ option.description }}</span>
        </button>
      </div>

      <div v-if="targetError" class="treehole-target-error">{{ targetError }}</div>

      <div class="treehole-submit-row">
        <button
          class="treehole-submit"
          type="button"
          :disabled="isSubmitting || targetLoading || !!targetError"
          @click="submitTreehole"
        >
          <template v-if="isSubmitting">
            <loading-four class="treehole-submit-icon" />
            发布中...
          </template>
          <template v-else>发布</template>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import PostEditor from '~/components/PostEditor.vue'
import { toast } from '~/main'
import { getApiErrorMessage } from '~/utils/apiError'
import { getToken } from '~/utils/auth'
import {
  TREEHOLE_VISIBILITY_OPTIONS,
  isTreeholeCategory,
  isTreeholeTag,
} from '~/utils/treehole'

definePageMeta({ middleware: ['auth-required'] })
useHead({ title: '发布匿名树洞 - 珞珈论坛' })

const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const title = ref('')
const content = ref('')
const visibility = ref('PUBLIC')
const categoryId = ref(null)
const tagId = ref(null)
const targetLoading = ref(true)
const targetError = ref('')
const isSubmitting = ref(false)

const loadTreeholeTarget = async () => {
  targetLoading.value = true
  targetError.value = ''
  try {
    const [categories, searchedTags] = await Promise.all([
      $fetch(`${API_BASE_URL}/api/categories`),
      $fetch(`${API_BASE_URL}/api/tags?keyword=${encodeURIComponent('树洞')}&limit=30`),
    ])
    const category = Array.isArray(categories) ? categories.find(isTreeholeCategory) : null
    let tag = Array.isArray(searchedTags) ? searchedTags.find(isTreeholeTag) : null
    if (!tag) {
      const allTags = await $fetch(`${API_BASE_URL}/api/tags?limit=200`)
      tag = Array.isArray(allTags) ? allTags.find(isTreeholeTag) : null
    }
    if (!category || !tag) {
      targetError.value = '树洞默认分类或标签未初始化，请联系管理员。'
      return
    }
    categoryId.value = category.id
    tagId.value = tag.id
  } catch (e) {
    targetError.value = '树洞发布配置加载失败，请稍后再试。'
  } finally {
    targetLoading.value = false
  }
}

const submitTreehole = async () => {
  if (!title.value.trim()) {
    toast.error('标题不能为空')
    return
  }
  if (!content.value.trim()) {
    toast.error('内容不能为空')
    return
  }
  if (!categoryId.value || !tagId.value) {
    toast.error(targetError.value || '树洞发布配置未就绪')
    return
  }

  const token = getToken()
  if (!token) {
    toast.error('请先登录')
    return
  }

  isSubmitting.value = true
  try {
    const res = await fetch(`${API_BASE_URL}/api/posts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        title: title.value,
        content: content.value,
        categoryId: categoryId.value,
        tagIds: [tagId.value],
        type: 'TREEHOLE',
        anonymous: true,
        treeholeExpectedVisibility: visibility.value,
      }),
    })
    const data = await res.json().catch(() => ({}))
    if (res.ok) {
      if (visibility.value === 'PUBLIC') {
        toast.success('发布成功，审核中仅你可见')
        await navigateTo('/treehole/square')
      } else {
        toast.success('发布成功，仅自己可见')
        await navigateTo('/treehole/mine')
      }
      return
    }
    if (res.status === 429) {
      toast.error('发布过于频繁，请稍后再试')
    } else {
      toast.error(getApiErrorMessage(data, '发布失败，请修改后重试'))
    }
  } catch (e) {
    toast.error('发布失败，请稍后再试')
  } finally {
    isSubmitting.value = false
  }
}

onMounted(loadTreeholeTarget)
</script>

<style scoped>
.treehole-compose-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 18px 48px;
}

.treehole-compose-header {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.treehole-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  background: var(--background-color);
  color: var(--text-color);
  cursor: pointer;
  padding: 0 12px;
}

.treehole-compose-header h1 {
  color: var(--text-color);
  font-size: 24px;
  margin: 0;
}

.treehole-compose-header p {
  color: var(--text-color-secondary);
  line-height: 1.7;
  margin: 6px 0 0;
}

.treehole-compose-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.treehole-title-input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  background: var(--background-color);
  color: var(--text-color);
  font-size: 18px;
  outline: none;
  padding: 12px 14px;
}

.treehole-title-input:focus {
  border-color: var(--primary-color);
}

.treehole-visibility {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.treehole-visibility-option {
  text-align: left;
  border: 1px solid var(--normal-border-color);
  border-radius: 8px;
  background: var(--background-color);
  cursor: pointer;
  padding: 14px;
}

.treehole-visibility-option.selected {
  border-color: var(--primary-color);
  box-shadow: inset 0 0 0 1px var(--primary-color);
}

.treehole-visibility-title,
.treehole-visibility-desc {
  display: block;
}

.treehole-visibility-title {
  color: var(--text-color);
  font-size: 15px;
  font-weight: 700;
}

.treehole-visibility-desc {
  color: var(--text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
  margin-top: 5px;
}

.treehole-target-error {
  color: #b42318;
  background: rgba(244, 63, 94, 0.1);
  border-radius: 8px;
  padding: 10px 12px;
}

.treehole-submit-row {
  display: flex;
  justify-content: flex-end;
}

.treehole-submit {
  min-width: 112px;
  min-height: 40px;
  border: 1px solid var(--primary-color);
  border-radius: 8px;
  background: var(--primary-color);
  color: white;
  cursor: pointer;
  font-weight: 700;
}

.treehole-submit:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.treehole-submit-icon {
  margin-right: 6px;
}

@media (max-width: 700px) {
  .treehole-compose-header {
    flex-direction: column;
  }

  .treehole-visibility {
    grid-template-columns: 1fr;
  }
}
</style>
