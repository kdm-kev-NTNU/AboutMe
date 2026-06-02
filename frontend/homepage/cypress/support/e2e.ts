// ***********************************************************
// This example support/index.js is processed and
// loaded automatically before your test files.
//
// This is a great place to put global configuration and
// behavior that modifies Cypress.
//
// You can change the location of this file or turn off
// automatically serving support files with the
// 'supportFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/configuration
// ***********************************************************

// Import commands.js using ES2015 syntax:
import './commands'

/**
 * When the production build has PostHog enabled, the cookie banner covers the chat input and footer actions.
 * Seed a dismissed "necessary only" record before load for every spec except those that assert banner behavior.
 *
 * @see frontend/homepage/src/lib/posthog-consent.ts (CONSENT_RECORD_KEY, CookieConsentRecord)
 */
const E2E_MINIMAL_CONSENT = {
  dismissed: true,
  pageviews: false,
  sessionRecording: false,
  errorTracking: false,
  featureFlags: false,
  policyVersion: '2026-06-02',
  updatedAt: new Date().toISOString(),
  source: 'banner_necessary_only',
} as const

beforeEach(() => {
  if (Cypress.spec.relative.includes('cookie-consent')) return

  cy.on('window:before:load', (win) => {
    try {
      win.localStorage.setItem('aboutme_cookie_consent_v2', JSON.stringify(E2E_MINIMAL_CONSENT))
    } catch {
      /* ignore */
    }
  })
})
