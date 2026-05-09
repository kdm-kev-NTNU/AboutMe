/**
 * E2E coverage for speech-to-text on the chat page (ChatView).
 *
 * Covers happy path, server failures (500/503/429), language header forwarding, and
 * verifies that error states do NOT crash the UI or navigate the user away.
 *
 * Prerequisites: preview server at baseUrl:
 * `npm run build && npx vite preview --port 4173`
 * then: `npx cypress run --spec cypress/e2e/transcription-flow.cy.ts`
 */

const FAKE_MODELS = [
  { id: 'gpt-5.4-mini', provider: 'OPENAI', label: 'GPT-5.4 mini', tags: ['FAST'] },
]

/**
 * useSpeechTranscription starts MediaRecorder with a 250ms timeslice; stopping immediately yields an empty Blob and skips fetch.
 * voice-waveform.cy.ts avoids this accidentally via screenshot latency — we wait explicitly here.
 */
function waitForRecordedAudioChunks() {
  // Aligns with useSpeechTranscription's 250ms MediaRecorder timeslice; stopping sooner yields an empty blob (no /transcribe).
  // Slightly above 250ms × 2 so CI/Electron scheduling misses fewer chunks (800ms for slow runners).
  // eslint-disable-next-line cypress/no-unnecessary-waiting
  cy.wait(800)
}

function withFakeMicrophone() {
  return {
    onBeforeLoad(win: Cypress.AUTWindow) {
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
            osc.frequency.value = 440
            osc.start()
            return dst.stream
          },
        },
      })
    },
  }
}

