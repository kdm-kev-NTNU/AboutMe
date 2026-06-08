describe('Admin interview smoke', () => {
  beforeEach(() => {
    cy.window().then((win) => {
      win.sessionStorage.setItem(
        'auth',
        JSON.stringify({ username: 'admin', role: 'ADMIN' }),
      )
    })
  })

  it('loads interview admin page and shows wizard', () => {
    cy.visit('/admin/interview')
    cy.contains('Stemmeintervju', { matchCase: false })
    cy.contains('Spørsmål', { matchCase: false })
    cy.contains('Last opp fil med spørsmål', { matchCase: false })
  })
})
