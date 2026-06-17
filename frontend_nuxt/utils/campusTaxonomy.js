export const CAMPUS_CATEGORY_TAGS = Object.freeze({
  教务公告: ['选课', '考试', '课程经验', '教务通知', '奖助学金'],
  学术交流: ['讲座', '科研', '论文', '竞赛', '升学'],
  跳蚤市场: ['出闲置', '求购', '教材', '数码', '生活用品'],
  校园生活: ['食堂', '宿舍', '社团', '运动', '校园活动'],
})

export const HIDDEN_POST_CATEGORY_NAMES = Object.freeze(['树洞互助'])

export const CAMPUS_CATEGORY_NAMES = Object.freeze(Object.keys(CAMPUS_CATEGORY_TAGS))
export const CAMPUS_TAG_NAMES = Object.freeze([
  ...new Set(Object.values(CAMPUS_CATEGORY_TAGS).flat()),
])

export const normalizeTaxonomyName = (value) => String(value ?? '').trim()

export const getCategoryTagNames = (categoryName) =>
  CAMPUS_CATEGORY_TAGS[normalizeTaxonomyName(categoryName)] || []

export const isPlaceholderTaxonomy = (item) => /^测试用/.test(item?.name || '')
export const isHiddenPostCategory = (item) =>
  HIDDEN_POST_CATEGORY_NAMES.includes(normalizeTaxonomyName(item?.name))

export const filterCampusTaxonomy = (items) =>
  Array.isArray(items)
    ? items.filter((item) => !isPlaceholderTaxonomy(item) && !isHiddenPostCategory(item))
    : []

export const findCategoryById = (categories, categoryId) => {
  if (categoryId == null || categoryId === '') return null
  return (Array.isArray(categories) ? categories : []).find(
    (category) => String(category.id) === String(categoryId),
  )
}

export const resolveCategoryName = (category, categories = []) => {
  if (!category) return ''
  if (typeof category === 'object') return normalizeTaxonomyName(category.name)

  const matchedCategory = findCategoryById(categories, category)
  if (matchedCategory) return normalizeTaxonomyName(matchedCategory.name)

  return Number.isNaN(Number(category)) ? normalizeTaxonomyName(category) : ''
}

export const sortTagsByCategory = (tags, categoryName, fallbackToAll = true) => {
  const tagNames = getCategoryTagNames(categoryName)
  if (tagNames.length === 0) return fallbackToAll && Array.isArray(tags) ? tags : []

  const order = new Map(tagNames.map((name, index) => [name, index]))
  return (Array.isArray(tags) ? tags : [])
    .filter((tag) => order.has(normalizeTaxonomyName(tag.name)))
    .sort(
      (a, b) => order.get(normalizeTaxonomyName(a.name)) - order.get(normalizeTaxonomyName(b.name)),
    )
}