describe('Transcription flow', () => {
  beforeEach(() => {
    cy.intercept('GET', '**/api/chat/models', {
      statusCode: 200,
      body: FAKE_MODELS,
    }).as('chatModels')
    // After STT, user can send from the composer; preview has no backend — stub /ask for the happy path.
    cy.intercept('POST', '**/ask', {
      statusCode: 200,
      body: { answer: '(e2e stub)', sources: [] },
    }).as('askStub')
  })

  it('fills the composer with the transcript and can send on a successful round-trip', () => {
    cy.intercept('POST', '**/transcribe', {
      statusCode: 200,
      body: { text: 'tell me about kevin' },
    }).as('transcribe')

    cy.visit('/chat', withFakeMicrophone())
    cy.wait('@chatModels')

    cy.get('main form', { timeout: 15_000 }).find('[aria-label="Voice input"]').as('mic')
    cy.get('@mic').scrollIntoView()
    cy.get('@mic').click({ force: true })

    cy.get('[role="img"][aria-label="Audio level while recording"]', { timeout: 15_000 })
      .should('be.visible')

    waitForRecordedAudioChunks()
    cy.get('main form', { timeout: 15_000 }).find('[aria-label="Voice input"]').click({ force: true })

    cy.wait('@transcribe', { timeout: 15_000 }).then((intercept) => {
      // Frontend must send the X-Chat-Language header so the backend can pick the right Whisper hint.
      expect(intercept.request.headers).to.have.property('x-chat-language', 'en')
      expect(intercept.request.headers['content-type']).to.match(/^multipart\/form-data;\s*boundary=/i)
    })

    cy.location('pathname', { timeout: 5_000 }).should('eq', '/chat')
    cy.get('main form input[type="text"]').should('have.value', 'tell me about kevin')
    cy.contains('button', /send/i).click()
    cy.wait('@askStub')
  })

  it('shows the destructive error alert when /transcribe returns 500', () => {
    cy.intercept('POST', '**/transcribe', {
      statusCode: 500,
      body: { error: 'Transcription failed unexpectedly. Please try again.' },
    }).as('transcribe')

    cy.visit('/chat', withFakeMicrophone())
    cy.wait('@chatModels')

    cy.get('main form', { timeout: 15_000 }).find('[aria-label="Voice input"]').as('mic')
    cy.get('@mic').scrollIntoView()
    cy.get('@mic').click({ force: true })

    cy.get('[role="img"][aria-label="Audio level while recording"]', { timeout: 15_000 })
      .should('be.visible')

    waitForRecordedAudioChunks()
    cy.get('main form', { timeout: 15_000 }).find('[aria-label="Voice input"]').click({ force: true })
    cy.wait('@transcribe', { timeout: 15_000 })

    // User-friendly canned message (we deliberately do NOT leak server text), and we stay on chat.
    cy.contains(/Transcription failed on the server/i, { timeout: 5_000 }).should('be.visible')
    cy.location('pathname').should('eq', '/chat')
  })

  it('shows the rate-limit message on 429', () => {
    cy.intercept('POST', '**/transcribe', {
      statusCode: 429,
      body: { error: 'over budget' },
    }).as('transcribe')

    cy.visit('/chat', withFakeMicrophone())
    cy.wait('@chatModels')

    cy.get('main form', { timeout: 15_000 }).find('[aria-label="Voice input"]').as('mic')
    cy.get('@mic').scrollIntoView()
    cy.get('@mic').click({ force: true })

    cy.get('[role="img"][aria-label="Audio level while recording"]', { timeout: 15_000 })
      .should('be.visible')

    waitForRecordedAudioChunks()
    cy.get('main form', { timeout: 15_000 }).find('[aria-label="Voice input"]').click({ force: true })
    cy.wait('@transcribe', { timeout: 15_000 })

    cy.contains(/Too many requests or budget limit/i, { timeout: 5_000 }).should('be.visible')
    cy.location('pathname').should('eq', '/chat')
  })

  it('shows the temporarily-unavailable message on 503', () => {
    cy.intercept('POST', '**/transcribe', {
      statusCode: 503,
      body: { error: 'Speech-to-text is temporarily unavailable.' },
    }).as('transcribe')

    cy.visit('/chat', withFakeMicrophone())
    cy.wait('@chatModels')

    cy.get('main form', { timeout: 15_000 }).find('[aria-label="Voice input"]').as('mic')
    cy.get('@mic').scrollIntoView()
    cy.get('@mic').click({ force: true })

    cy.get('[role="img"][aria-label="Audio level while recording"]', { timeout: 15_000 })
      .should('be.visible')

    waitForRecordedAudioChunks()
    cy.get('main form', { timeout: 15_000 }).find('[aria-label="Voice input"]').click({ force: true })
    cy.wait('@transcribe', { timeout: 15_000 })

    cy.contains(/Speech-to-text is temporarily unavailable/i, { timeout: 5_000 })
      .should('be.visible')
    cy.location('pathname').should('eq', '/chat')
  })

  it('forwards Norwegian X-Chat-Language when the UI is in Norwegian', () => {
    cy.intercept('POST', '**/transcribe', {
      statusCode: 200,
      body: { text: 'hei kevin' },
    }).as('transcribe')

    cy.visit('/chat', {
      onBeforeLoad(win) {
        win.sessionStorage.clear()
        win.localStorage.setItem('lang', 'no')
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

    cy.get('main form', { timeout: 15_000 }).find('[aria-label="Taleinndata"]').as('mic')
    cy.get('@mic').scrollIntoView()
    cy.get('@mic').click({ force: true })

    cy.get('[role="img"][aria-label="Lydnivå under opptak"]', { timeout: 15_000 })
      .should('be.visible')

    waitForRecordedAudioChunks()
    cy.get('main form', { timeout: 15_000 }).find('[aria-label="Taleinndata"]').click({ force: true })

    cy.wait('@transcribe', { timeout: 15_000 }).then((intercept) => {
      expect(intercept.request.headers).to.have.property('x-chat-language', 'no')
    })
  })

  it('keeps the mic button disabled while a transcription is in flight', () => {
    cy.intercept('POST', '**/transcribe', (req) => {
      req.reply({
        statusCode: 200,
        body: { text: 'delayed reply' },
        delay: 800,
      })
    }).as('transcribe')

    cy.visit('/chat', withFakeMicrophone())
    cy.wait('@chatModels')

    cy.get('main form', { timeout: 15_000 }).find('[aria-label="Voice input"]').as('mic')
    cy.get('@mic').scrollIntoView()
    cy.get('@mic').click({ force: true })

    cy.get('[role="img"][aria-label="Audio level while recording"]', { timeout: 15_000 })
      .should('be.visible')

    waitForRecordedAudioChunks()
    cy.get('main form', { timeout: 15_000 }).find('[aria-label="Voice input"]').click({ force: true })

    // While the request is mid-flight, the mic must be disabled to prevent overlapping recordings.
    cy.get('main form', { timeout: 15_000 })
      .find('[aria-label="Voice input"]', { timeout: 5_000 })
      .should('be.disabled')

    cy.wait('@transcribe', { timeout: 15_000 })
    cy.location('pathname', { timeout: 5_000 }).should('eq', '/chat')
  })
})
