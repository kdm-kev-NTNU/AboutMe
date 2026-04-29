const CHAT_CONVERSATION_ID_KEY = 'portfolio.chat.conversationId.v1'

function safeUuidV4(): string {
  try {
    return crypto.randomUUID()
  } catch {
    // Fallback for older browsers / restricted contexts.
    return `${Date.now()}-${Math.random().toString(16).slice(2)}`
  }
}

export function getOrCreateChatConversationId(): string {
  try {
    const existing = localStorage.getItem(CHAT_CONVERSATION_ID_KEY)
    if (existing && existing.trim()) return existing
    const created = safeUuidV4()
    localStorage.setItem(CHAT_CONVERSATION_ID_KEY, created)
    return created
  } catch {
    return safeUuidV4()
  }
}

export function resetChatConversationId(): string {
  const created = safeUuidV4()
  try {
    localStorage.setItem(CHAT_CONVERSATION_ID_KEY, created)
  } catch {
    // ignore
  }
  return created
}
