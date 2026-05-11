function base64UrlEncode(value: string): string {
  return btoa(value).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '')
}

function fakeJwt(payload: Record<string, unknown>): string {
  return [
    base64UrlEncode(JSON.stringify({ alg: 'HS256', typ: 'JWT' })),
    base64UrlEncode(JSON.stringify(payload)),
    'signature',
  ].join('.')
}

function fakeElevenLabsToken(): string {
  return fakeJwt({
    iss: 'APITestIssuer',
    sub: 'user_agent_test',
    name: 'user_agent_test_conv',
    exp: Math.floor(Date.now() / 1000) + 900,
    video: {
      room: 'room_agent_test',
    },
  })
}

type StubMode = 'connect' | 'disconnectThenConnect' | 'rejectLiveKitValidate404'

function installSyntheticMicrophoneAndElevenLabsStub(win: Cypress.AUTWindow, mode: StubMode) {
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

  let sessionCalls = 0
  ;(win as typeof win & {
    __ABOUTME_E2E_ELEVENLABS_CLIENT__?: {
      startSession: (options: {
        conversationToken: string
        connectionType: 'webrtc'
        onConnect?: () => void
        onDisconnect?: (details: {
          reason?: string
          closeCode?: number
          closeReason?: string
          message?: string
          context?: Event
        }) => void
        onError?: (message: string) => void
      }) => Promise<{ endSession: () => Promise<void>; getId: () => string }>
    }
    __ABOUTME_E2E_END_SESSION_CALLS__?: number
  }).__ABOUTME_E2E_ELEVENLABS_CLIENT__ = {
    startSession: async (options) => {
      sessionCalls += 1

      if (mode === 'rejectLiveKitValidate404') {
        const error = Object.assign(
          new win.Error('ServiceNotFound: rtc/v1/validate returned 404'),
          {
            name: 'SessionConnectionError',
            closeCode: 4001,
            closeReason: 'agent branch mismatch',
          },
        )
        throw error
      }

      queueMicrotask(() => {
        options.onConnect?.()
      })

      if (mode === 'disconnectThenConnect' && sessionCalls === 1) {
        win.setTimeout(() => {
          options.onDisconnect?.({
            reason: 'agent',
            closeReason: 'agent disconnected',
            context: new win.Event('close'),
          })
        }, 25)
      }

      return {
        endSession: async () => {
          const current = (win as typeof win & { __ABOUTME_E2E_END_SESSION_CALLS__?: number })
            .__ABOUTME_E2E_END_SESSION_CALLS__ ?? 0
          ;(win as typeof win & { __ABOUTME_E2E_END_SESSION_CALLS__?: number })
            .__ABOUTME_E2E_END_SESSION_CALLS__ = current + 1
        },
        getId: () => `conv_stub_${sessionCalls}`,
      }
    },
  }
}

function stubVoiceEndpoints() {
  cy.intercept('GET', '**/api/realtime/status', {
    statusCode: 200,
    body: {
      enabled: true,
      voices: ['marin', 'cedar'],
      reasoningEfforts: ['low', 'medium', 'high'],
      defaultVoice: 'marin',
      defaultReasoningEffort: 'low',
    },
  }).as('realtimeStatus')

  cy.intercept('GET', '**/api/realtime/models', {
    statusCode: 200,
    body: [
      {
        provider: 'OPENAI',
        id: 'gpt-realtime-2',
        label: 'OpenAI GPT-Realtime-2',
        defaultOption: false,
      },
      {
        provider: 'ELEVENLABS',
        id: 'agent_test',
        label: "Kevin's AI",
        defaultOption: true,
      },
    ],
  }).as('realtimeModels')
}

function visitVoiceWithStub(mode: StubMode) {
  stubVoiceEndpoints()
  cy.intercept('POST', '**/api/realtime/elevenlabs/token', {
    statusCode: 200,
    body: { token: fakeElevenLabsToken() },
  }).as('elevenLabsToken')

  cy.visit('/voice', {
    onBeforeLoad(win) {
      installSyntheticMicrophoneAndElevenLabsStub(win, mode)
    },
  })

  cy.wait('@realtimeStatus')
  cy.wait('@realtimeModels')
}

