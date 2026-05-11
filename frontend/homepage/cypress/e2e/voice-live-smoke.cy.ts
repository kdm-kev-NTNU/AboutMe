/**
 * Live smoke coverage for the deployed voice feature.
 *
 * This spec is intentionally skipped unless CYPRESS_VOICE_LIVE=true is set.
 * It is meant for manual/post-deploy verification, not the normal PR E2E gate.
 */

function envFlag(name: string, defaultValue = false): boolean {
  const value = Cypress.env(name)
  if (value === undefined || value === null || value === '') return defaultValue
  if (typeof value === 'boolean') return value
  return ['1', 'true', 'yes', 'on'].includes(String(value).toLowerCase())
}

function withSyntheticMicrophone() {
  return {
    onBeforeLoad(win: Cypress.AUTWindow) {
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

describe('Voice live smoke', () => {
  const voiceLive = envFlag('VOICE_LIVE')
  const expectRealtimeEnabled = envFlag('VOICE_LIVE_EXPECT_REALTIME_ENABLED', true)
  const runOpenAiSession = envFlag('VOICE_LIVE_RUN_OPENAI')
  const runElevenLabsToken = envFlag('VOICE_LIVE_RUN_ELEVENLABS')
  const runElevenLabsSession = envFlag('VOICE_LIVE_RUN_ELEVENLABS_SESSION')
  const elevenLabsAgentId = (Cypress.env('VOICE_LIVE_ELEVENLABS_AGENT_ID') as string | undefined) ?? ''

  beforeEach(function () {
    if (!voiceLive) {
      this.skip()
    }
  })

  it('verifies the live realtime status contract', () => {
    cy.request({
      method: 'GET',
      url: '/api/realtime/status',
      failOnStatusCode: false,
    }).then((response) => {
      expect(response.status).to.eq(200)
      expect(response.headers['content-type']).to.match(/application\/json/i)
      expect(response.body).to.have.property('enabled', expectRealtimeEnabled)
    })
  })

  it('renders the live voice entry point according to backend availability', () => {
    cy.intercept('GET', '**/api/realtime/status').as('realtimeStatus')

    cy.visit('/voice', {
      onBeforeLoad(win) {
        win.localStorage.setItem('lang', 'en')
      },
    })

    cy.wait('@realtimeStatus').its('response.statusCode').should('eq', 200)

    if (expectRealtimeEnabled) {
      cy.contains('button', 'Start voice', { timeout: 15_000 }).should('be.visible')
      cy.contains('Voice chat is not enabled on the server right now.').should('not.exist')
    } else {
      cy.contains('Voice chat is not enabled on the server right now.', { timeout: 15_000 }).should(
        'be.visible',
      )
    }
  })

  it('can create a real OpenAI Realtime SDP session when explicitly enabled', function () {
    if (!runOpenAiSession) {
      this.skip()
    }

    cy.intercept('GET', '**/api/realtime/status').as('realtimeStatus')
    cy.intercept('POST', '**/api/realtime/session').as('realtimeSession')

    cy.visit('/voice', withSyntheticMicrophone())
    cy.wait('@realtimeStatus').its('response.body').should('deep.include', { enabled: true })

    cy.contains('button', 'Start voice', { timeout: 15_000 }).click()

    cy.wait('@realtimeSession', { timeout: 30_000 }).then((interception) => {
      expect(interception.request.headers).to.have.property('x-chat-language', 'en')
      expect(interception.request.headers['content-type']).to.match(/^application\/sdp/i)
      expect(interception.response?.statusCode).to.be.within(200, 299)
      expect(interception.response?.headers['content-type']).to.match(/application\/sdp/i)
      expect(interception.response?.body).to.be.a('string').and.include('v=0')
    })
  })

  it('mints a real ElevenLabs conversation token when explicitly enabled', function () {
    if (!runElevenLabsToken) {
      this.skip()
    }

    cy.request({
      method: 'GET',
      url: '/api/realtime/models',
      failOnStatusCode: false,
    }).then((modelsResponse) => {
      expect(modelsResponse.status, 'realtime models endpoint').to.eq(200)
      const elevenLabsModels = (modelsResponse.body as Array<{ id: string; provider: string }>).filter(
        (m) => m.provider === 'ELEVENLABS',
      )
      expect(elevenLabsModels, 'at least one ELEVENLABS model is exposed').to.have.length.greaterThan(0)

      const targetAgentId = elevenLabsAgentId || elevenLabsModels[0].id

      cy.request({
        method: 'POST',
        url: '/api/realtime/elevenlabs/token',
        headers: { 'Content-Type': 'application/json' },
        body: { modelId: targetAgentId },
        failOnStatusCode: false,
      }).then((tokenResponse) => {
        expect(tokenResponse.status, 'elevenlabs token mint').to.eq(200)
        expect(tokenResponse.headers['content-type']).to.match(/application\/json/i)
        const body = tokenResponse.body as { token?: string }
        expect(body).to.have.property('token').that.is.a('string')
        expect((body.token ?? '').length, 'JWT-shaped token length').to.be.greaterThan(40)
        expect(body.token, 'three-segment JWT').to.match(/^[\w-]+\.[\w-]+\.[\w-]+$/)
      })
    })
  })

  it('starts a real ElevenLabs browser session when explicitly enabled', function () {
    if (!runElevenLabsSession) {
      this.skip()
    }

    cy.request('/api/realtime/models').then((modelsResponse) => {
      expect(modelsResponse.status, 'realtime models endpoint').to.eq(200)
      const elevenLabsModels = (modelsResponse.body as Array<{ id: string; provider: string }>).filter(
        (m) => m.provider === 'ELEVENLABS',
      )
      expect(elevenLabsModels, 'at least one ELEVENLABS model is exposed').to.have.length.greaterThan(0)

      const targetAgentId = elevenLabsAgentId || elevenLabsModels[0].id

      cy.intercept('GET', '**/api/realtime/status').as('realtimeStatus')
      cy.intercept('POST', '**/api/realtime/elevenlabs/token').as('elevenLabsToken')

      cy.visit('/voice', withSyntheticMicrophone())
      cy.wait('@realtimeStatus').its('response.body').should('deep.include', { enabled: true })

      cy.get('[data-testid="voice-model-select"]', { timeout: 15_000 })
        .should('be.visible')
        .select(targetAgentId)

      cy.contains('button', 'Start voice', { timeout: 15_000 }).click()

      cy.wait('@elevenLabsToken', { timeout: 30_000 }).then((interception) => {
        expect(interception.request.body).to.deep.equal({ modelId: targetAgentId })
        expect(interception.response?.statusCode).to.eq(200)
      })

      cy.contains('button', 'End session', { timeout: 30_000 }).should('be.visible')
      cy.contains('Voice could not start').should('not.exist')
      cy.contains('Voice agent disconnected').should('not.exist')

      cy.contains('button', 'End session').click()
      cy.contains('button', 'Start voice', { timeout: 15_000 }).should('be.visible')
    })
  })

  it('renders the ElevenLabs option in the model selector when realtime is enabled', function () {
    if (!expectRealtimeEnabled) {
      this.skip()
    }

    cy.intercept('GET', '**/api/realtime/status').as('realtimeStatus')
    cy.visit('/voice', {
      onBeforeLoad(win) {
        win.localStorage.setItem('lang', 'en')
      },
    })
    cy.wait('@realtimeStatus').its('response.statusCode').should('eq', 200)

    cy.request('/api/realtime/models').then((modelsResponse) => {
      const hasElevenLabs = (modelsResponse.body as Array<{ provider: string }>).some(
        (m) => m.provider === 'ELEVENLABS',
      )
      if (hasElevenLabs) {
        cy.get('[data-testid="voice-model-select"]', { timeout: 15_000 })
          .should('be.visible')
          .find('option')
          .then((options) => {
            const ids = Array.from(options).map((o) => (o as HTMLOptionElement).value)
            const expectedId = elevenLabsAgentId || ((modelsResponse.body as Array<{ id: string; provider: string }>)
              .find((m) => m.provider === 'ELEVENLABS')?.id ?? '')
            expect(ids, 'ELEVENLABS agent id is selectable').to.include(expectedId)
          })
      }
    })
  })
})
