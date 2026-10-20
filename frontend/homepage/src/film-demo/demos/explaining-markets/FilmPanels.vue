<script setup lang="ts">
import { COPY, DATA_ATTRIBUTES } from './film-script'

function daAttr(name: keyof typeof DATA_ATTRIBUTES): Record<string, string> {
  return { [DATA_ATTRIBUTES[name]]: '' }
}
</script>

<template>
  <div class="relative h-full">
    <!-- Panel 1: Konkurransen -->
    <section
      v-bind="daAttr('competitionPanel')"
      class="absolute inset-0 overflow-hidden bg-slate-50 px-8 py-7"
    >
      <h1
        v-bind="daAttr('competitionTitle')"
        class="max-w-4xl text-3xl font-bold leading-tight text-slate-900"
      >
        {{ COPY.competition.title }}
      </h1>
      <p
        v-bind="daAttr('competitionIntro')"
        class="mt-3 max-w-3xl text-sm leading-relaxed text-slate-600"
      >
        {{ COPY.competition.intro }}
      </p>

      <div
        v-bind="daAttr('competitionMetrics')"
        class="mt-6 grid grid-cols-4 gap-3"
      >
        <div
          v-for="metric in COPY.competition.metrics"
          :key="metric.label"
          class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm"
        >
          <p class="text-xs font-medium text-slate-500">{{ metric.label }}</p>
          <p class="mt-1 text-lg font-bold text-slate-900">{{ metric.value }}</p>
          <p class="mt-2 text-xs font-semibold text-emerald-600">↑ {{ metric.delta }}</p>
        </div>
      </div>

      <div
        v-bind="daAttr('competitionQuestion')"
        class="mt-6 rounded-xl border border-sky-100 bg-white p-5 shadow-sm"
      >
        <h2 class="text-base font-semibold text-slate-900">
          {{ COPY.competition.questionTitle }}
        </h2>
        <blockquote class="mt-3 border-l-4 border-sky-400 pl-4 text-sm italic leading-relaxed text-slate-700">
          {{ COPY.competition.question }}
        </blockquote>
      </div>
    </section>

    <!-- Panel 2: Agenten -->
    <section
      v-bind="daAttr('agentPanel')"
      class="demo-hidden absolute inset-0 overflow-hidden bg-slate-50 px-8 py-7"
    >
      <div v-bind="daAttr('agentHeading')">
        <p class="text-xs font-semibold uppercase tracking-wide text-sky-700">
          {{ COPY.agent.label }}
        </p>
        <h1 class="mt-1 max-w-3xl text-3xl font-bold leading-tight text-slate-900">
          {{ COPY.agent.title }}
        </h1>
        <p class="mt-3 max-w-3xl text-sm leading-relaxed text-slate-600">
          {{ COPY.agent.subcopy }}
        </p>
      </div>

      <div v-bind="daAttr('agentClocks')" class="mt-6 grid grid-cols-2 gap-4">
        <div
          v-for="clock in COPY.agent.clocks"
          :key="clock.value"
          class="rounded-xl border border-slate-200 bg-white p-5 shadow-sm"
        >
          <p class="text-3xl font-bold text-sky-700">{{ clock.value }}</p>
          <p class="mt-2 text-sm font-semibold text-slate-900">{{ clock.title }}</p>
          <p class="mt-1 text-sm leading-relaxed text-slate-600">{{ clock.desc }}</p>
        </div>
      </div>

      <div v-bind="daAttr('agentRules')" class="mt-6">
        <h2 class="text-base font-semibold text-slate-900">{{ COPY.agent.rulesTitle }}</h2>
        <div class="mt-3 grid grid-cols-3 gap-3">
          <div
            v-for="rule in COPY.agent.rules"
            :key="rule.title"
            class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm"
          >
            <p class="text-sm font-semibold text-slate-900">{{ rule.title }}</p>
            <p class="mt-2 text-xs leading-relaxed text-slate-600">{{ rule.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Panel 3: Forskning -->
    <section
      v-bind="daAttr('researchPanel')"
      class="demo-hidden absolute inset-0 overflow-hidden bg-slate-50 px-8 py-7"
    >
      <div v-bind="daAttr('researchHeading')">
        <p class="text-xs font-semibold uppercase tracking-wide text-sky-700">
          {{ COPY.research.label }}
        </p>
        <h1 class="mt-1 max-w-3xl text-3xl font-bold leading-tight text-slate-900">
          {{ COPY.research.title }}
        </h1>
        <p class="mt-3 max-w-3xl text-sm leading-relaxed text-slate-600">
          {{ COPY.research.subcopy }}
        </p>
      </div>

      <div v-bind="daAttr('researchStats')" class="mt-6 grid grid-cols-3 gap-3">
        <div
          v-for="stat in COPY.research.stats"
          :key="stat.label"
          class="rounded-xl border border-slate-200 bg-white p-4 text-center shadow-sm"
        >
          <p class="text-2xl font-bold text-slate-900">{{ stat.value }}</p>
          <p class="mt-1 text-xs text-slate-500">{{ stat.label }}</p>
        </div>
      </div>

      <div
        v-bind="daAttr('researchPaper')"
        class="mt-6 rounded-xl border border-slate-200 bg-white p-5 shadow-sm"
      >
        <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">
          {{ COPY.research.weekHeading }}
        </p>
        <h2 class="mt-2 text-base font-semibold text-slate-900">
          {{ COPY.research.paper.title }}
        </h2>
        <p class="mt-1 text-xs text-sky-700">{{ COPY.research.paper.meta }}</p>
        <p class="mt-3 text-sm leading-relaxed text-slate-600">
          {{ COPY.research.paper.body }}
        </p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.demo-hidden {
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
}
</style>
