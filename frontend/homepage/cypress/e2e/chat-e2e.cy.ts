/**
 * Full chat flow e2e tests with stubbed backend.
 * Covers: send/receive, markdown rendering, error states (429/503/400),
 * conversation ID header, sessionStorage model persistence, and clear chat.
 */

const ALL_MODELS = [
  { id: 'gpt-5.4-mini', provider: 'OPENAI', label: 'GPT-5.4 mini', tags: ['FAST'] },
  { id: 'gpt-5.4', provider: 'OPENAI', label: 'GPT-5.4', tags: ['REASONING'] },
  { id: 'claude-haiku-4-5-20251001', provider: 'ANTHROPIC', label: 'Claude Haiku 4.5', tags: ['FAST'] },
]

function stubCatalogAndAsk(askStatus: number, askBody: object) {
  cy.intercept('GET', '/api/**', (req) => {
    if (req.url.includes('/chat/models')) {
      req.reply({ statusCode: 200, body: ALL_MODELS })
    } else {
      req.reply({ statusCode: 200, body: [] })
    }
  }).as('apiGet')

  cy.intercept('POST', '**/ask', {
    statusCode: askStatus,
    body: askBody,
  }).as('askPost')
}

describe('Chat e2e flow', () => {
  beforeEach(() => {
    cy.window().then((win) => win.sessionStorage.clear())
  })

  it('sends a question and receives a rendered answer', () => {
    stubCatalogAndAsk(200, { answer: 'Hello! I am **Kevin\'s AI**.' })
    cy.visit('/chat')

    cy.get('input[type="text"]').type('Who are you?')
    cy.contains('button', /send/i).click()

    cy.wait('@askPost').its('request.headers').should('have.property', 'x-conversation-id')

    cy.contains('Who are you?').should('exist')
    cy.contains("Kevin's AI").should('exist')
  })

  it('shows error on 429 rate limit', () => {
    stubCatalogAndAsk(429, { error: 'Rate limit exceeded' })
    cy.visit('/chat')

    cy.get('input[type="text"]').type('Hello')
    cy.contains('button', /send/i).click()

    cy.wait('@askPost')
    cy.contains(/for mange|rate limit/i).should('be.visible')
  })

  it('shows error on 503 service unavailable', () => {
    stubCatalogAndAsk(503, { error: 'AI service temporarily unavailable' })
    cy.visit('/chat')

    cy.get('input[type="text"]').type('Hello')
    cy.contains('button', /send/i).click()

    cy.wait('@askPost')
    cy.contains(/gikk galt|unavailable|prøv igjen/i).should('be.visible')
  })

  it('shows error on 400 bad request', () => {
    stubCatalogAndAsk(400, { error: 'Unknown chat model.' })
    cy.visit('/chat')

    cy.get('input[type="text"]').type('Hello')
    cy.contains('button', /send/i).click()

    cy.wait('@askPost')
    cy.contains(/unknown|gikk galt/i).should('be.visible')
  })

  it('persists selected model in sessionStorage across navigation', () => {
    cy.intercept('GET', '/api/**', (req) => {
      if (req.url.includes('/chat/models')) {
        req.reply({ statusCode: 200, body: ALL_MODELS })
      } else {
        req.reply({ statusCode: 200, body: [] })
      }
    }).as('apiGet')

    cy.visit('/chat')
    cy.get('#chat-model-select').should('exist')

    cy.get('#chat-model-select').select('gpt-5.4')
    cy.window().then((win) => {
      expect(win.sessionStorage.getItem('chatSelectedModel')).to.eq('gpt-5.4')
    })

    cy.visit('/')
    cy.visit('/chat')

    cy.window().then((win) => {
      expect(win.sessionStorage.getItem('chatSelectedModel')).to.eq('gpt-5.4')
    })
    cy.get('#chat-model-select').should('have.value', 'gpt-5.4')
  })

  it('clear chat removes messages and resets state', () => {
    stubCatalogAndAsk(200, { answer: 'Reply one' })
    cy.visit('/chat')

    cy.get('input[type="text"]').type('First message')
    cy.contains('button', /send/i).click()
    cy.wait('@askPost')
    cy.contains('Reply one').should('exist')

    cy.contains('button', /clear chat/i).click()

    cy.contains('First message').should('not.exist')
    cy.contains('Reply one').should('not.exist')
  })

  it('disables send button while loading', () => {
    cy.intercept('GET', '/api/**', (req) => {
      if (req.url.includes('/chat/models')) {
        req.reply({ statusCode: 200, body: ALL_MODELS })
      } else {
        req.reply({ statusCode: 200, body: [] })
      }
    }).as('apiGet')

    cy.intercept('POST', '**/ask', (req) => {
      req.reply({ statusCode: 200, body: { answer: 'Delayed reply' }, delay: 1000 })
    }).as('askPost')

    cy.visit('/chat')
    cy.get('input[type="text"]').type('Hello')
    cy.contains('button', /send/i).click()

    cy.contains('button', /sending/i).should('be.disabled')
  })

  it('rejects prompt exceeding max length client-side', () => {
    stubCatalogAndAsk(200, { answer: 'Should not appear' })
    cy.visit('/chat')

    const longText = 'a'.repeat(3001)
    cy.get('input[type="text"]').invoke('val', longText).trigger('input')
    cy.contains('button', /send/i).click()

    cy.contains(/for lang|too long/i).should('be.visible')
    cy.get('@askPost.all').should('have.length', 0)
  })
})
