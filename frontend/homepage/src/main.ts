import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { MotionPlugin } from '@vueuse/motion'
import { captureClientException, initPosthogIfConfigured } from './lib/analytics'

import App from './App.vue'
import router from './router'

// Pinia must be registered before the router so route guards and components can inject stores synchronously.
const app = createApp(App)

const isPosthogEnabled = initPosthogIfConfigured(
  import.meta.env.VITE_POSTHOG_KEY,
  import.meta.env.VITE_POSTHOG_HOST,
)
if (!isPosthogEnabled) {
  console.warn('[analytics] PostHog disabled: VITE_POSTHOG_KEY is missing or empty.')
}

app.use(createPinia())
app.use(router)
app.use(MotionPlugin)

app.config.errorHandler = (err) => {
  if (isPosthogEnabled) {
    captureClientException(err)
  }
}

app.mount('#app')
