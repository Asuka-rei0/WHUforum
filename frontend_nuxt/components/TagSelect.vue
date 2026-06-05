<template>
  <Dropdown
    ref="dropdownRef"
    v-model="selected"
    :fetch-options="fetchTags"
    multiple
    placeholder="选择标签"
    remote
    :initial-options="mergedOptions"
  >
    <template #option="{ option }">
      <div class="option-container">
        <div class="option-main">
          <template v-if="option.smallIcon || option.icon">
            <BaseImage
              v-if="isImageIcon(option.smallIcon || option.icon)"
              :src="option.smallIcon || option.icon"
              class="option-icon"
              :alt="option.name"
            />
            <component v-else :is="option.smallIcon || option.icon" class="option-icon" />
          </template>
          <span>{{ option.name }}</span>
          <span class="option-count" v-if="option.count > 0"> x {{ option.count }}</span>
        </div>
        <div v-if="option.description" class="option-desc">{{ option.description }}</div>
      </div>
    </template>
    <template #footer>
      <div v-if="hasMoreRemoteTags" class="dropdown-footer">
        <a
          href="#"
          class="dropdown-more"
          :class="{ disabled: loadMoreRequested }"
          @click.prevent="loadMoreRemoteTags"
        >
          {{ loadMoreRequested ? '加载中...' : '查看更多' }}
        </a>
      </div>
    </template>
  </Dropdown>
</template>

<script setup>
import { computed, reactive, ref, watch, nextTick } from 'vue'
import { toast } from '~/main'
import Dropdown from '~/components/Dropdown.vue'
import {
  filterCampusTaxonomy,
  getCategoryTagNames,
  resolveCategoryName,
  sortTagsByCategory,
} from '~/utils/campusTaxonomy'
const config = useRuntimeConfig()
const API_BASE_URL = config.public.apiBaseUrl

const emit = defineEmits(['update:modelValue'])
const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  creatable: { type: Boolean, default: false },
  options: { type: Array, default: () => [] },
  category: { type: [String, Number, Object], default: '' },
  categories: { type: Array, default: () => [] },
  requireCategory: { type: Boolean, default: false },
})

const dropdownRef = ref(null)
const localTags = ref([])
const providedTags = ref(Array.isArray(props.options) ? [...props.options] : [])
const providedCategories = ref(Array.isArray(props.categories) ? [...props.categories] : [])
const fetchedCategories = ref([])

const TAG_PAGE_SIZE = 10
const remoteState = reactive({
  keyword: '',
  categoryName: '',
  nextPage: 0,
  hasMore: true,
  options: [],
})
const loadMoreRequested = ref(false)

const resetRemoteState = () => {
  remoteState.keyword = ''
  remoteState.categoryName = ''
  remoteState.nextPage = 0
  remoteState.hasMore = true
  remoteState.options = []
  loadMoreRequested.value = false
}

watch(
  () => props.options,
  (val) => {
    providedTags.value = Array.isArray(val) ? [...val] : []
  },
)

watch(
  () => props.categories,
  (val) => {
    providedCategories.value = Array.isArray(val) ? [...val] : []
  },
)

watch(
  () => props.category,
  () => {
    resetRemoteState()
    loadCategoriesForSelectedCategory()
  },
)

const allCategories = computed(() => {
  const arr = [...providedCategories.value, ...fetchedCategories.value]
  return Array.from(new Map(arr.map((category) => [category.id, category])).values())
})

const selectedCategoryName = computed(() =>
  resolveCategoryName(props.category, allCategories.value),
)
const allowedTagNames = computed(() => getCategoryTagNames(selectedCategoryName.value))
const shouldLimitToCategory = computed(
  () => allowedTagNames.value.length > 0 || props.requireCategory,
)

const mergedOptions = computed(() => {
  const arr = [...providedTags.value, ...localTags.value, ...remoteState.options]
  const deduped = arr.filter((v, i, a) => a.findIndex((t) => t.id === v.id) === i)
  if (!shouldLimitToCategory.value) return deduped
  return sortTagsByCategory(deduped, selectedCategoryName.value, !props.requireCategory)
})

const isImageIcon = (icon) => {
  if (!icon) return false
  return /^https?:\/\//.test(icon) || icon.startsWith('/')
}

const shouldLookupCategory = () =>
  props.category &&
  typeof props.category !== 'object' &&
  !Number.isNaN(Number(props.category)) &&
  !selectedCategoryName.value

async function loadCategoriesForSelectedCategory() {
  if (!shouldLookupCategory()) return
  try {
    const res = await fetch(`${API_BASE_URL}/api/categories`)
    if (!res.ok) return
    const data = await res.json()
    fetchedCategories.value = filterCampusTaxonomy(data)
  } catch (e) {
    console.error('Failed to fetch categories', e)
  }
}

