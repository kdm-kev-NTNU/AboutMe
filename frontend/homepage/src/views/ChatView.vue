<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'

type Message = { role: 'user' | 'assistant'; text: string }

const route = useRoute()
const input = ref('')
const state = reactive<{ messages: Message[] }>({ messages: [] })

async function send(text: string) {
  if (!text.trim()) return
  state.messages.push({ role: 'user', text })
  input.value = ''
  try {
    const res = await fetch('/api/ask', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question: text }),
    })
    if (!res.ok) throw new Error('Request failed')
    const data: { answer: string } = await res.json()
    state.messages.push({ role: 'assistant', text: data.answer })
  } catch (e: any) {
    state.messages.push({ role: 'assistant', text: 'Noe gikk galt. Prøv igjen.' })
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
  <div class="chat">
    <div class="messages">
      <div v-for="(m, idx) in state.messages" :key="idx" class="message" :class="m.role">
        <div class="bubble">{{ m.text }}</div>
      </div>
    </div>
    <form class="composer" @submit.prevent="send(input)">
      <input v-model="input" type="text" placeholder="Skriv et spørsmål..." />
      <button type="submit">Send</button>
    </form>
  </div>
</template>

<style scoped>
.chat {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 180px);
  max-width: 900px;
  margin: 0 auto;
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
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
</style>
