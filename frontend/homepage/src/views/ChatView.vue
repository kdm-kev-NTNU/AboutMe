<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

type Message = { role: 'user' | 'assistant'; text: string }

const route = useRoute()
const router = useRouter()
const input = ref('')
const isLoading = ref(false)
const errorText = ref('')
const state = reactive<{ messages: Message[] }>({ messages: [] })
const MAX_PROMPT_CHARS = 3000

async function send(text: string) {
  if (!text.trim() || isLoading.value) return
  // client-side validation to mirror backend
  if (text.length > MAX_PROMPT_CHARS) {
    errorText.value = `Prompten er for lang (${text.length}/${MAX_PROMPT_CHARS}).`;
    return
  }
  errorText.value = ''
  state.messages.push({ role: 'user', text })
  input.value = ''
  try {
    isLoading.value = true
    const res = await fetch('/api/ask', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question: text }),
    })
    if (!res.ok) {
      // Try parse JSON error from backend
      let msg = 'Noe gikk galt. Prøv igjen.'
      try {
        const data = await res.json() as any
        if (data && typeof data.error === 'string') {
          msg = data.error
        }
      } catch (_) {
        // ignore parse errors
      }
      if (res.status === 429) {
        msg = 'For mange forespørsler. Vent litt før du prøver igjen.'
      } else if (res.status === 400 && !msg) {
        msg = 'Ugyldig forespørsel.'
      }
      errorText.value = msg
      return
    }
    const data: { answer: string } = await res.json()
    state.messages.push({ role: 'assistant', text: data.answer })
  } catch (e: any) {
    errorText.value = 'Nettverksfeil. Prøv igjen.'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  const q = (route.query.q as string) || ''
  if (q) {
    input.value = q
    // auto-send the initial question from the homepage button
    send(q)
  }
})
</script>

<template>
  <div class="topbar">
    <button class="back" @click="router.push({ name: 'home' })">← Back</button>
  </div>
  <div class="chat">
    <div class="messages">
      <div v-if="errorText" class="error-banner">{{ errorText }}</div>
      <div v-for="(m, idx) in state.messages" :key="idx" class="message" :class="m.role">
        <div class="bubble">{{ m.text }}</div>
      </div>
      <div v-if="isLoading" class="message assistant">
        <div class="bubble typing">
          <span class="dot"></span>
          <span class="dot"></span>
          <span class="dot"></span>
        </div>
      </div>
    </div>
    <form class="composer" @submit.prevent="send(input)">
      <input v-model="input" :disabled="isLoading" type="text" placeholder="Skriv et spørsmål..." />
      <button type="submit" :disabled="isLoading">{{ isLoading ? 'Venter…' : 'Send' }}</button>
    </form>
  </div>
</template>

<style scoped>
.topbar {
  position: sticky;
  top: 0;
  background: white;
  z-index: 10;
  border-bottom: 1px solid #eee;
  padding: 10px 0;
}
.back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 0 auto;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
}
.back:hover { background: #f7f7f7; }
.chat {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 140px);
  max-width: 900px;
  margin: 0 auto;
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}
.error-banner {
  margin: 8px 0 12px;
  padding: 10px 12px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
  border-radius: 8px;
}
.message {
  display: flex;
  margin: 8px 0;
}
.message.user {
  justify-content: flex-end;
}
.message.assistant {
  justify-content: flex-start;
}
.bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 12px;
  background: #f0f0f0;
}
.message.user .bubble {
  background: #3b82f6;
  color: white;
}
.composer {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #ddd;
}
.composer input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 8px;
}
.composer button {
  padding: 10px 16px;
  border: none;
  background: #3b82f6;
  color: white;
  border-radius: 8px;
  cursor: pointer;
}
.composer button:hover {
  background: #2563eb;
}
.typing {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.dot {
  width: 6px;
  height: 6px;
  background: #9ca3af;
  border-radius: 50%;
  animation: blink 1.2s infinite ease-in-out;
}
.dot:nth-child(2) { animation-delay: .2s; }
.dot:nth-child(3) { animation-delay: .4s; }

@keyframes blink {
  0%, 80%, 100% { transform: scale(0.8); opacity: .4; }
  40% { transform: scale(1); opacity: 1; }
}
</style>
