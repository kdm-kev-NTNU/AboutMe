/**
 * Short cross-cutting smoke over the production preview server.
 * Keeps a single place for "must not break" journeys; detailed behaviour stays in chat-e2e, public-pages, admin-smoke, etc.
 *
 * Voice/realtime deep checks: see `voice-live-smoke.cy.ts` and workflow_dispatch job `.github/workflows/voice-live-smoke.yml`
 * (requires base URL + optional secrets for live token/OpenAI SDP).
 */

const MODELS = [
  { id: 'gpt-5.4-mini', provider: 'OPENAI', label: 'GPT-5.4 mini', tags: ['FAST'] },
]

describe('Critical full-stack smoke', () => {
  beforeEach(() => {
    cy.intercept('GET', '**/api/chat/models', { statusCode: 200, body: MODELS }).as('chatModels')
    cy.intercept('GET', '**/api/realtime/status', {
      statusCode: 200,
      body: { enabled: false, providers: {} },
    }).as('realtimeStatus')
  })

  it('home loads then chat sends one stubbed ask', () => {
    cy.intercept('POST', '**/ask', { statusCode: 200, body: { answer: 'Smoke OK.' } }).as('askPost')

    cy.visit('/')
    cy.location('pathname').should('eq', '/')

    cy.visit('/chat')
    cy.window().then((win) => {
      win.localStorage.setItem('chatInfoPopupDismissed.v2', 'true')
    })

    cy.get('input[type="text"]').type('Smoke test?')
    cy.contains('button', /send/i).click()
    cy.wait('@askPost')
    cy.contains('Smoke OK.').should('be.visible')
  })

  it('voice route renders with stubbed realtime status', () => {
    cy.visit('/voice')
    cy.location('pathname').should('eq', '/voice')
  })

  it('admin tools redirects to home without ADMIN session', () => {
    cy.window().then((win) => win.sessionStorage.clear())
    cy.visit('/admin/tools')
    cy.location('pathname').should('eq', '/')
  })
})
