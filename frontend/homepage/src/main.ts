import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

// Pinia must be registered before the router so route guards and components can inject stores synchronously.
const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
