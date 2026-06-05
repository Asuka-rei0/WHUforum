const DEFAULT_ERROR_MESSAGE = '操作失败，请稍后重试'

const getRawMessage = (error) => {
  if (!error) return ''
  if (typeof error === 'string') return error.trim()
  if (typeof error.error === 'string') return error.error.trim()
  if (typeof error.message === 'string') return error.message.trim()
  return ''
}

const getReasonCode = (error) => {
  if (!error || typeof error !== 'object') return ''
  return String(error.reason_code || error.reasonCode || '').trim()
}

const hasChinese = (message) => /[\u4e00-\u9fff]/.test(message)

export const isDuplicateEmailError = (error) => {
  const message = getRawMessage(error).toLowerCase()
  return (
    message.includes('uk_users_email') ||
    message.includes('users_email') ||
    message.includes('email already') ||
    message.includes('邮箱已') ||
    (message.includes('duplicate entry') && message.includes('@whu.edu.cn'))
  )
}

export const isDuplicateUsernameError = (error) => {
  const message = getRawMessage(error).toLowerCase()
  return (
    message.includes('uk_users_username') ||
    message.includes('users_username') ||
    message.includes('username already') ||
    message.includes('用户名已') ||
    (message.includes('duplicate entry') && message.includes('username'))
  )
}

const getKnownErrorMessage = (error) => {
  const reasonCode = getReasonCode(error)
  if (reasonCode === 'WHU_EMAIL_REQUIRED') return '请使用 @whu.edu.cn 邮箱'
  if (reasonCode === 'INVALID_CREDENTIALS') return '登录状态已失效，请重新登录'
  if (reasonCode === 'NOT_VERIFIED') return '账号尚未完成邮箱验证'
  if (reasonCode === 'NOT_APPROVED') return '账号尚未通过审核'
  if (reasonCode === 'IS_APPROVING') return '账号正在审核中'
  if (reasonCode === 'INVALID_PASSWORD') return '当前密码不正确'

  if (isDuplicateEmailError(error)) return '该邮箱已注册，请直接登录，或使用其他武汉大学邮箱'
  if (isDuplicateUsernameError(error)) return '该用户名已被占用，请换一个昵称'

  const message = getRawMessage(error).toLowerCase()
  if (!message) return ''
  if (message.includes('invalid or expired token')) return '登录状态已失效，请重新登录'
  if (message.includes('invalid credentials')) return '邮箱或密码不正确'
  if (message.includes('bad credentials')) return '邮箱或密码不正确'
  if (message.includes('invalid email or password')) return '邮箱或密码不正确'
  if (message.includes('forbidden')) return '没有权限执行此操作'
  if (message.includes('unauthorized')) return '请先登录'
  if (message.includes('failed to fetch') || message.includes('networkerror')) {
    return '网络连接异常，请稍后重试'
  }
  if (message.includes('rate limit') || message.includes('too many requests')) {
    return '操作过于频繁，请稍后再试'
  }
  return ''
}

const isTechnicalMessage = (message) => {
  const lowerMessage = message.toLowerCase()
  return [
    'could not execute statement',
    'duplicate entry',
    'constraint',
    'sql [',
    'hibernate',
    'jdbc',
    'java.',
    'org.springframework',
    'nullpointerexception',
    'internal server error',
    'http error',
    'status:',
    'stacktrace',
  ].some((part) => lowerMessage.includes(part))
}

export const getApiErrorMessage = (error, fallback = DEFAULT_ERROR_MESSAGE) => {
  const knownMessage = getKnownErrorMessage(error)
  if (knownMessage) return knownMessage

  const rawMessage = getRawMessage(error)
  if (!rawMessage) return fallback
  if (isTechnicalMessage(rawMessage)) return fallback
  if (!hasChinese(rawMessage)) return fallback
  return rawMessage
}
