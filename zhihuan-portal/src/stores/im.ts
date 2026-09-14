import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Conversation, ChatMessage } from '@/types/entity/im'
import { imApi } from '@/api/modules/im.api'

export const useImStore = defineStore('im', () => {
  const conversations = ref<Conversation[]>([])
  const activeConversationId = ref<string>('')
  const messages = ref<Record<string, ChatMessage[]>>({})

  async function loadConversations() {
    conversations.value = await imApi.getConversations()
  }

  async function loadMessages(conversationId: string) {
    const list = await imApi.getMessages(conversationId)
    messages.value[conversationId] = list
  }

  async function sendMessage(content: string, type: ChatMessage['type'] = 'text', productId?: number) {
    const conv = conversations.value.find(c => c.id === activeConversationId.value)
    if (!conv) return
    const msg = await imApi.sendMessage({
      conversationId: activeConversationId.value,
      receiverId: conv.targetUser.id,
      content,
      type,
      productId,
    })
    if (!messages.value[activeConversationId.value]) {
      messages.value[activeConversationId.value] = []
    }
    messages.value[activeConversationId.value].push(msg)
    return msg
  }

  return { conversations, activeConversationId, messages, loadConversations, loadMessages, sendMessage }
})