const buildTagsUrl = (kw = '', page = 0, paginate = true) => {
  const base = API_BASE_URL || (import.meta.client ? window.location.origin : '')
  const url = new URL('/api/tags', base)

  if (kw) url.searchParams.set('keyword', kw)
  if (paginate) {
    url.searchParams.set('page', String(page))
    url.searchParams.set('pageSize', String(TAG_PAGE_SIZE))
  }

  return url.toString()
}

const fetchRemoteTags = async (kw = '', page = 0, paginate = true) => {
  const url = buildTagsUrl(kw, page, paginate)
  try {
    const res = await fetch(url)
    if (res.ok) {
      const data = await res.json()
      return filterCampusTaxonomy(data)
    }
    throw new Error('failed to fetch tags')
  } catch (e) {
    console.error('Failed to fetch tags', e)
    toast.error('获取标签失败')
    throw e
  }
}

const combineOptions = (remoteOptions = []) => {
  const options = [...providedTags.value, ...localTags.value, ...remoteOptions]
  return Array.from(new Map(options.map((t) => [t.id, t])).values())
}

const fetchTags = async (kw = '') => {
  const defaultOption = { id: 0, name: '无标签' }
  await loadCategoriesForSelectedCategory()
  const categoryName = selectedCategoryName.value

  if (kw !== remoteState.keyword || categoryName !== remoteState.categoryName) {
    remoteState.keyword = kw
    remoteState.categoryName = categoryName
    remoteState.nextPage = 0
    remoteState.options = []
    remoteState.hasMore = true
  }

  if (props.requireCategory && allowedTagNames.value.length === 0) {
    remoteState.hasMore = false
    return [defaultOption]
  }

  const shouldFetch = remoteState.options.length === 0 || loadMoreRequested.value
  if (shouldFetch) {
    const pageToFetch = loadMoreRequested.value ? remoteState.nextPage : 0
    try {
      if (allowedTagNames.value.length > 0) {
        const data = await fetchRemoteTags('', 0, false)
        remoteState.options = sortTagsByCategory(data, categoryName).filter((tag) =>
          tag.name.toLowerCase().includes(remoteState.keyword.toLowerCase()),
        )
        remoteState.hasMore = false
        remoteState.nextPage = 0
      } else {
        const data = await fetchRemoteTags(remoteState.keyword, pageToFetch)
        if (pageToFetch === 0) {
          remoteState.options = data
        } else {
          const existing = Array.isArray(remoteState.options) ? remoteState.options : []
          const merged = [...existing, ...data]
          remoteState.options = Array.from(new Map(merged.map((t) => [t.id, t])).values())
        }
        remoteState.hasMore = data.length === TAG_PAGE_SIZE
        remoteState.nextPage = pageToFetch + 1
      }
    } catch (e) {
      return [defaultOption, ...combineOptions(remoteState.options)]
    } finally {
      loadMoreRequested.value = false
    }
  }

  let options = combineOptions(remoteState.options)
  if (shouldLimitToCategory.value) {
    options = sortTagsByCategory(options, categoryName, !props.requireCategory)
  }

  if (
    props.creatable &&
    !shouldLimitToCategory.value &&
    kw &&
    !options.some((t) => t.name.toLowerCase() === kw.toLowerCase())
  ) {
    options.push({ id: `__create__:${kw}`, name: `创建"${kw}"` })
  }

  return [defaultOption, ...options]
}

const hasMoreRemoteTags = computed(() => remoteState.hasMore && !shouldLimitToCategory.value)

const loadMoreRemoteTags = async () => {
  if (!remoteState.hasMore || loadMoreRequested.value) return
  loadMoreRequested.value = true
  try {
    await dropdownRef.value?.reload()
    await nextTick()
    dropdownRef.value?.scrollToBottom?.()
  } catch (e) {
    console.error('Failed to load more tags', e)
    loadMoreRequested.value = false
  }
}

const selected = computed({
  get: () => props.modelValue,
  set: (v) => {
    if (Array.isArray(v)) {
      if (v.includes(0)) {
        emit('update:modelValue', [])
        return
      }
      if (v.length > 2) {
        toast.error('最多选择两个标签')
        return
      }
      v = v.map((id) => {
        if (typeof id === 'string' && id.startsWith('__create__:')) {
          const name = id.slice(11)
          const newId = `__new__:${name}`
          if (!localTags.value.find((t) => t.id === newId)) {
            localTags.value.push({ id: newId, name })
          }
          return newId
        }
        return id
      })
    }
    emit('update:modelValue', v)
  },
})
</script>

<style scoped>
.option-container {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.option-main {
  display: flex;
  align-items: center;
  gap: 5px;
}

.option-desc {
  font-size: 12px;
  color: #666;
}

.option-count {
  font-weight: bold;
  opacity: 0.4;
}

.dropdown-footer {
  padding: 8px 20px;
  text-align: center;
  border-top: 1px solid var(--normal-border-color);
}

.dropdown-more {
  color: var(--primary-color);
  text-decoration: none;
  cursor: pointer;
}

.dropdown-more.disabled {
  pointer-events: none;
  opacity: 0.6;
}
</style>
