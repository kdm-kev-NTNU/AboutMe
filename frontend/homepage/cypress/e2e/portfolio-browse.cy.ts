/**
 * Smoke navigation across public portfolio routes (no backend required).
 */

describe('Portfolio browsing', () => {
  beforeEach(() => {
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

  it('loads home, projects, project page, and career', () => {
    cy.visit('/')
    cy.wait('@chatModels')
    cy.contains("Kevin's").should('be.visible')

    cy.visit('/projects')
    cy.contains('Projects', { timeout: 30000 }).should('be.visible')

    cy.visit('/project')
    cy.contains(/Tech stack|Teknologistakk/).should('be.visible')

    cy.visit('/career')
    cy.contains(/Experience & education|Erfaring og utdanning/).should('be.visible')
  })
})
