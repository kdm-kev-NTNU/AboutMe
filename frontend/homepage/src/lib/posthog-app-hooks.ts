import type { App } from 'vue'
import type { Router } from 'vue-router'
import posthog from 'posthog-js'
import { hasPageviewConsent } from './posthog-consent'

let hooksRegistered = false

export function setupPosthogAppHooks(app: App, router: Router): void {
  if (hooksRegistered) return
  hooksRegistered = true

  router.afterEach((to) => {
    if (!hasPageviewConsent()) return
    posthog.capture('$pageview', {
      path: to.path,
      routeName: typeof to.name === 'string' ? to.name : null,
      query: to.query,
    })
  })

  app.config.globalProperties.$posthog = posthog
}
