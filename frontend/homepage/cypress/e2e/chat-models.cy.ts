/**
 * Chat model catalog e2e tests.
 * Covers: catalog load, tag display, anonymous FAST default, per-model selection,
 * provider switching round-trip, and per-model chat submission.
 */

const FULL_CATALOG = [
  { id: 'gpt-5.4-nano', provider: 'OPENAI', label: 'GPT-5.4 Nano', tags: ['FAST'] },
  { id: 'gpt-5.4-mini', provider: 'OPENAI', label: 'GPT-5.4 mini', tags: ['FAST'] },
  { id: 'gpt-5.4', provider: 'OPENAI', label: 'GPT-5.4', tags: ['REASONING'] },
  { id: 'gpt-5.5', provider: 'OPENAI', label: 'GPT-5.5', tags: ['REASONING'] },
  { id: 'claude-haiku-4-5-20251001', provider: 'ANTHROPIC', label: 'Claude Haiku 4.5', tags: ['FAST'] },
  { id: 'claude-sonnet-4-6', provider: 'ANTHROPIC', label: 'Claude Sonnet 4.6', tags: ['REASONING'] },
  { id: 'claude-opus-4-7', provider: 'ANTHROPIC', label: 'Claude Opus 4.7', tags: ['REASONING'] },
]

function stubFullCatalog() {
  cy.intercept('GET', '/api/**', (req) => {
    if (req.url.includes('/chat/models')) {
      req.reply({ statusCode: 200, body: FULL_CATALOG })
    } else {
      req.reply({ statusCode: 200, body: [] })
    }
  }).as('apiGet')
}

describe('Chat model catalog', () => {
  beforeEach(() => {
    cy.window().then((win) => win.sessionStorage.clear())
  })

  it('loads models with tags, defaults to FAST when anonymous, and switches provider subset', () => {
    stubFullCatalog()
    cy.visit('/chat')

    cy.get('#chat-model-select').should('exist')
    cy.get('#chat-model-select').find('option').should('have.length.at.least', 2)
    cy.get('#chat-model-select').find('option').contains('Fast')
    cy.get('#chat-model-select').find('option').contains('Reasoning')
    cy.get('#chat-model-select').should('have.value', 'gpt-5.4-nano')

    cy.contains('button', /^Anthropic$/i).click()
    cy.get('#chat-model-select').find('option').should('have.length.at.least', 2)
    cy.get('#chat-model-select').should('have.value', 'claude-haiku-4-5-20251001')
    cy.get('#chat-model-select').find('option').contains('Haiku')
  })

  it('can select every OpenAI model individually', () => {
    stubFullCatalog()
    cy.visit('/chat')

    const openaiModels = FULL_CATALOG.filter((m) => m.provider === 'OPENAI')
    openaiModels.forEach((model) => {
      cy.get('#chat-model-select').select(model.id)
      cy.get('#chat-model-select').should('have.value', model.id)
    })
  })

  it('can select every Anthropic model individually', () => {
    stubFullCatalog()
    cy.visit('/chat')

    cy.contains('button', /^Anthropic$/i).click()

    const anthropicModels = FULL_CATALOG.filter((m) => m.provider === 'ANTHROPIC')
    anthropicModels.forEach((model) => {
      cy.get('#chat-model-select').select(model.id)
      cy.get('#chat-model-select').should('have.value', model.id)
    })
  })

  it('provider toggle switches between OpenAI and Anthropic models', () => {
    stubFullCatalog()
    cy.visit('/chat')

    cy.get('#chat-model-select').should('have.value', 'gpt-5.4-nano')

    cy.contains('button', /^Anthropic$/i).click()
    cy.get('#chat-model-select').should('have.value', 'claude-haiku-4-5-20251001')
    cy.get('#chat-model-select').find('option').each(($opt) => {
      expect($opt.text()).to.match(/claude|anthropic/i)
    })

    cy.contains('button', /^OpenAI$/i).click()
    cy.get('#chat-model-select').should('have.value', 'gpt-5.4-nano')
    cy.get('#chat-model-select').find('option').each(($opt) => {
      expect($opt.text()).to.match(/gpt|openai/i)
    })
  })

  it('sends the selected model in the /ask request body', () => {
    stubFullCatalog()
    cy.intercept('POST', '**/ask', { statusCode: 200, body: { answer: 'ok' } }).as('askPost')

    cy.visit('/chat')
    cy.get('#chat-model-select').select('gpt-5.4')
    cy.get('input[type="text"]').type('Test question')
    cy.contains('button', /send/i).click()

    cy.wait('@askPost').its('request.body').should('have.property', 'model', 'gpt-5.4')
  })

  it('sends the selected Anthropic model in the /ask request body', () => {
    stubFullCatalog()
    cy.intercept('POST', '**/ask', { statusCode: 200, body: { answer: 'ok' } }).as('askPost')

    cy.visit('/chat')
    cy.contains('button', /^Anthropic$/i).click()
    cy.get('#chat-model-select').select('claude-sonnet-4-6')
    cy.get('input[type="text"]').type('Test question')
    cy.contains('button', /send/i).click()

    cy.wait('@askPost').its('request.body').should('have.property', 'model', 'claude-sonnet-4-6')
  })

  it('round-trip: switch providers, select model, send, switch back', () => {
    stubFullCatalog()
    cy.intercept('POST', '**/ask', { statusCode: 200, body: { answer: 'reply' } }).as('askPost')

    cy.visit('/chat')

    cy.contains('button', /^Anthropic$/i).click()
    cy.get('#chat-model-select').select('claude-opus-4-7')
    cy.get('input[type="text"]').type('Q1')
    cy.contains('button', /send/i).click()
    cy.wait('@askPost').its('request.body').should('have.property', 'model', 'claude-opus-4-7')

    cy.contains('button', /^OpenAI$/i).click()
    cy.get('#chat-model-select').select('gpt-5.5')
    cy.get('input[type="text"]').type('Q2')
    cy.contains('button', /send/i).click()
    cy.wait('@askPost').its('request.body').should('have.property', 'model', 'gpt-5.5')
  })
})
