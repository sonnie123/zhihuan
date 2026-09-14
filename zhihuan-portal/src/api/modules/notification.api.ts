import http from '../client'

export const notificationApi = {
  getUnreadCount: () => http.get<{ count: number }>('/notification/unread'),
}