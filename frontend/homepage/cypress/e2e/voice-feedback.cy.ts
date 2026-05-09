/**
 * Voice page availability gate and feedback form (stubbed backend).
 */

const FAKE_MODELS = [
  { id: 'gpt-5.4-mini', provider: 'OPENAI', label: 'GPT-5.4 mini', tags: ['FAST'] },
]

describe('Voice page (realtime disabled)', () => {
  beforeEach(() => {
    cy.intercept('GET', '**/api/chat/models', {
      statusCode: 200,
      body: FAKE_MODELS,
    }).as('chatModels')
    cy.intercept('GET', '**/api/realtime/status', {
      statusCode: 200,
      body: { enabled: false },
    }).as('realtimeStatus')
  })

  it('shows unavailable copy when server reports realtime off', () => {
    cy.visit('/voice', {
      onBeforeLoad(win) {
        win.localStorage.setItem('lang', 'en')
      },
    })
    cy.wait('@realtimeStatus')
    cy.contains('Voice chat is not enabled on the server right now.').should('be.visible')
  })

  it('shows Norwegian unavailable copy when lang is no', () => {
    cy.visit('/voice', {
      onBeforeLoad(win) {
        win.localStorage.setItem('lang', 'no')
      },
    })
    cy.wait('@realtimeStatus')
    cy.contains('Stemmechat er ikke slått på hos serveren akkurat nå.').should('be.visible')
  })
})

describe('Feedback form', () => {
  beforeEach(() => {
    cy.intercept('GET', '**/api/chat/models', {
      statusCode: 200,
      body: FAKE_MODELS,
    }).as('chatModels')
  })

  it('submits feedback and shows success on 204', () => {
    cy.intercept('POST', '**/api/feedback', {
      statusCode: 204,
      body: '',
    }).as('feedbackPost')

    cy.visit('/feedback', {
      onBeforeLoad(win) {
        win.localStorage.setItem('lang', 'en')
      },
    })

    cy.get('textarea[required]').type('Great site — e2e test message.')
    cy.contains('button', 'Send feedback').click()

    cy.wait('@feedbackPost').then(({ request }) => {
      const raw = request.body
      const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
      expect(parsed).to.have.property('message').that.includes('Great site')
    })

    cy.contains('Thank you!').should('be.visible')
    cy.contains('Your feedback has been received').should('be.visible')
  })

  it('shows rate limit message on 429', () => {
    cy.intercept('POST', '**/api/feedback', {
      statusCode: 429,
      body: { error: 'too many' },
    }).as('feedbackPost')

    cy.visit('/feedback', {
      onBeforeLoad(win) {
        win.localStorage.setItem('lang', 'en')
      },
    })

    cy.get('textarea[required]').type('Spam attempt')
    cy.contains('button', 'Send feedback').click()

    cy.wait('@feedbackPost')
    cy.contains('Too many submissions. Please wait a moment before trying again.').should('be.visible')
  })

  it('shows generic error on 400', () => {
    cy.intercept('POST', '**/api/feedback', {
      statusCode: 400,
      body: { error: 'Invalid payload' },
    }).as('feedbackPost')

    cy.visit('/feedback', {
      onBeforeLoad(win) {
        win.localStorage.setItem('lang', 'en')
      },
    })

    cy.get('textarea[required]').type('Bad req')
    cy.contains('button', 'Send feedback').click()

    cy.wait('@feedbackPost')
    cy.contains('Invalid payload').should('be.visible')
  })
})
