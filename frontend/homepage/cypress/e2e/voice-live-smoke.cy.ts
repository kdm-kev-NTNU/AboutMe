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
      cy.contains('button', 'Start live voice', { timeout: 15_000 }).should('be.visible')
      cy.contains('Live voice is not enabled on the server right now.').should('not.exist')
    } else {
      cy.contains('Live voice is not enabled on the server right now.', { timeout: 15_000 }).should(
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

    cy.contains('button', 'Start live voice', { timeout: 15_000 }).click()

    cy.wait('@realtimeSession', { timeout: 30_000 }).then((interception) => {
      expect(interception.request.headers).to.have.property('x-chat-language', 'en')
      expect(interception.request.headers['content-type']).to.match(/^application\/sdp/i)
      expect(interception.response?.statusCode).to.be.within(200, 299)
      expect(interception.response?.headers['content-type']).to.match(/application\/sdp/i)
      expect(interception.response?.body).to.be.a('string').and.include('v=0')
    })
  })
})
