/**
 * Captures the live waveform region during recording for visual regression / proof.
 *
 * Prerequisites: preview server at baseUrl:
 * `npm run build && npx vite preview --port 4173`
 * then: `npx cypress run --spec cypress/e2e/voice-waveform.cy.ts`
 */

describe('Voice waveform visual', () => {
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
    cy.intercept('POST', '**/transcribe', { statusCode: 200, body: { text: 'ok' } }).as('transcribePost')
  })

  it('shows waveform canvas while recording (screenshot)', () => {
    cy.visit('/', {
      onBeforeLoad(win) {
        /** Otherwise navigator locale can pick NO and Mic uses aria-label Taleinndata */
        win.localStorage.setItem('lang', 'en')
        Object.defineProperty(win.navigator, 'mediaDevices', {
          configurable: true,
          value: {
            getUserMedia: async () => {
              const ctx = new win.AudioContext()
              await ctx.resume().catch(() => {})
              const osc = ctx.createOscillator()
              const dst = ctx.createMediaStreamDestination()
              osc.connect(dst)
              osc.frequency.value = 440
              osc.start()
              return dst.stream
            },
          },
        })
      },
    })
    cy.wait('@chatModels')

    cy.get('main').scrollTo('bottom', { ensureScrollable: false })

    cy.get('main form', { timeout: 15000 }).find('[aria-label="Voice input"]').scrollIntoView()
    cy.get('main form', { timeout: 15000 })
      .find('[aria-label="Voice input"]')
      .should('be.visible')
      /** Cookie / feedback widgets can still occlude the mic in the corner */
      .click({ force: true })

    cy.get('[role="img"][aria-label="Audio level while recording"]', { timeout: 15000 }).should('be.visible')
    cy.get('[role="img"][aria-label="Audio level while recording"] canvas', { timeout: 5000 }).should(($canvas) => {
      expect($canvas.width(), 'waveform canvas has layout width').to.be.greaterThan(0)
    })

    cy.screenshot('waveform-home-recording', { capture: 'viewport' })

    cy.get('main form').find('[aria-label="Voice input"]').click({ force: true })

    cy.wait('@transcribePost')
  })

  it('shows waveform canvas on chat while recording (screenshot)', () => {
    cy.intercept('POST', '**/ask', {
      statusCode: 200,
      body: { answer: 'hi' },
    }).as('askPost')

    cy.visit('/chat', {
      onBeforeLoad(win) {
        win.localStorage.setItem('lang', 'en')
        win.localStorage.setItem('chatInfoPopupDismissed.v2', 'true')
        Object.defineProperty(win.navigator, 'mediaDevices', {
          configurable: true,
          value: {
            getUserMedia: async () => {
              const ctx = new win.AudioContext()
              await ctx.resume().catch(() => {})
              const osc = ctx.createOscillator()
              const dst = ctx.createMediaStreamDestination()
              osc.connect(dst)
              osc.frequency.value = 330
              osc.start()
              return dst.stream
            },
          },
        })
      },
    })
    cy.wait('@chatModels')

    cy.get('main form').find('[aria-label="Voice input"]').scrollIntoView()
    cy.get('main form').find('[aria-label="Voice input"]').should('be.visible').click()

    cy.get('[role="img"][aria-label="Audio level while recording"]', { timeout: 15000 }).should('be.visible')
    cy.get('[role="img"][aria-label="Audio level while recording"] canvas', { timeout: 5000 }).should(($canvas) => {
      expect($canvas.width(), 'waveform canvas has layout width').to.be.greaterThan(0)
    })

    cy.screenshot('waveform-chat-recording', { capture: 'viewport' })

    cy.get('main form').find('[aria-label="Voice input"]').click()

    cy.wait('@transcribePost')
  })
})
