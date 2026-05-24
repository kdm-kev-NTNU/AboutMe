function waitForRecordedAudioChunks() {
  // Aligns with useSpeechTranscription's 250ms MediaRecorder timeslice; stopping sooner yields an empty blob (no /transcribe).
  // eslint-disable-next-line cypress/no-unnecessary-waiting
  cy.wait(800)
}

function withFakeMicrophone() {
  return {
    onBeforeLoad(win: Cypress.AUTWindow) {
      win.sessionStorage.clear()
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
  }
}

describe('Standard voice mode', () => {
  it('runs STT -> lookup -> synthesize pipeline', () => {
    cy.intercept('GET', '**/api/realtime/status', {
      statusCode: 200,
      body: {
        enabled: true,
        standardEnabled: true,
        liveEnabled: true,
        voices: ['marin', 'cedar'],
        reasoningEfforts: ['low', 'medium', 'high'],
        defaultVoice: 'marin',
        defaultReasoningEffort: 'low',
      },
    }).as('status')
    cy.intercept('GET', '**/api/realtime/models', {
      statusCode: 200,
      body: [{ provider: 'OPENAI', id: 'gpt-realtime-2', label: 'OpenAI', defaultOption: true }],
    }).as('models')
    cy.intercept('POST', '**/api/transcribe', {
      statusCode: 200,
      body: { text: 'what does Kevin study' },
    }).as('transcribe')
    cy.intercept('POST', '**/api/realtime/lookup', {
      statusCode: 200,
      body: {
        found: true,
        snippets: [{ sourceType: 'profile', title: 'NTNU', text: 'Kevin studies data engineering at NTNU.' }],
      },
    }).as('lookup')
    cy.intercept('POST', '**/api/synthesize', {
      statusCode: 200,
      headers: { 'content-type': 'audio/mpeg' },
      body: 'FAKEAUDIO',
    }).as('synthesize')

    cy.visit('/voice', withFakeMicrophone())
    cy.wait('@status')
    cy.wait('@models')
    cy.contains('button', 'English').click()
    cy.contains('button', /start recording/i).click({ force: true })
    cy.get('[role="img"][aria-label="Recording..."]', { timeout: 15_000 }).should('be.visible')
    waitForRecordedAudioChunks()
    cy.contains('button', /stop recording/i).click({ force: true })
    cy.wait('@transcribe', { timeout: 15_000 })
    cy.wait('@lookup')
    cy.wait('@synthesize')
  })
})
