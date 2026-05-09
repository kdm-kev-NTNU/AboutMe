/**
 * Smoke coverage for static/public routes, redirects, and admin guards (stubbed API where layout needs models).
 */

const FAKE_MODELS = [
  { id: 'gpt-5.4-mini', provider: 'OPENAI', label: 'GPT-5.4 mini', tags: ['FAST'] },
]

const ADMIN_PATHS = [
  '/admin/tools',
  '/admin/pipeline',
  '/admin/chunks',
  '/admin/question-suggestions',
  '/admin/prompts',
  '/admin/experiments',
]

describe('Public pages and redirects', () => {
  beforeEach(() => {
    cy.intercept('GET', '**/api/chat/models', {
      statusCode: 200,
      body: FAKE_MODELS,
    }).as('chatModels')
  })

  it('shows Privacy Policy in English', () => {
    cy.visit('/privacy-policy', {
      onBeforeLoad(win) {
        win.localStorage.setItem('lang', 'en')
      },
    })
    cy.contains('h1', 'Privacy Policy').should('be.visible')
    cy.contains('Last updated: May 2026').should('be.visible')
  })

  it('shows Personvernerklæring in Norwegian', () => {
    cy.visit('/privacy-policy', {
      onBeforeLoad(win) {
        win.localStorage.setItem('lang', 'no')
      },
    })
    cy.contains('h1', 'Personvernerklæring').should('be.visible')
    cy.contains('Sist oppdatert: mai 2026').should('be.visible')
  })

  it('loads Heathen Army project case study', () => {
    cy.visit('/projects/heathen-army', {
      onBeforeLoad(win) {
        win.localStorage.setItem('lang', 'en')
      },
    })
    cy.get('#heathen-army-title').should('contain.text', 'Heathen Army')
  })

  it('redirects /work-experience to /career', () => {
    cy.visit('/work-experience')
    cy.location('pathname').should('eq', '/career')
  })

  it('redirects /education to /career', () => {
    cy.visit('/education')
    cy.location('pathname').should('eq', '/career')
  })

  it('redirects /bachelor to /project#bachelor', () => {
    cy.visit('/bachelor')
    cy.location('pathname').should('eq', '/project')
    cy.location('hash').should('eq', '#bachelor')
  })

  it('redirects /tech-stack to /project#tech-stack', () => {
    cy.visit('/tech-stack')
    cy.location('pathname').should('eq', '/project')
    cy.location('hash').should('eq', '#tech-stack')
  })

  it('redirects /future-work to /project#future-work', () => {
    cy.visit('/future-work')
    cy.location('pathname').should('eq', '/project')
    cy.location('hash').should('eq', '#future-work')
  })

  it('redirects unauthenticated visitors away from every admin route', () => {
    cy.wrap(ADMIN_PATHS).each((path: string) => {
      cy.visit(path, { failOnStatusCode: false })
      cy.location('pathname').should('eq', '/')
    })
  })
})
