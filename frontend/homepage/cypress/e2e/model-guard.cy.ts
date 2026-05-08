/**
 * Model catalog edge-case tests.
 * Covers: empty catalog, OpenAI-only, Anthropic-only scenarios.
 */

describe('Model catalog guard rails', () => {
  it('gracefully handles empty model catalog', () => {
    cy.intercept('GET', '/api/**', (req) => {
      if (req.url.includes('/chat/models')) {
        req.reply({ statusCode: 200, body: [] })
      } else {
        req.reply({ statusCode: 200, body: [] })
      }
    }).as('apiGet')

    cy.visit('/chat')
    cy.get('#chat-model-select').should('not.exist')
    cy.get('input[type="text"]').should('exist')
  })

  it('hides provider toggle when only OpenAI models are available', () => {
    cy.intercept('GET', '/api/**', (req) => {
      if (req.url.includes('/chat/models')) {
        req.reply({
          statusCode: 200,
          body: [{ id: 'gpt-5.4-mini', provider: 'OPENAI', label: 'GPT-5.4 mini', tags: ['FAST'] }],
        })
      } else {
        req.reply({ statusCode: 200, body: [] })
      }
    }).as('apiGet')

    cy.visit('/chat')
    cy.get('#chat-model-select').should('exist')
    cy.get('#chat-model-select').find('option').should('have.length', 1)

    cy.contains('button', /^OpenAI$/i).should('not.exist')
    cy.contains('button', /^Anthropic$/i).should('not.exist')
  })

  it('hides provider toggle when only Anthropic models are available', () => {
    cy.intercept('GET', '/api/**', (req) => {
      if (req.url.includes('/chat/models')) {
        req.reply({
          statusCode: 200,
          body: [
            {
              id: 'claude-haiku-4-5-20251001',
              provider: 'ANTHROPIC',
              label: 'Claude Haiku 4.5',
              tags: ['FAST'],
            },
          ],
        })
      } else {
        req.reply({ statusCode: 200, body: [] })
      }
    }).as('apiGet')

    cy.visit('/chat')
    cy.get('#chat-model-select').should('exist')
    cy.get('#chat-model-select').find('option').should('have.length', 1)

    cy.contains('button', /^OpenAI$/i).should('not.exist')
    cy.contains('button', /^Anthropic$/i).should('not.exist')
  })

  it('shows provider toggle only when both providers have models', () => {
    cy.intercept('GET', '/api/**', (req) => {
      if (req.url.includes('/chat/models')) {
        req.reply({
          statusCode: 200,
          body: [
            { id: 'gpt-5.4-mini', provider: 'OPENAI', label: 'GPT-5.4 mini', tags: ['FAST'] },
            {
              id: 'claude-haiku-4-5-20251001',
              provider: 'ANTHROPIC',
              label: 'Claude Haiku 4.5',
              tags: ['FAST'],
            },
          ],
        })
      } else {
        req.reply({ statusCode: 200, body: [] })
      }
    }).as('apiGet')

    cy.visit('/chat')

    cy.contains('button', /^OpenAI$/i).should('exist')
    cy.contains('button', /^Anthropic$/i).should('exist')
  })

  it('handles /chat/models endpoint failure gracefully', () => {
    cy.intercept('GET', '/api/**', (req) => {
      if (req.url.includes('/chat/models')) {
        req.reply({ statusCode: 500, body: { error: 'Internal server error' } })
      } else {
        req.reply({ statusCode: 200, body: [] })
      }
    }).as('apiGet')

    cy.visit('/chat')
    cy.get('#chat-model-select').should('not.exist')
    cy.get('input[type="text"]').should('exist')
  })

  it('defaults to FAST model when catalog loads', () => {
    cy.intercept('GET', '/api/**', (req) => {
      if (req.url.includes('/chat/models')) {
        req.reply({
          statusCode: 200,
          body: [
            { id: 'gpt-5.4', provider: 'OPENAI', label: 'GPT-5.4', tags: ['REASONING'] },
            { id: 'gpt-5.4-mini', provider: 'OPENAI', label: 'GPT-5.4 mini', tags: ['FAST'] },
          ],
        })
      } else {
        req.reply({ statusCode: 200, body: [] })
      }
    }).as('apiGet')

    cy.visit('/chat')
    cy.get('#chat-model-select').should('have.value', 'gpt-5.4-mini')
  })
})
