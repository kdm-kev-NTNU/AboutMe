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

  function assertRecordingWaveformCanvasReady() {
    cy.get('[role="img"][aria-label="Audio level while recording"]').within(() => {
      cy.get('canvas').should('have.prop', 'width').and('be.greaterThan', 0)
    })
  }

  it('shows waveform canvas while recording on chat STT (screenshot)', () => {
    cy.visit('/chat', {
      onBeforeLoad(win) {
        win.sessionStorage.clear()
        /** Otherwise navigator locale can pick NO and Mic uses aria-label Taleinndata */
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
              osc.frequency.value = 440
              osc.start()
              return dst.stream
            },
          },
        })
      },
    })
    cy.wait('@chatModels')

    cy.get('main form', { timeout: 15000 }).find('[aria-label="Voice input"]').as('voiceInputChatStt')
    cy.get('@voiceInputChatStt').scrollIntoView()
    cy.get('@voiceInputChatStt').should('be.visible')
    /** Cookie / feedback widgets can still occlude the mic in the corner */
    cy.get('@voiceInputChatStt').click({ force: true })

    cy.get('[role="img"][aria-label="Audio level while recording"]', { timeout: 15000 }).should('be.visible')
    assertRecordingWaveformCanvasReady()

    cy.screenshot('waveform-chat-recording-stt', { capture: 'viewport' })

    cy.get('main form [aria-label="Voice input"]').click({ force: true })

    cy.wait('@transcribePost')
  })

  it('shows waveform canvas on chat while recording (screenshot)', () => {
    cy.intercept('POST', '**/ask', {
      statusCode: 200,
      body: { answer: 'hi' },
    }).as('askPost')

    cy.visit('/chat', {
      onBeforeLoad(win) {
        win.sessionStorage.clear()
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

    cy.get('main form').find('[aria-label="Voice input"]').as('voiceInputChat')
    cy.get('@voiceInputChat').scrollIntoView()
    cy.get('@voiceInputChat').should('be.visible')
    cy.get('@voiceInputChat').click()

    cy.get('[role="img"][aria-label="Audio level while recording"]', { timeout: 15000 }).should('be.visible')
    assertRecordingWaveformCanvasReady()

    cy.screenshot('waveform-chat-recording', { capture: 'viewport' })

    cy.get('main form [aria-label="Voice input"]').click()

    cy.wait('@transcribePost')
  })
})
