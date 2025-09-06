<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const questions = [
  'Hva kan du om meg?',
  'Hvilke prosjekter har du jobbet med?',
  'Hvilke fag og ferdigheter har du?',
  'Hva er planene dine videre?',
]

function ask(q: string) {
  router.push({ name: 'chat', query: { q } })
}
</script>

<template>
  <main class="home">
    <h1>Velg et spørsmål</h1>
    <div class="grid">
      <button v-for="q in questions" :key="q" @click="ask(q)">{{ q }}</button>
    </div>
  </main>
</template>

<style scoped>
.home {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}
button {
  padding: 14px;
  border: 1px solid #ddd;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}
button:hover {
  background: #f7f7f7;
}
</style>
