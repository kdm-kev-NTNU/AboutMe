import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getOrCreateChatConversationId, resetChatConversationId } from '../chat-telemetry'

const KEY = 'portfolio.chat.conversationId.v1'

describe('chat-telemetry', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.stubGlobal('crypto', { randomUUID: () => 'aaaaaaaa-bbbb-4ccc-dddd-eeeeeeeeeeee' })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  it('creates and persists a conversation id when missing', () => {
    const id = getOrCreateChatConversationId()
    expect(id).toBe('aaaaaaaa-bbbb-4ccc-dddd-eeeeeeeeeeee')
    expect(localStorage.getItem(KEY)).toBe(id)
  })

  it('returns existing id from localStorage', () => {
    localStorage.setItem(KEY, '  existing-id  ')
    expect(getOrCreateChatConversationId()).toBe('existing-id')
  })

  it('regenerates id on reset and updates storage', () => {
    localStorage.setItem(KEY, 'old')
    const next = resetChatConversationId()
    expect(next).toBe('aaaaaaaa-bbbb-4ccc-dddd-eeeeeeeeeeee')
    expect(localStorage.getItem(KEY)).toBe(next)
  })

  it('falls back when localStorage getItem throws', () => {
    const spy = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('blocked')
    })
    const id = getOrCreateChatConversationId()
    expect(id.length).toBeGreaterThan(0)
    spy.mockRestore()
  })
})
