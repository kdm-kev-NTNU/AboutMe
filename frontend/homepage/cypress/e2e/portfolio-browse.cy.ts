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

  function visitEn(path: string) {
    cy.visit(path, {
      onBeforeLoad(win) {
        win.localStorage.setItem('lang', 'en')
      },
    })
  }

  it('loads home, projects, project page, and career', () => {
    visitEn('/')
    cy.wait('@chatModels')
    cy.contains("Kevin's").should('be.visible')

    visitEn('/projects')
    cy.contains('Projects', { timeout: 30000 }).should('be.visible')

    visitEn('/project')
    cy.contains(/Tech stack|Teknologistakk/).should('be.visible')

    visitEn('/career')
    cy.contains(/Experience & education|Erfaring og utdanning/).should('be.visible')
  })
})
