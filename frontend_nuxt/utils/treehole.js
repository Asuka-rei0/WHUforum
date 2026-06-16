export const TREEHOLE_CATEGORY_NAMES = ['树洞互助']
export const TREEHOLE_TAG_NAMES = ['树洞']

export const TREEHOLE_STATUS_TEXT = Object.freeze({
  AI_REVIEWING: '审核中，仅你可见',
  PRIVATE: '仅自己可见',
  ADMIN_REVIEWING: '管理员复核中，仅你可见',
  PUBLIC_RESTRICTED: '已限制公开',
  REPORTED: '已上报管理员',
  PUBLIC: '已公开',
})

export const TREEHOLE_STATUS_CLASS = Object.freeze({
  AI_REVIEWING: 'reviewing',
  PRIVATE: 'private',
  ADMIN_REVIEWING: 'admin',
  PUBLIC_RESTRICTED: 'restricted',
  REPORTED: 'reported',
  PUBLIC: 'public',
})

export const TREEHOLE_VISIBILITY_OPTIONS = Object.freeze([
  {
    id: 'PUBLIC',
    title: '公开到树洞广场',
    description: '审核通过后对其他用户公开，审核中仅你可见。',
  },
  {
    id: 'ONLY_ME',
    title: '仅自己可见',
    description: '保存为自己的匿名记录，不进入广场公开展示。',
  },
])

export const normalizeTreeholeName = (value) => String(value ?? '').trim()

export const isTreeholeCategory = (category) =>
  TREEHOLE_CATEGORY_NAMES.includes(normalizeTreeholeName(category?.name))

export const isTreeholeTag = (tag) => TREEHOLE_TAG_NAMES.includes(normalizeTreeholeName(tag?.name))

export const getTreeholeStatusText = (status) => TREEHOLE_STATUS_TEXT[status] || '未知状态'

export const getTreeholeStatusClass = (status) => TREEHOLE_STATUS_CLASS[status] || 'unknown'
