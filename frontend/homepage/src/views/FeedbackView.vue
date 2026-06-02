<script setup lang="ts">
import { ref, computed } from 'vue'
import { useLangStore } from '../stores/lang'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { MessageSquare, Send, CheckCircle, Info } from 'lucide-vue-next'
import { submitFeedback } from '@/api/generated/portfolio'

const langStore = useLangStore()

const t = computed(() => {
  if (langStore.language === 'no') {
    return {
      pageTitle: 'Tilbakemeldinger',
      intro: 'Har du tanker om nettsiden, chat-funksjonen eller noe annet? Alle tilbakemeldinger hjelper meg å forbedre opplevelsen.',
      messageLabel: 'Din tilbakemelding',
      messagePlaceholder: 'Skriv tilbakemeldingen din her ...',
      submit: 'Send tilbakemelding',
      sending: 'Sender ...',
      successTitle: 'Takk!',
      successBody: 'Tilbakemeldingen din er mottatt. Den hjelper meg å gjøre siden bedre.',
      errorGeneric: 'Noe gikk galt. Prøv igjen senere.',
      errorRateLimit: 'For mange innsendinger. Vent litt før du prøver igjen.',
      privacyNote: 'Tilbakemeldingen lagres kun for å forbedre nettsiden.',
    }
  }
  return {
    pageTitle: 'Feedback',
    intro: 'Have thoughts on the site, the chat feature, or anything else? Every piece of feedback helps me improve the website.',
    messageLabel: 'Your feedback',
    messagePlaceholder: 'Write your feedback here ...',
    submit: 'Send feedback',
    sending: 'Sending ...',
    successTitle: 'Thank you!',
    successBody: 'Your feedback has been received. It helps me make the site better.',
    errorGeneric: 'Something went wrong. Please try again later.',
    errorRateLimit: 'Too many submissions. Please wait a moment before trying again.',
    privacyNote: 'Feedback is stored solely to improve the site.',
  }
})

const message = ref('')
const loading = ref(false)
const success = ref(false)
const error = ref('')

async function handleSubmit() {
  error.value = ''
  if (!message.value.trim()) return

  loading.value = true
  try {
    const res = await submitFeedback({ message: message.value.trim() })
    if (res.status === 204) {
      success.value = true
      message.value = ''
    } else if (res.status === 429) {
      error.value = t.value.errorRateLimit
    } else {
      error.value = (res.data as { error?: string })?.error || t.value.errorGeneric
    }
  } catch {
    error.value = t.value.errorGeneric
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main id="main-content" class="min-h-screen pt-20 bg-gradient-to-br from-slate-50 to-slate-100 relative">
    <div class="absolute inset-0 pointer-events-none">
      <div class="absolute top-0 left-0 w-full h-full" style="background: radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.08) 0%, transparent 50%), radial-gradient(circle at 80% 20%, rgba(37, 99, 235, 0.08) 0%, transparent 50%), radial-gradient(circle at 50% 50%, rgba(96, 165, 250, 0.05) 0%, transparent 70%);"></div>
    </div>

    <div class="relative z-10 mx-auto max-w-xl px-4 py-8 sm:px-6 lg:px-8">
      <h1 class="text-3xl font-bold mb-4 text-center bg-gradient-to-r from-blue-600 via-blue-700 to-blue-800 bg-clip-text text-transparent">
        {{ t.pageTitle }}
      </h1>
      <p class="text-center text-gray-600 mb-10 text-sm leading-relaxed max-w-md mx-auto">
        {{ t.intro }}
      </p>

      <Alert v-if="success" class="mb-8 border-green-200 bg-green-50">
        <CheckCircle class="size-4 text-green-600" />
        <AlertTitle class="text-green-800">{{ t.successTitle }}</AlertTitle>
        <AlertDescription class="text-green-700">{{ t.successBody }}</AlertDescription>
      </Alert>

      <Card v-if="!success" class="border-2 border-transparent bg-white/90 backdrop-blur-sm transition-all duration-300 hover:border-blue-300/30 hover:bg-white/95 hover:shadow-xl hover:shadow-blue-500/15">
        <CardHeader>
          <CardTitle class="flex items-center gap-2 text-lg">
            <MessageSquare class="size-5 text-blue-600" />
            {{ t.messageLabel }}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form class="space-y-5" @submit.prevent="handleSubmit">
            <div>
              <label for="feedback-message" class="sr-only">{{ t.messageLabel }}</label>
              <textarea
                id="feedback-message"
                v-model="message"
                rows="5"
                :placeholder="t.messagePlaceholder"
                class="w-full rounded-lg border-2 border-blue-200/20 bg-white/80 px-4 py-3 text-sm transition-all duration-300 focus:border-blue-300/50 focus:bg-white/95 focus:shadow-sm focus:shadow-blue-500/10 focus:outline-none placeholder:text-blue-600/40 resize-y"
                required
              ></textarea>
            </div>

            <Alert v-if="error" class="border-red-200 bg-red-50">
              <AlertDescription class="text-red-700 text-sm">{{ error }}</AlertDescription>
            </Alert>

            <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <p class="text-xs text-gray-400 flex items-center gap-1 max-w-xs">
                <Info class="size-3 shrink-0" />
                {{ t.privacyNote }}
              </p>
              <Button
                type="submit"
                :disabled="loading || !message.trim()"
                class="cursor-pointer bg-gradient-to-r from-blue-600 to-blue-700 hover:from-blue-700 hover:to-blue-800 text-white font-semibold hover:-translate-y-0.5 hover:shadow-lg hover:shadow-blue-500/40 transition-all duration-300 relative overflow-hidden shrink-0"
              >
                <Send v-if="!loading" class="size-4 mr-1.5" />
                {{ loading ? t.sending : t.submit }}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  </main>
</template>
