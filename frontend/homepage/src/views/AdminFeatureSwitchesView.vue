<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useVoiceAvailabilityStore } from '@/stores/voice-availability'
import { customFetch } from '@/api/orval-mutator'
import { Button } from '@/components/ui/button'
import { formatAdminHttpError } from '@/lib/api-error'

type VoiceFeatureStatus = {
  killSwitchEngaged: boolean
  killSwitchState: string
  liveEnabled: boolean
  capability: boolean
  source: string
  lastSyncedAt: string | null
  posthogConfigured: boolean
  posthogReadConfigured: boolean
  flagKey: string
  posthogUrl: string
}

const auth = useAuthStore()
const voiceAvailability = useVoiceAvailabilityStore()

const status = ref<VoiceFeatureStatus | null>(null)
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const confirmEngage = ref(false)

const engaged = computed(() => status.value?.killSwitchEngaged === true)

async function loadStatus() {
  loading.value = true
  error.value = ''
  try {
    const r = await customFetch<{ data: VoiceFeatureStatus; status: number }>(
      '/admin/tools/features/voice',
      { method: 'GET' },
    )
    if (r.status < 200 || r.status >= 300) {
      error.value = formatAdminHttpError(r.status, r.data)
      return
    }
    status.value = r.data
  } catch {
    error.value = 'Kunne ikke hente voice-status.'
  } finally {
    loading.value = false
  }
}

async function setEngaged(next: boolean) {
  busy.value = true
  error.value = ''
  confirmEngage.value = false
  try {
    const r = await customFetch<{ data: VoiceFeatureStatus; status: number }>(
      '/admin/tools/features/voice/kill-switch',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ engaged: next }),
      },
    )
    if (r.status < 200 || r.status >= 300) {
      error.value = formatAdminHttpError(r.status, r.data)
      return
    }
    status.value = r.data
    await voiceAvailability.refresh()
  } catch {
    error.value = 'Kunne ikke oppdatere PostHog-flagget.'
  } finally {
    busy.value = false
  }
}

onMounted(async () => {
  auth.restore()
  await loadStatus()
})
</script>

<template>
  <div class="min-h-screen bg-[hsl(220_20%_97%)] text-[hsl(220_25%_10%)] font-sans antialiased pb-12">
    <nav
      class="border-b border-gray-200/80 bg-white/90 backdrop-blur-sm px-4 py-3 text-sm flex flex-wrap gap-x-4 gap-y-1 items-center max-w-3xl mx-auto"
    >
      <RouterLink to="/" class="text-blue-600 hover:underline">Hjem</RouterLink>
      <span class="text-gray-500">/</span>
      <RouterLink to="/admin/tools" class="text-blue-600 hover:underline">Internal tools</RouterLink>
      <span class="text-gray-500">/</span>
      <strong class="text-gray-900">Feature switches</strong>
    </nav>

    <main id="main-content" class="mx-auto max-w-3xl px-4 pt-8">
      <h1 class="text-2xl font-semibold tracking-tight text-gray-900 mb-2">Feature switches</h1>
      <p class="text-sm text-gray-600 mb-8 leading-relaxed">
        Skru av offentlig live voice via PostHog-flagget
        <code class="font-mono text-xs bg-gray-100 px-1 rounded">aboutme_voice_kill_switch</code>
        uten redeploy. Admin-intervjuet påvirkes ikke.
      </p>

      <p v-if="error" class="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800" role="alert">
        {{ error }}
      </p>

      <section
        class="rounded-xl border border-gray-200 bg-white p-5 shadow-[0_1px_3px_rgb(0_0_0/0.06)]"
        data-testid="voice-kill-switch-panel"
      >
        <div class="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h2 class="text-base font-semibold text-gray-900">Offentlig live voice</h2>
            <p class="mt-1 text-sm text-gray-600">
              Når kill switch er på, skjules voice på forsiden, chat og
              <code class="font-mono text-xs">/voice</code>, og
              <code class="font-mono text-xs">POST /realtime/session</code> avvises.
            </p>
          </div>
          <Button type="button" variant="outline" size="sm" :disabled="loading || busy" @click="loadStatus">
            Oppdater
          </Button>
        </div>

        <p v-if="loading && !status" class="mt-4 text-sm text-gray-500">Henter status…</p>

        <template v-else-if="status">
          <dl class="mt-4 grid gap-2 text-sm sm:grid-cols-2">
            <div>
              <dt class="text-gray-500">Kill switch</dt>
              <dd class="font-medium" :class="engaged ? 'text-amber-800' : 'text-green-700'">
                {{ engaged ? 'På (voice av for besøkende)' : 'Av (voice kan tilbys)' }}
              </dd>
            </div>
            <div>
              <dt class="text-gray-500">Offentlig liveEnabled</dt>
              <dd class="font-medium">{{ status.liveEnabled ? 'true' : 'false' }}</dd>
            </div>
            <div>
              <dt class="text-gray-500">Realtime-kapabilitet</dt>
              <dd class="font-medium">{{ status.capability ? 'true' : 'false' }}</dd>
            </div>
            <div>
              <dt class="text-gray-500">Kilde</dt>
              <dd class="font-mono text-xs">{{ status.source }} / {{ status.killSwitchState }}</dd>
            </div>
            <div>
              <dt class="text-gray-500">Sist synket</dt>
              <dd class="font-mono text-xs">{{ status.lastSyncedAt ?? '—' }}</dd>
            </div>
            <div>
              <dt class="text-gray-500">Flagg</dt>
              <dd>
                <a
                  v-if="status.posthogUrl"
                  :href="status.posthogUrl"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="text-blue-600 hover:underline font-mono text-xs"
                >
                  {{ status.flagKey }}
                </a>
                <span v-else class="font-mono text-xs">{{ status.flagKey }}</span>
              </dd>
            </div>
          </dl>

          <p
            v-if="!status.posthogConfigured"
            class="mt-4 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900"
            role="status"
          >
            PostHog management er ikke konfigurert. Sett
            <code class="font-mono text-xs">POSTHOG_PERSONAL_API_KEY</code>,
            <code class="font-mono text-xs">POSTHOG_PROJECT_ID</code> og
            <code class="font-mono text-xs">POSTHOG_API_HOST</code> på backend-tjenesten.
          </p>

          <div class="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center">
            <template v-if="!engaged">
              <Button
                v-if="!confirmEngage"
                type="button"
                variant="destructive"
                :disabled="busy || !status.posthogConfigured"
                data-testid="voice-kill-switch-engage"
                @click="confirmEngage = true"
              >
                Skru av offentlig voice
              </Button>
              <template v-else>
                <Button
                  type="button"
                  variant="destructive"
                  :disabled="busy"
                  data-testid="voice-kill-switch-confirm"
                  @click="setEngaged(true)"
                >
                  Bekreft: skru av nå
                </Button>
                <Button type="button" variant="outline" :disabled="busy" @click="confirmEngage = false">
                  Avbryt
                </Button>
              </template>
            </template>
            <Button
              v-else
              type="button"
              :disabled="busy || !status.posthogConfigured"
              data-testid="voice-kill-switch-release"
              @click="setEngaged(false)"
            >
              Skru på offentlig voice
            </Button>
          </div>
        </template>
      </section>
    </main>
  </div>
</template>
