<script setup lang="ts">
import { COPY, DATA_ATTRIBUTES } from './film-script'

function daAttr(name: keyof typeof DATA_ATTRIBUTES): Record<string, string> {
  return { [DATA_ATTRIBUTES[name]]: '' }
}

const cardIconColors: Record<string, string> = {
  blue: 'bg-blue-100 text-blue-600',
  yellow: 'bg-amber-100 text-amber-600',
  green: 'bg-emerald-100 text-emerald-600',
}

const badgeColors: Record<string, string> = {
  blue: 'bg-blue-100 text-blue-700',
  yellow: 'bg-amber-100 text-amber-700',
  green: 'bg-emerald-100 text-emerald-700',
}

const statusColors: Record<string, string> = {
  red: 'bg-red-100 text-red-700 border-red-200',
  blue: 'bg-blue-100 text-blue-700 border-blue-200',
}
</script>

<template>
  <div class="relative h-full">
    <!-- Panel 1: Landing -->
    <section
      v-bind="daAttr('landingPanel')"
      class="absolute inset-0 overflow-y-auto"
    >
      <div
        v-bind="daAttr('heroSection')"
        class="relative bg-blue-900 px-8 pb-20 pt-10 text-white"
      >
        <div
          class="absolute -bottom-1 left-0 right-0 h-12 bg-white"
          style="clip-path: ellipse(55% 100% at 50% 100%)"
          aria-hidden="true"
        />
        <div class="mx-auto max-w-6xl text-left">
          <h1
            v-bind="daAttr('headline')"
            class="max-w-3xl text-4xl font-bold leading-tight"
          >
            {{ COPY.landing.headline }}
          </h1>
          <p
            v-bind="daAttr('subcopy')"
            class="mt-5 max-w-2xl text-base leading-relaxed text-blue-100"
          >
            {{ COPY.landing.subcopy }}
          </p>
          <div class="mt-8 flex items-center justify-start gap-4">
            <button
              v-bind="daAttr('komIGang')"
              type="button"
              class="rounded-lg bg-white px-6 py-3 text-sm font-semibold text-blue-700 shadow-md"
            >
              {{ COPY.landing.ctaPrimary }}
            </button>
            <button
              v-bind="daAttr('seKart')"
              type="button"
              class="rounded-lg border-2 border-white px-6 py-3 text-sm font-semibold text-white"
            >
              {{ COPY.landing.ctaSecondary }}
            </button>
          </div>
        </div>
      </div>

      <div v-bind="daAttr('infoSection')" class="px-8 pb-10 pt-14">
        <h2 class="text-center text-2xl font-bold text-slate-900">
          {{ COPY.landing.infoTitle }}
        </h2>
        <p class="mx-auto mt-3 max-w-2xl text-center text-sm text-slate-500">
          {{ COPY.landing.infoSubcopy }}
        </p>
        <div
          v-bind="daAttr('infoCards')"
          class="mx-auto mt-8 grid max-w-5xl grid-cols-3 gap-5"
        >
          <div
            v-for="card in COPY.landing.cards"
            :key="card.title"
            class="relative rounded-xl border border-slate-100 bg-white p-6 shadow-md"
          >
            <span
              class="absolute right-4 top-4 rounded-full px-2.5 py-0.5 text-xs font-semibold"
              :class="badgeColors[card.color]"
            >
              {{ card.badge }}
            </span>
            <div
              class="mb-4 flex size-12 items-center justify-center rounded-full"
              :class="cardIconColors[card.color]"
            >
              <svg
                v-if="card.icon === 'plus'"
                class="size-6"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                aria-hidden="true"
              >
                <path d="M12 5v14M5 12h14" />
              </svg>
              <svg
                v-else-if="card.icon === 'warning'"
                class="size-6"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                aria-hidden="true"
              >
                <path d="M12 9v4M12 17h.01" />
                <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
              </svg>
              <svg
                v-else
                class="size-6"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                aria-hidden="true"
              >
                <path d="M20 6 9 17l-5-5" />
              </svg>
            </div>
            <h3 class="text-lg font-bold text-slate-900">{{ card.title }}</h3>
          </div>
        </div>
      </div>
    </section>

    <!-- Panel 2: Registration -->
    <section
      v-bind="daAttr('registrationPanel')"
      class="demo-hidden absolute inset-0 flex items-start justify-center overflow-y-auto bg-slate-50 px-8 pt-10"
    >
      <div class="w-full max-w-md rounded-2xl border border-slate-100 bg-white p-8 shadow-xl">
        <h2 class="mb-6 text-center text-2xl font-bold text-slate-900">
          {{ COPY.registration.title }}
        </h2>

        <div class="space-y-4">
          <div>
            <label class="mb-1 block text-sm font-medium text-slate-700">Fornavn</label>
            <div class="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5">
              <svg class="size-4 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
              <span class="text-sm text-slate-800">{{ COPY.registration.firstName }}</span>
            </div>
          </div>
          <div>
            <label class="mb-1 block text-sm font-medium text-slate-700">Etternavn</label>
            <div class="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5">
              <svg class="size-4 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
              <span class="text-sm text-slate-800">{{ COPY.registration.lastName }}</span>
            </div>
          </div>
          <div>
            <label class="mb-1 block text-sm font-medium text-slate-700">E-post</label>
            <div class="flex items-center gap-2 rounded-lg border border-blue-200 bg-blue-50 px-3 py-2.5">
              <svg class="size-4 text-blue-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <rect width="20" height="16" x="2" y="4" rx="2" />
                <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
              </svg>
              <span class="text-sm text-slate-800">{{ COPY.registration.email }}</span>
            </div>
          </div>
          <div>
            <label class="mb-1 block text-sm font-medium text-slate-700">Passord</label>
            <div class="flex items-center justify-between rounded-lg border border-blue-200 bg-blue-50 px-3 py-2.5">
              <div class="flex items-center gap-2">
                <svg class="size-4 text-blue-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <rect width="18" height="11" x="3" y="11" rx="2" />
                  <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                </svg>
                <span class="text-sm text-slate-800">{{ COPY.registration.password }}</span>
              </div>
              <svg class="size-4 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            </div>
          </div>
          <div>
            <label class="mb-1 block text-sm font-medium text-slate-700">Bekreft passord</label>
            <div class="flex items-center justify-between rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5">
              <div class="flex items-center gap-2">
                <svg class="size-4 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <rect width="18" height="11" x="3" y="11" rx="2" />
                  <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                </svg>
                <span class="text-sm text-slate-800">{{ COPY.registration.password }}</span>
              </div>
              <svg class="size-4 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            </div>
          </div>
        </div>

        <label class="mt-5 flex items-start gap-2 text-sm text-slate-600">
          <input type="checkbox" checked class="mt-0.5 rounded border-slate-300" disabled />
          <span>
            Jeg godtar
            <span class="font-medium text-blue-600">personvernerklæringen</span>
          </span>
        </label>

        <div class="mt-5 flex items-center justify-center gap-2 rounded-lg bg-slate-800 px-4 py-3 text-xs text-slate-300">
          <svg class="size-4 animate-spin text-orange-400" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          {{ COPY.registration.verifying }}
          <span class="font-semibold text-white">CLOUDFLARE</span>
        </div>
      </div>
    </section>

    <!-- Panel 3: Husstand -->
    <section
      v-bind="daAttr('husstandPanel')"
      class="demo-hidden absolute inset-0 overflow-y-auto bg-white px-8 pb-10 pt-8"
    >
      <h2 class="text-center text-3xl font-bold text-slate-900">
        {{ COPY.husstand.title }}
      </h2>
      <p class="mx-auto mt-3 max-w-2xl text-center text-sm text-slate-500">
        {{ COPY.husstand.subcopy }}
      </p>

      <div class="mx-auto mt-10 grid max-w-4xl grid-cols-2 gap-6">
        <div class="rounded-2xl border border-slate-100 bg-white p-8 shadow-lg">
          <div class="mb-4 flex size-14 items-center justify-center rounded-full bg-blue-100 text-blue-600">
            <svg class="size-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M3 10.5 12 3l9 7.5V21H3z" />
              <path d="M9 21v-6h6v6" />
            </svg>
          </div>
          <h3 class="text-xl font-bold text-slate-900">{{ COPY.husstand.createTitle }}</h3>
          <p class="mt-3 text-sm leading-relaxed text-slate-500">{{ COPY.husstand.createDesc }}</p>
          <button
            type="button"
            class="mt-6 inline-flex items-center gap-2 rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white"
          >
            <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M12 5v14M5 12h14" />
            </svg>
            {{ COPY.husstand.createCta }}
          </button>
        </div>

        <div class="rounded-2xl border border-slate-100 bg-white p-8 shadow-lg">
          <div class="mb-4 flex size-14 items-center justify-center rounded-full bg-emerald-100 text-emerald-600">
            <svg class="size-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
          </div>
          <h3 class="text-xl font-bold text-slate-900">{{ COPY.husstand.joinTitle }}</h3>
          <p class="mt-3 text-sm leading-relaxed text-slate-500">{{ COPY.husstand.joinDesc }}</p>
          <button
            type="button"
            class="mt-6 inline-flex items-center gap-2 rounded-lg bg-emerald-600 px-5 py-2.5 text-sm font-semibold text-white"
          >
            <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M12 5v14M5 12h14" />
            </svg>
            {{ COPY.husstand.joinCta }}
          </button>
        </div>
      </div>

      <h3 class="mt-12 text-center text-xl font-bold text-slate-900">
        {{ COPY.husstand.benefitsTitle }}
      </h3>
      <div class="mx-auto mt-6 grid max-w-4xl grid-cols-3 gap-6">
        <div
          v-for="benefit in COPY.husstand.benefits"
          :key="benefit.title"
          class="text-center"
        >
          <div class="mx-auto mb-3 flex size-12 items-center justify-center rounded-full bg-blue-100 text-blue-600">
            <svg class="size-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
          </div>
          <h4 class="font-semibold text-slate-900">{{ benefit.title }}</h4>
          <p class="mt-1 text-xs text-slate-500">{{ benefit.desc }}</p>
        </div>
      </div>
    </section>

    <!-- Panel 4: Kriser -->
    <section
      v-bind="daAttr('kriserPanel')"
      class="demo-hidden absolute inset-0 overflow-y-auto bg-slate-50 px-8 pb-10 pt-8"
    >
      <div class="flex items-start justify-between gap-6">
        <div>
          <h2 class="text-3xl font-bold text-slate-900">{{ COPY.kriser.title }}</h2>
          <p class="mt-2 max-w-xl text-sm text-slate-500">{{ COPY.kriser.subcopy }}</p>
        </div>
        <button
          type="button"
          class="shrink-0 rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm"
        >
          {{ COPY.kriser.reflections }}
        </button>
      </div>

      <div class="mt-8 flex gap-6">
        <div class="min-w-0 flex-1">
          <div class="mb-4 flex gap-6 border-b border-slate-200">
            <button
              v-for="(tab, i) in COPY.kriser.tabs"
              :key="tab"
              type="button"
              class="pb-2 text-sm font-medium"
              :class="i === 0 ? 'border-b-2 border-blue-600 text-blue-600' : 'text-slate-500'"
            >
              {{ tab }}
              <span
                v-if="tab === 'Pågående'"
                class="ml-1 inline-block size-1.5 rounded-full bg-red-500"
                aria-hidden="true"
              />
            </button>
          </div>

          <div class="space-y-4">
            <div
              v-for="event in COPY.kriser.events"
              :key="event.title"
              class="rounded-xl border border-slate-100 bg-white p-5 shadow-sm"
              :class="{
                'border-l-4 border-l-red-500': event.statusColor === 'red',
                'border-l-4 border-l-blue-500': event.statusColor === 'blue',
              }"
            >
              <div class="flex items-start justify-between gap-3">
                <div>
                  <span
                    class="inline-block rounded-full border px-2.5 py-0.5 text-xs font-semibold"
                    :class="statusColors[event.statusColor]"
                  >
                    {{ event.status }}
                  </span>
                  <h3 class="mt-2 font-bold text-slate-900">{{ event.title }}</h3>
                  <p class="mt-1 text-sm text-slate-500">{{ event.desc }}</p>
                  <p class="mt-2 text-xs text-slate-400">{{ event.date }}</p>
                </div>
              </div>
              <button type="button" class="mt-3 text-sm font-medium text-blue-600">
                Se detaljer &gt;
              </button>
            </div>
          </div>
        </div>

        <aside class="w-64 shrink-0">
          <div class="rounded-xl border border-slate-100 bg-white p-4 shadow-sm">
            <h3 class="mb-3 text-sm font-bold text-slate-900">{{ COPY.kriser.mapTitle }}</h3>
            <div class="flex h-40 flex-col items-center justify-center rounded-lg bg-slate-100 text-slate-400">
              <svg class="mb-2 size-8 text-blue-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M3 6l6-3 6 3 6-3v15l-6 3-6-3-6 3V6z" />
                <path d="M9 3v15M15 6v15" />
              </svg>
              <span class="text-xs font-medium text-blue-600">{{ COPY.kriser.mapCta }}</span>
            </div>
            <button type="button" class="mt-3 text-xs font-medium text-blue-600">
              {{ COPY.kriser.mapLink }} &gt;
            </button>
          </div>
        </aside>
      </div>
    </section>
  </div>
</template>

<style scoped>
.demo-hidden {
  visibility: hidden;
  opacity: 0;
  pointer-events: none;
}
</style>
