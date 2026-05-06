/**
 * Cookie settings modal via footer (works without PostHog banner).
 */

describe('Cookie consent settings', () => {
  beforeEach(() => {
    cy.clearAllLocalStorage()
    cy.intercept('GET', '**/api/chat/models', {
      statusCode: 200,
      body: [
        {
          id: 'gpt-5.4-mini',
          provider: 'OPENAI',
          label: 'GPT-5.4 mini',
          tags: ['FAST'],
        },
      ],
    }).as('chatModels')
  })

  it('opens cookie settings from footer, cancels, then saves analytics choice', () => {
    cy.visit('/')
    cy.wait('@chatModels')

    cy.contains('button', 'Cookie Settings').click()
    cy.get('[role="dialog"]').should('be.visible').and('contain.text', 'Cookies and analytics')

    cy.contains('button', 'Cancel').click()
    cy.get('[role="dialog"]').should('not.exist')

    cy.contains('button', 'Cookie Settings').click()
    cy.get('[role="dialog"]').within(() => {
      cy.get('input[type="checkbox"]:not([disabled])').should('have.length', 4)
      cy.get('input[type="checkbox"]:not([disabled])').eq(0).check({ force: true })
      cy.get('input[type="checkbox"]:not([disabled])').eq(1).check({ force: true })
    })
    cy.contains('button', 'Save choices').click()

    cy.window().then((win) => {
      expect(win.localStorage.getItem('aboutme_cookie_consent_v2')).to.be.a('string')
    })
  })
})
