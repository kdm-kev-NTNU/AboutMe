import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { MotionPlugin } from '@vueuse/motion'
import { captureClientException } from './lib/analytics'
import { applyPendingOwnerIdentity } from './lib/analytics-identity'
import {
  applyStoredTrackingConsent,
  isPosthogEnabled,
  registerPosthogActivationHandler,
} from './lib/posthog-consent'
import { setupPosthogAppHooks } from './lib/posthog-app-hooks'

import App from './App.vue'
import router from './router'
import { setupDocumentLang } from './composables/useDocumentLang'

// Pinia must be registered before the router so route guards and components can inject stores synchronously.
const app = createApp(App)

const posthogEnabled = isPosthogEnabled()

app.use(createPinia())
setupDocumentLang()
app.use(router)
app.use(MotionPlugin)

if (posthogEnabled) {
  registerPosthogActivationHandler(() => {
    setupPosthogAppHooks(app, router)
    applyPendingOwnerIdentity()
  })
  applyStoredTrackingConsent()
} else {
  console.info('[analytics] PostHog disabled: set VITE_POSTHOG_ENABLED=true and VITE_POSTHOG_KEY to enable.')
}

app.config.errorHandler = (err) => {
  captureClientException(err)
}

app.mount('#app')
