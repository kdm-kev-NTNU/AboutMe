// https://on.cypress.io/api

describe('Core frontend flows', () => {
  beforeEach(() => {
    cy.intercept('GET', '/api/**', {
      statusCode: 200,
      body: [],
    }).as('apiGet')
  })

  it('loads the home page and shows hero content', () => {
    cy.visit('/')
    cy.get('h1').should('contain.text', 'Kevin')
    cy.contains('button', /send/i).should('exist')
  })

  it('opens and uses chat flow entry points', () => {
    cy.visit('/chat')
    cy.get('h1').should('contain.text', "Kevin's AI")
    cy.get('textarea, input').first().should('exist')
  })

  it('redirects unauthenticated user away from admin area', () => {
    cy.visit('/admin/tools', { failOnStatusCode: false })
    cy.url().should('not.include', '/admin')
    cy.location('pathname').should('eq', '/')
  })
})
