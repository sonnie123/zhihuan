import http from '../client'
import type { Conversation, ChatMessage } from '@/types/entity/im'

export const imApi = {
  getConversations: () => http.get<Conversation[]>('/im/conversations'),
  getMessages: (conversationId: string) => http.get<ChatMessage[]>('/im/messages', { params: { conversationId } }),
  sendMessage: (data: Partial<ChatMessage>) => http.post<ChatMessage>('/im/send', data),
}