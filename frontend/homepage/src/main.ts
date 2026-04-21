import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { MotionPlugin } from '@vueuse/motion'
import posthog from 'posthog-js'

import App from './App.vue'
import router from './router'

// Pinia must be registered before the router so route guards and components can inject stores synchronously.
const app = createApp(App)

posthog.init(import.meta.env.VITE_POSTHOG_KEY, {
  api_host: import.meta.env.VITE_POSTHOG_HOST || 'https://us.i.posthog.com',
  defaults: '2026-01-30',
  opt_out_capturing_by_default: true,
  persistence: 'localStorage+cookie',
})

app.use(createPinia())
app.use(router)
app.use(MotionPlugin)

app.config.errorHandler = (err) => {
  posthog.captureException(err)
}

app.mount('#app')
