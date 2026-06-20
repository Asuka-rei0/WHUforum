import { useWebSocket } from './useWebSocket'
import { fetchUnreadCount, notificationState } from '~/utils/notification'
import { getToken } from '~/utils/auth'

let isInitialized = false

export function useSiteNotificationUnread() {
  const { subscribe, isConnected, connect } = useWebSocket()

  const setupWebSocketListener = () => {
    const destination = '/user/queue/notification-unread-count'
    subscribe(destination, (message) => {
      const unreadCount = parseInt(message.body, 10)
      if (!isNaN(unreadCount)) {
        notificationState.unreadCount = unreadCount
      }
    })
  }

  const initialize = () => {
    const token = getToken()
    if (!token) {
      notificationState.unreadCount = 0
      return
    }

    if (!isConnected.value) {
      connect(token)
    }

    fetchUnreadCount()
    setupWebSocketListener()
  }

  if (!isInitialized && import.meta.client) {
    const token = getToken()
    if (token) {
      isInitialized = true
      initialize()
    }
  }

  return {
    notificationState,
    initialize,
    fetchUnreadCount,
  }
}