function selectElevenLabsModel() {
  cy.get('[data-testid="voice-model-select"]', { timeout: 15_000 })
    .should('be.visible')
    .select('agent_test')
    .should('have.value', 'agent_test')
}

describe('Voice ElevenLabs E2E', () => {
  it('starts and ends a deterministic ElevenLabs browser session', () => {
    visitVoiceWithStub('connect')
    selectElevenLabsModel()

    cy.contains('button', 'Start voice', { timeout: 15_000 }).click()

    cy.wait('@elevenLabsToken').then((interception) => {
      expect(interception.request.body).to.deep.equal({ modelId: 'agent_test' })
    })

    cy.contains('button', 'End session', { timeout: 15_000 }).should('be.visible')
    cy.contains('Voice agent disconnected').should('not.exist')

    cy.contains('button', 'End session').click()
    cy.contains('button', 'Start voice', { timeout: 15_000 }).should('be.visible')
    cy.window().its('__ABOUTME_E2E_END_SESSION_CALLS__').should('eq', 1)
  })

  it('surfaces an agent disconnect dialog and lets the user retry into a fresh session', () => {
    visitVoiceWithStub('disconnectThenConnect')
    selectElevenLabsModel()

    cy.contains('button', 'Start voice', { timeout: 15_000 }).click()
    cy.wait('@elevenLabsToken')

    cy.contains('Voice agent disconnected', { timeout: 15_000 }).should('be.visible')
    cy.contains('The live voice session ended unexpectedly. Start a new session and try again.').should(
      'be.visible',
    )
    cy.contains('The voice agent could not start. Please try again later.').should('be.visible')

    cy.contains('button', 'Try again').click()
    cy.wait('@elevenLabsToken')
    cy.contains('button', 'End session', { timeout: 15_000 }).should('be.visible')
  })

  it('shows a targeted message when ElevenLabs startSession fails with the LiveKit validate 404 signature', () => {
    visitVoiceWithStub('rejectLiveKitValidate404')
    selectElevenLabsModel()

    cy.contains('button', 'Start voice', { timeout: 15_000 }).click()
    cy.wait('@elevenLabsToken')

    cy.contains('Voice could not start', { timeout: 15_000 }).should('be.visible')
    cy.contains('The live AI service needs a fresh session before it can continue.').should('be.visible')
    cy.contains('Voice server connection failed. The agent may be misconfigured or temporarily unavailable.')
      .should('be.visible')
    cy.contains('button', 'Start voice', { timeout: 15_000 }).should('be.visible')
  })

  it('shows the backend token failure without attempting an ElevenLabs browser session', () => {
    stubVoiceEndpoints()
    cy.intercept('POST', '**/api/realtime/elevenlabs/token', {
      statusCode: 502,
      body: {
        error: 'ElevenLabs rejected the session: branch_id is invalid',
        code: 'ELEVENLABS_REJECTED',
      },
    }).as('elevenLabsToken')

    cy.visit('/voice', {
      onBeforeLoad(win) {
        installSyntheticMicrophoneAndElevenLabsStub(win, 'connect')
      },
    })

    cy.wait('@realtimeStatus')
    cy.wait('@realtimeModels')
    selectElevenLabsModel()
    cy.contains('button', 'Start voice', { timeout: 15_000 }).click()
    cy.wait('@elevenLabsToken')

    cy.contains('Voice could not start', { timeout: 15_000 }).should('be.visible')
    cy.contains('The live AI service needs a fresh session before it can continue.').should('be.visible')
    cy.contains('ElevenLabs could not start the session: branch_id is invalid').should('be.visible')
    cy.window().then((win) => {
      expect((win as typeof win & { __ABOUTME_E2E_END_SESSION_CALLS__?: number }).__ABOUTME_E2E_END_SESSION_CALLS__)
        .to.be.undefined
    })
  })
})
