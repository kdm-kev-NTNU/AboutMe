<script setup lang="ts">
import { COPY, DATA_ATTRIBUTES } from './film-script'

function daAttr(name: keyof typeof DATA_ATTRIBUTES): Record<string, string> {
  return { [DATA_ATTRIBUTES[name]]: "" };
}
</script>

<template>
  <div class="relative flex h-full flex-col px-6 pt-24">
    <!-- Hero panel -->
    <section
      v-bind="daAttr('heroCard')"
      class="relative z-10 mx-auto grid w-full max-w-6xl items-center gap-8 overflow-hidden rounded-[2rem] border border-blue-100/80 bg-white/82 p-8 shadow-2xl shadow-blue-950/10 backdrop-blur-xl lg:grid-cols-[1.1fr_0.9fr] lg:p-10"
    >
      <div class="min-w-0">
        <div
          v-bind="daAttr('statusPill')"
          class="mb-5 inline-flex items-center gap-2 rounded-full border border-blue-100 bg-blue-50/80 px-3 py-1.5 text-xs font-semibold text-blue-800"
        >
          <span class="size-2 rounded-full bg-emerald-500" aria-hidden="true" />
          {{ COPY.hero.statusPill }}
        </div>

        <h1
          v-bind="daAttr('headline')"
          class="max-w-3xl text-4xl font-bold tracking-tight text-slate-950 lg:text-5xl"
        >
          {{ COPY.hero.headline }}
        </h1>

        <p
          v-bind="daAttr('subcopy')"
          class="mt-5 max-w-2xl text-base leading-7 text-slate-600 lg:text-lg"
        >
          {{ COPY.hero.subcopy }}
        </p>

        <div class="mt-7 flex flex-col gap-3 sm:flex-row">
          <button
            v-bind="daAttr('voiceButton')"
            type="button"
            class="inline-flex h-14 items-center justify-center rounded-2xl bg-gradient-to-r from-blue-600 to-indigo-700 px-7 text-sm font-semibold text-white shadow-xl shadow-blue-500/25"
          >
            <svg
              class="me-2 size-5"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <path d="M3 18v-6a9 9 0 0 1 18 0v6" />
              <path
                d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z"
              />
            </svg>
            {{ COPY.hero.voiceButton }}
          </button>

          <button
            v-bind="daAttr('textButton')"
            type="button"
            class="inline-flex h-14 items-center justify-center rounded-2xl border border-blue-200 bg-white/85 px-7 text-sm font-semibold text-slate-800"
          >
            <svg
              class="me-2 size-5"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <path
                d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"
              />
            </svg>
            {{ COPY.hero.textButton }}
          </button>
        </div>
      </div>

      <div
        class="relative mx-auto flex min-h-[18rem] w-full max-w-md flex-col items-center justify-center"
      >
        <div
          class="absolute inset-6 rounded-full bg-blue-500/10 blur-3xl"
          aria-hidden="true"
        />
        <button
          v-bind="daAttr('micOrb')"
          type="button"
          class="relative flex aspect-square w-64 items-center justify-center rounded-full bg-gradient-to-br from-cyan-400 via-blue-600 to-indigo-800 text-white shadow-2xl shadow-blue-700/30 ring-8 ring-white/70"
        >
          <span
            class="absolute inset-8 rounded-full border border-white/30"
            aria-hidden="true"
          />
          <svg
            class="size-20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            aria-hidden="true"
          >
            <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z" />
            <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
            <line x1="12" x2="12" y1="19" y2="22" />
          </svg>
        </button>
      </div>
    </section>

    <!-- Voice panel (hidden initially) -->
    <section
      v-bind="daAttr('voicePanel')"
      class="demo-hidden absolute inset-x-6 top-24 bottom-44 z-20 mx-auto flex max-w-2xl flex-col items-center"
    >
      <div class="mb-6 text-center">
        <h2
          class="text-2xl font-bold tracking-tight text-slate-800 sm:text-3xl"
        >
          {{ COPY.voice.title }}
        </h2>
        <button
          v-bind="daAttr('textChatCta')"
          type="button"
          class="mt-3 inline-flex items-center gap-1.5 text-sm font-medium text-blue-700"
        >
          <svg
            class="size-4 shrink-0"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            aria-hidden="true"
          >
            <path
              d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"
            />
          </svg>
          {{ COPY.voice.textChatCta }}
        </button>
      </div>

      <div
        v-bind="daAttr('voiceStatus')"
        class="demo-hidden mb-4 inline-flex items-center gap-2 rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700"
      >
        <span
          class="size-2 animate-pulse rounded-full bg-blue-500"
          aria-hidden="true"
        />
        {{ COPY.voice.statusListening }}
      </div>

      <div class="flex w-full max-w-md flex-col items-center gap-6">
        <div
          v-bind="daAttr('voiceMicBtn')"
          class="relative flex h-32 w-32 items-center justify-center rounded-full bg-gradient-to-br from-blue-600 to-indigo-700 shadow-xl shadow-blue-500/25"
        >
          <svg
            class="size-14 text-white opacity-90"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            aria-hidden="true"
          >
            <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z" />
            <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
            <line x1="12" x2="12" y1="19" y2="22" />
          </svg>
        </div>

        <label
          class="w-full text-left text-xs font-semibold uppercase text-slate-600"
        >
          {{ COPY.voice.modelLabel }}
          <select
            disabled
            class="mt-1 h-10 w-full rounded-lg border border-blue-100 bg-white/90 px-3 text-sm font-medium normal-case text-slate-800 shadow-sm"
          >
            <option>{{ COPY.voice.modelValue }}</option>
          </select>
        </label>

        <div class="grid w-full grid-cols-1 gap-3 sm:grid-cols-2">
          <label
            class="text-left text-xs font-semibold uppercase text-slate-600"
          >
            {{ COPY.voice.voiceLabel }}
            <select
              disabled
              class="mt-1 h-10 w-full rounded-lg border border-blue-100 bg-white/90 px-3 text-sm font-medium normal-case text-slate-800 shadow-sm"
            >
              <option>{{ COPY.voice.voiceValue }}</option>
            </select>
          </label>

          <label
            class="text-left text-xs font-semibold uppercase text-slate-600"
          >
            {{ COPY.voice.reasoningLabel }}
            <select
              disabled
              class="mt-1 h-10 w-full rounded-lg border border-blue-100 bg-white/90 px-3 text-sm font-medium normal-case text-slate-800 shadow-sm"
            >
              <option>{{ COPY.voice.reasoningValue }}</option>
            </select>
          </label>
        </div>

        <button
          type="button"
          class="inline-flex h-10 items-center justify-center rounded-md bg-slate-900 px-4 text-sm font-semibold text-white shadow-sm"
        >
          {{ COPY.voice.connectLabel }}
        </button>
      </div>
    </section>

    <!-- Chat panel (hidden initially) -->
    <section
      v-bind="daAttr('chatPanel')"
      class="demo-hidden absolute inset-x-6 top-24 bottom-12 z-20 mx-auto flex w-full max-w-4xl flex-col"
    >
      <div
        class="mb-5 flex-shrink-0 rounded-3xl border border-blue-100/70 bg-white/85 p-4 shadow-lg shadow-blue-900/10 backdrop-blur-xl sm:p-5"
      >
        <div class="flex items-center justify-between gap-3">
          <div class="flex items-center gap-3">
            <h2
              class="text-2xl font-bold tracking-tight text-slate-800 sm:text-3xl"
            >
              {{ COPY.chat.title }}
            </h2>
            <span
              class="rounded-full border border-blue-200/70 bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700"
            >
              {{ COPY.chat.badgeOnline }}
            </span>
          </div>
          <button
            type="button"
            class="inline-flex h-9 items-center justify-center gap-1.5 rounded-md border border-blue-200/80 bg-white/85 px-3 text-sm font-medium text-blue-700"
          >
            <svg
              class="size-4 shrink-0"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <path d="M3 18v-6a9 9 0 0 1 18 0v6" />
              <path
                d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z"
              />
            </svg>
            {{ COPY.chat.voiceChatButton }}
          </button>
        </div>
      </div>

      <div
        class="mb-4 flex-shrink-0 rounded-lg border border-blue-200 bg-blue-50/90 px-4 py-3 text-blue-950"
        role="note"
      >
        <div class="flex gap-3">
          <svg
            class="mt-0.5 size-4 shrink-0 text-blue-700"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            aria-hidden="true"
          >
            <path d="M12 8V4H8" />
            <rect width="16" height="12" x="4" y="8" rx="2" />
            <path d="M2 14h2" />
            <path d="M20 14h2" />
            <path d="M15 13v2" />
            <path d="M9 13v2" />
          </svg>
          <div>
            <p class="text-sm font-semibold text-blue-900">
              {{ COPY.chat.disclaimer }}
            </p>
            <p class="mt-1 text-sm text-blue-800">
              {{ COPY.chat.disclaimerSub }}
            </p>
            <span
              class="mt-2 inline-block text-sm font-medium text-blue-700 underline underline-offset-2"
            >
              {{ COPY.chat.disclaimerLink }}
            </span>
          </div>
        </div>
      </div>

      <div
        class="relative mb-6 flex-1 overflow-y-auto rounded-3xl border border-blue-100/70 bg-white/85 p-5 shadow-lg shadow-blue-900/10 backdrop-blur-xl sm:p-6"
      >
        <div
          v-bind="daAttr('chatEmpty')"
          class="absolute inset-0 flex items-center justify-center text-slate-500"
        >
          <div class="text-center">
            <svg
              class="mx-auto mb-2 h-8 w-8 opacity-50"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <path
                d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"
              />
            </svg>
            <p class="text-sm">{{ COPY.chat.emptyState }}</p>
          </div>
        </div>

        <div class="space-y-5">
          <div class="flex justify-end">
            <div
              v-bind="daAttr('chatUserMsg')"
              class="demo-hidden max-w-[85%] rounded-2xl border border-blue-500/70 bg-gradient-to-r from-blue-600 to-blue-700 px-4 py-3.5 text-sm text-white shadow-lg shadow-blue-500/35 sm:max-w-[78%]"
            />
          </div>

          <div class="flex justify-start">
            <div
              v-bind="daAttr('chatThinking')"
              class="demo-hidden rounded-2xl border border-blue-100/80 bg-white/95 px-4 py-3.5 shadow-sm"
            >
              <div class="flex items-center gap-1">
                <div class="size-2 animate-bounce rounded-full bg-blue-400" />
                <div
                  class="size-2 animate-bounce rounded-full bg-blue-400 [animation-delay:0.1s]"
                />
                <div
                  class="size-2 animate-bounce rounded-full bg-blue-400 [animation-delay:0.2s]"
                />
              </div>
            </div>
          </div>

          <div class="flex justify-start">
            <div
              v-bind="daAttr('chatAiMsg')"
              class="demo-hidden max-w-[85%] rounded-2xl border border-blue-100/80 bg-white/95 px-4 py-3.5 text-sm text-slate-700 shadow-sm sm:max-w-[78%]"
            />
          </div>
        </div>
      </div>

      <div class="space-y-4">
        <div class="grid gap-3 text-sm text-slate-700 sm:grid-cols-2">
          <div
            class="rounded-2xl border border-blue-100/70 bg-white/85 px-4 py-3 shadow-sm backdrop-blur-md"
          >
            <p
              class="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500"
            >
              {{ COPY.chat.providerLabel }}
            </p>
            <div
              class="relative flex w-fit rounded-full border border-blue-100/70 bg-slate-100/80 p-1"
            >
              <div
                class="absolute bottom-1 top-1 w-28 rounded-full border border-blue-100 bg-white shadow-sm"
              />
              <button
                type="button"
                class="relative z-10 w-28 rounded-full py-2 text-sm font-semibold text-blue-700"
              >
                {{ COPY.chat.providerOpenAI }}
              </button>
              <button
                type="button"
                class="relative z-10 w-28 rounded-full py-2 text-sm font-medium text-slate-500"
              >
                {{ COPY.chat.providerAnthropic }}
              </button>
            </div>
          </div>

          <div
            class="rounded-2xl border border-blue-100/70 bg-white/85 px-4 py-3 shadow-sm backdrop-blur-md"
          >
            <label
              class="mb-2 block text-xs font-semibold uppercase tracking-wide text-slate-500"
            >
              {{ COPY.chat.modelLabel }}
            </label>
            <select
              disabled
              class="w-full rounded-xl border border-blue-200/70 bg-white/90 px-3 py-2.5 text-sm text-slate-700"
            >
              <option>{{ COPY.chat.modelValue }}</option>
            </select>
          </div>
        </div>

        <div
          class="flex items-center gap-3 rounded-3xl border border-blue-100/70 bg-white/85 p-3 shadow-lg shadow-blue-900/10 backdrop-blur-xl"
        >
          <button
            type="button"
            class="inline-flex size-10 shrink-0 items-center justify-center rounded-xl text-slate-500"
            aria-hidden="true"
          >
            <svg
              class="size-5"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z" />
              <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
              <line x1="12" x2="12" y1="19" y2="22" />
            </svg>
          </button>
          <input
            v-bind="daAttr('chatInput')"
            type="text"
            readonly
            :placeholder="COPY.chat.inputPlaceholder"
            class="flex-1 rounded-xl border border-blue-100 bg-slate-50 px-4 py-2.5 text-sm text-slate-700 outline-none"
          />
          <button
            v-bind="daAttr('chatSend')"
            type="button"
            class="inline-flex h-10 items-center justify-center gap-1.5 rounded-xl bg-blue-600 px-4 text-sm font-semibold text-white"
          >
            {{ COPY.chat.sendButton }}
            <svg
              class="size-4"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <path d="M5 12h14" />
              <path d="m12 5 7 7-7 7" />
            </svg>
          </button>
        </div>
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
