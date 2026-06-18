export const CONTENT_REPORT_REASON_OPTIONS = [
  { value: 'SPAM', label: '垃圾广告' },
  { value: 'HARASSMENT', label: '骚扰或攻击' },
  { value: 'HATE', label: '仇恨或歧视' },
  { value: 'SEXUAL', label: '色情低俗' },
  { value: 'VIOLENCE', label: '暴力威胁' },
  { value: 'SELF_HARM', label: '自伤风险' },
  { value: 'PRIVACY', label: '隐私泄露' },
  { value: 'ILLEGAL', label: '违法违规' },
  { value: 'OTHER', label: '其他' },
]

const REASON_TEXT = Object.fromEntries(
  CONTENT_REPORT_REASON_OPTIONS.map((option) => [option.value, option.label]),
)

const TARGET_TEXT = {
  POST: '帖子',
  COMMENT: '评论',
  MESSAGE: '私信',
  TREEHOLE: '树洞',
}

export const getContentReportReasonText = (reason) => REASON_TEXT[reason] || reason || '其他'

export const getContentReportTargetText = (targetType) =>
  TARGET_TEXT[targetType] || targetType || '内容'

export const getContentReportPostRoute = (report) => {
  if (!report?.postId) return ''
  const hash = report.commentId ? `#comment-${report.commentId}` : ''
  return `/posts/${report.postId}${hash}`
}
