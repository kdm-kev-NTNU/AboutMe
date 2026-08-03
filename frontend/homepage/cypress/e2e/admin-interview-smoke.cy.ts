describe('Admin interview smoke', () => {
  beforeEach(() => {
    cy.intercept('GET', '**/api/auth/me', {
      statusCode: 200,
      body: { username: 'admin', role: 'ADMIN' },
    }).as('authMe')

    cy.intercept('GET', '**/api/realtime/status', {
      statusCode: 200,
      body: {
        liveEnabled: false,
        voice: 'marin',
        reasoningEffort: 'low',
      },
    }).as('realtimeStatus')
  })

  it('loads interview admin page and shows wizard', () => {
    cy.visit('/admin/interview', {
      onBeforeLoad(win) {
        win.sessionStorage.setItem(
          'auth',
          JSON.stringify({ username: 'admin', role: 'ADMIN' }),
        )
      },
    })
    cy.wait('@authMe')
    cy.contains('Stemmeintervju', { matchCase: false })
    cy.contains('Spørsmål', { matchCase: false })
    cy.contains('Last opp fil med spørsmål', { matchCase: false })
  })
})
