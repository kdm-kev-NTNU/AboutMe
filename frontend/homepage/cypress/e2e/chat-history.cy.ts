/**
 * Chat history list and conversation preview (stubbed GET /api/conversations).
 */

const FAKE_MODELS = [
  { id: 'gpt-5.4-mini', provider: 'OPENAI', label: 'GPT-5.4 mini', tags: ['FAST'] },
]

const SESSION_ROW = {
  id: 1,
  startedAt: '2026-01-10T10:00:00.000Z',
  endedAt: '2026-01-10T10:05:00.000Z',
  messageCount: 2,
  preview: 'Hello preview line for e2e',
}

const CONVERSATION_DETAIL = {
  id: 1,
  startedAt: '2026-01-10T10:00:00.000Z',
  endedAt: '2026-01-10T10:05:00.000Z',
  messages: [
    {
      id: 10,
      role: 'user',
      text: 'Hi from history',
      createdAt: '2026-01-10T10:01:00.000Z',
    },
    {
      id: 11,
      role: 'assistant',
      text: 'Assistant reply in history',
      createdAt: '2026-01-10T10:02:00.000Z',
    },
  ],
}

describe('Chat history', () => {
  beforeEach(() => {
    cy.window().then((win) => {
      win.sessionStorage.clear()
      win.localStorage.setItem('chatInfoPopupDismissed.v2', 'true')
    })
    cy.intercept('GET', '**/api/chat/models', {
      statusCode: 200,
      body: FAKE_MODELS,
    }).as('chatModels')
  })

  it('shows empty state when API returns no sessions', () => {
    cy.intercept('GET', '**/api/conversations', {
      statusCode: 200,
      body: [],
    }).as('convList')

    cy.visit('/chat-history', {
      onBeforeLoad(win) {
        win.localStorage.setItem('lang', 'en')
      },
    })

    cy.wait('@convList')
    cy.contains('No chat history yet').should('be.visible')
    cy.contains('Start a conversation to see your chat history here.').should('be.visible')
  })

  it('opens read-only dialog with messages after View', () => {
    cy.intercept('GET', '**/api/conversations', {
      statusCode: 200,
      body: [SESSION_ROW],
    }).as('convList')

    cy.intercept('GET', '**/api/conversations/1', {
      statusCode: 200,
      body: CONVERSATION_DETAIL,
    }).as('convDetail')

    cy.visit('/chat-history', {
      onBeforeLoad(win) {
        win.localStorage.setItem('lang', 'en')
      },
    })

    cy.wait('@convList')
    cy.contains('Hello preview line for e2e').should('be.visible')
    cy.contains('button', 'View').click()

    cy.wait('@convDetail')
    cy.get('[role="dialog"]').should('be.visible')
    cy.contains('Hi from history').should('be.visible')
    cy.contains('Assistant reply in history').should('be.visible')
    cy.contains('Kevin\'s AI').should('be.visible')
  })

  it('Continue navigates to chat with conversationId query', () => {
    cy.intercept('GET', '**/api/conversations', {
      statusCode: 200,
      body: [{ ...SESSION_ROW, id: 42, preview: 'Continue flow preview' }],
    }).as('convList')

    cy.intercept('GET', '**/api/conversations/42', {
      statusCode: 200,
      body: {
        ...CONVERSATION_DETAIL,
        id: 42,
        messages: CONVERSATION_DETAIL.messages,
      },
    }).as('convDetail')

    cy.visit('/chat-history', {
      onBeforeLoad(win) {
        win.localStorage.setItem('lang', 'en')
      },
    })

    cy.wait('@convList')
    cy.contains('button', 'Continue').click()

    cy.location('pathname').should('eq', '/chat')
    cy.location('search').should('include', 'conversationId=42')

    cy.wait('@convDetail')
    cy.contains('Hi from history').should('be.visible')
  })
})
