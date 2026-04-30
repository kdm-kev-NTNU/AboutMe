describe('Chat model catalog', () => {
  it('loads models with tags, defaults to FAST, and switches provider subset', () => {
    cy.intercept('GET', '/api/**', (req) => {
      if (req.url.includes('/chat/models')) {
        req.reply({
          statusCode: 200,
          body: [
            {
              id: 'gpt-5.4',
              provider: 'OPENAI',
              label: 'GPT-5.4',
              tags: ['REASONING'],
            },
            {
              id: 'gpt-5.4-mini',
              provider: 'OPENAI',
              label: 'GPT-5.4 mini',
              tags: ['FAST'],
            },
            {
              id: 'claude-haiku-4-5-20251001',
              provider: 'ANTHROPIC',
              label: 'Claude Haiku 4.5',
              tags: ['FAST'],
            },
            {
              id: 'claude-sonnet-4-6',
              provider: 'ANTHROPIC',
              label: 'Claude Sonnet 4.6',
              tags: ['REASONING'],
            },
          ],
        })
      } else {
        req.reply({ statusCode: 200, body: [] })
      }
    }).as('apiGet')

    cy.visit('/chat')

    cy.get('#chat-model-select').should('exist')
    cy.get('#chat-model-select').find('option').should('have.length', 2)
    cy.get('#chat-model-select').find('option').contains('Fast')
    cy.get('#chat-model-select').find('option').contains('Reasoning')
    cy.get('#chat-model-select').should('have.value', 'gpt-5.4-mini')

    cy.contains('button', /^Anthropic$/i).click()
    cy.get('#chat-model-select').find('option').should('have.length', 2)
    cy.get('#chat-model-select').should('have.value', 'claude-haiku-4-5-20251001')
    cy.get('#chat-model-select').find('option').contains('Haiku')
  })
})
