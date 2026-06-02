/**
 * Smoke navigation across public portfolio routes (no backend required).
 */

describe('Portfolio browsing', () => {
  function visitEn(path: string) {
    cy.visit(path, {
      onBeforeLoad(win) {
        win.localStorage.setItem('lang', 'en')
      },
    })
  }

  it('loads home, projects, project page, and career', () => {
    visitEn('/')
    cy.contains("Kevin's").should('be.visible')

    visitEn('/projects')
    cy.url().should('include', '/reason')
    cy.url().should('include', '#projects')
    cy.contains('Projects', { timeout: 30000 }).should('be.visible')
    cy.contains('AboutMe').should('be.visible')

    visitEn('/project')
    cy.url().should('include', '/reason')
    cy.contains('Projects', { timeout: 30000 }).should('be.visible')

    visitEn('/career')
    cy.contains(/Experience and education|Erfaring og utdanning/).should('be.visible')
  })
})
