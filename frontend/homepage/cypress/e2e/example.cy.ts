// https://on.cypress.io/api

describe('Home page', () => {
  it('loads without blocking tutorial modal', () => {
    cy.visit('/')
    cy.get('h1').should('contain.text', 'Kevin')
    cy.get('[data-slot="dialog-content"]').should('not.exist')
    cy.get('[role="alert"]').should('exist')
  })
})
