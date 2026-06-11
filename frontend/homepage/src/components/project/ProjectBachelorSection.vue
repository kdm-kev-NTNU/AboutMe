<script setup lang="ts">
import { computed } from 'vue'
import { useLangStore } from '@/stores/lang'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { GraduationCap, Sparkles, Link2 } from 'lucide-vue-next'

const langStore = useLangStore()
const isNo = computed(() => langStore.language === 'no')

const YOUTUBE_EMBED_ID = 'GBre4cWKhto'
const YOUTUBE_WATCH_URL = `https://www.youtube.com/watch?v=${YOUTUBE_EMBED_ID}`

type NarrativeCard = {
	category: string
	title: string
	body: string[]
}

const hero = computed(() =>
	isNo.value
		? {
				title: 'Bacheloroppgaven',
				lead:
					'Bacheloroppgaven Foresight AI ble levert våren 2026 hos Piscada AS i Trondheim. Porteføljen er fortsatt et aktivt arbeid i utvikling — jeg tilpasser innhold, struktur og AI-atferd med det jeg lærte der, slik at siden blir tydeligere og mer treffsikker over tid.',
				lastUpdated: 'Sist oppdatert: juni 2026',
			}
		: {
				title: "Bachelor's thesis",
				lead:
					"The bachelor's thesis Foresight AI was delivered in spring 2026 at Piscada AS in Trondheim. This portfolio remains an active work in progress — I keep adapting content, structure, and AI behavior using what I learned there so the site becomes clearer and more accurate over time.",
				lastUpdated: 'Last updated: June 2026',
			},
)

const gradeStamp = computed(() =>
	isNo.value
		? { letter: 'A', label: 'IDATT2901 · vår 2026' }
		: { letter: 'A', label: 'IDATT2901 · spring 2026' },
)

const videoSection = computed(() =>
	isNo.value
		? {
				heading: 'Hva vi har jobbet med',
				description:
					'I videoen under presenterer jeg og Kaamya Shinde bachelorprosjektet Foresight AI om hvordan naturlig språk kan gjøre energiinnsikt mer tilgjengelig i samarbeid med Piscada. Videoen oppsummerer problemstilling, løsning, evaluering og hovedfunn, og forteller at stemmen er laget med en ElevenLabs voice-modell.',
				badge: 'Demo',
        iframeTitle: 'YouTube-video om bachelorprosjektet',
				watchLabel: 'Åpne på YouTube (ny fane)',
			}
		: {
				heading: 'What we have been working on',
				description:
					'In the video below, Kaamya Shinde and I present our bachelor project, Foresight AI, on democratizing energy insights through natural-language interaction in collaboration with Piscada. The recording summarizes the problem, solution, evaluation, and key findings, and notes that the narration uses an ElevenLabs voice model.',
				badge: 'Demo',
        iframeTitle: "YouTube video about the bachelor's thesis project",
				watchLabel: 'Watch on YouTube (opens in a new tab)',
			},
)

const narrativeCards = computed<NarrativeCard[]>(() =>
	isNo.value
		? [
				{
					category: 'Bakgrunn',
					title: 'Prosjekt og problemstilling',
					body: [
						'Bachelorprosjektet Foresight AI ble utviklet med Piscada for energistyringssystemer som samler store mengder bygnings- og driftsdata.',
						'Kjerneutfordringen var å støtte tolkning av energidata for brukere uten domenekompetanse, samtidig som vi balanserte svarkvalitet mot tokenkostnad.',
					],
				},
				{
					category: 'Implementasjon',
					title: 'Hva vi bygde',
					body: [
						'Vi bygde en RAG-basert assistent i et eksisterende dashboard der brukere kan stille spørsmål om valgte energigrafer og få forklaringer forankret i både grafdata og domenedokumenter.',
						'Frontend inkluderte chatbot, hurtigspørsmål, widget-kontekst og annotasjoner, mens backend inkluderte dokumentprosessering med anonymisering, promptversjonering, logging og tracing.',
					],
				},
				{
					category: 'Funn',
					title: 'Hva vi fant',
					body: [
						'Kontekstforankret retrieval gjorde energidata enklere å forstå, og funksjoner som TL;DR, sitater og annotasjoner økte tillit og tolkbarhet.',
						'RAG fjernet ikke hallusinasjoner helt, og vi fant en tydelig trade-off mellom kvalitet, ventetid og kostnad: ingen enkeltmodell var best i alle situasjoner.',
						'Konklusjonen var at slike assistenter kan forbedre tilgang til energiinnsikt, men krever transparent forankring, godt grensesnittdesign og kontinuerlig evaluering.',
					],
				},
				{
					category: 'Avslutning',
					title: 'Sensor og alfabet',
					body: [
						'Våren 2026 ble Foresight AI sensurert ved NTNU. På det skala-systemet vi bruker der, endte vi på første bokstav — bokstavelig talt.',
					],
				},
			]
		: [
				{
					category: 'Background',
					title: 'Project and problem statement',
					body: [
						'The bachelor project, Foresight AI, was developed with Piscada for energy-management systems that collect large volumes of building and operational data.',
						'The core challenge was helping non-experts interpret energy data while balancing response quality against token-usage cost.',
					],
				},
				{
					category: 'Implementation',
					title: 'What we built',
					body: [
						'We built a RAG-based assistant inside an existing dashboard so users can ask natural-language questions about selected energy charts and receive grounded explanations.',
						'The frontend included chatbot UX, quick analysis prompts, widget-specific context, and chart annotations, while the backend included document processing with PII removal, prompt versioning, logging, and tracing.',
					],
				},
				{
					category: 'Findings',
					title: 'What the evaluation showed',
					body: [
						'Context-aware retrieval made energy data easier to understand, and features such as TL;DR summaries, citations, and chart annotations improved trust and interpretation.',
						'RAG did not fully remove hallucinations, and we observed a clear trade-off between quality, latency, and token cost, meaning no single model was best in every scenario.',
						'Overall, the project indicates that LLM assistants can improve access to energy insights when paired with transparent grounding, careful interface design, and continuous evaluation.',
					],
				},
				{
					category: 'Wrap-up',
					title: 'Assessment and alphabet',
					body: [
						'In spring 2026, Foresight AI was assessed at NTNU. On the letter scale they use there, we landed on the first letter — literally.',
					],
				},
			],
)
</script>

<template>
	<div class="relative pb-8">
		<div class="relative z-10 mx-auto max-w-4xl px-4 py-4 sm:px-6 lg:px-8">
			<!-- Hero -->
			<div
				v-motion
				:initial="{ opacity: 0, y: 24 }"
				:visible-once="{ opacity: 1, y: 0, transition: { duration: 550 } }"
				class="text-center mb-10"
			>
				<div
					class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-gradient-to-br from-blue-600 to-indigo-700 text-white shadow-lg shadow-blue-500/25 mb-5"
				>
					<GraduationCap class="w-8 h-8" aria-hidden="true" />
				</div>
				<h1
					class="text-3xl sm:text-4xl font-bold mb-3 bg-gradient-to-r from-blue-600 via-blue-700 to-blue-800 bg-clip-text text-transparent animate-gradient-x"
				>
					{{ hero.title }}
				</h1>
				<p class="text-gray-600 max-w-3xl mx-auto leading-relaxed text-sm sm:text-base">
					{{ hero.lead }}
				</p>
				<div
					class="mt-5 inline-flex flex-col items-center gap-1.5"
					:aria-label="`${gradeStamp.letter}, ${gradeStamp.label}`"
				>
					<span
						class="inline-flex h-11 w-11 items-center justify-center rounded-full border-2 border-blue-600/40 bg-blue-50/80 text-xl font-bold text-blue-700 shadow-sm"
						aria-hidden="true"
					>
						{{ gradeStamp.letter }}
					</span>
					<span class="text-xs font-medium tracking-wide text-slate-500">{{ gradeStamp.label }}</span>
				</div>
				<p class="mt-3 text-xs text-gray-500">{{ hero.lastUpdated }}</p>
			</div>

			<!-- Video -->
			<div
				v-motion
				:initial="{ opacity: 0, y: 20 }"
				:visible-once="{ opacity: 1, y: 0, transition: { duration: 550, delay: 120 } }"
				class="mb-12"
			>
				<Card
					class="border-2 border-transparent bg-white/90 backdrop-blur-sm shadow-lg shadow-slate-900/5 overflow-hidden"
				>
					<CardHeader class="space-y-2 pb-2">
						<div class="flex flex-wrap items-center gap-2">
							<Badge
								variant="secondary"
								class="text-xs border border-blue-300/30 text-blue-700 bg-blue-50/70"
							>
								{{ videoSection.badge }}
							</Badge>
						</div>
						<CardTitle class="text-xl text-gray-900 flex items-center gap-2">
							<Sparkles class="w-5 h-5 text-blue-600 shrink-0" aria-hidden="true" />
							{{ videoSection.heading }}
						</CardTitle>
						<p class="text-sm text-gray-600 leading-relaxed">{{ videoSection.description }}</p>
					</CardHeader>
					<CardContent class="space-y-4 pt-0">
						<div
							class="relative w-full overflow-hidden rounded-xl border border-slate-200 bg-slate-900 aspect-video shadow-inner"
						>
							<iframe
								class="absolute inset-0 h-full w-full"
								:src="`https://www.youtube.com/embed/${YOUTUBE_EMBED_ID}`"
								:title="videoSection.iframeTitle"
								allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
								allowfullscreen
								referrerpolicy="strict-origin-when-cross-origin"
							></iframe>
						</div>
						<p class="text-center">
							<a
								:href="YOUTUBE_WATCH_URL"
								class="inline-flex items-center gap-1.5 text-sm font-medium text-blue-700 underline underline-offset-2 hover:text-blue-900"
								target="_blank"
								rel="noopener noreferrer"
							>
								<Link2 class="w-4 h-4 shrink-0" aria-hidden="true" />
								{{ videoSection.watchLabel }}
							</a>
						</p>
					</CardContent>
				</Card>
			</div>

			<!-- Narrative cards -->
			<div class="space-y-6">
				<Card
					v-for="(card, index) in narrativeCards"
					:key="card.title"
					v-motion
					:initial="{ opacity: 0, y: 22 }"
					:visible-once="{
						opacity: 1,
						y: 0,
						transition: { duration: 500, delay: 200 + index * 90 },
					}"
					class="relative border-2 border-transparent transition-all duration-300 bg-white/90 backdrop-blur-sm hover:border-blue-300/30 hover:bg-white/95 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-blue-500/15"
				>
					<CardHeader class="space-y-3">
						<div class="flex flex-wrap items-center gap-2">
							<Badge
								variant="secondary"
								class="text-xs border border-blue-300/30 text-blue-700 bg-blue-50/70"
							>
								<span
									v-if="card.category === 'Avslutning' || card.category === 'Wrap-up'"
									class="font-semibold text-blue-700"
								>{{ card.category.charAt(0) }}</span><span>{{ card.category.slice(1) }}</span>
							</Badge>
						</div>
						<CardTitle class="text-xl text-gray-900">{{ card.title }}</CardTitle>
					</CardHeader>
					<CardContent>
						<div class="space-y-3 text-sm text-gray-700 leading-relaxed">
							<p v-for="(para, pIdx) in card.body" :key="pIdx">{{ para }}</p>
						</div>
					</CardContent>
				</Card>
			</div>
		</div>
	</div>
</template>

<style scoped>
@keyframes gradient-x {
	0%,
	100% {
		background-position: 0% 50%;
	}
	50% {
		background-position: 100% 50%;
	}
}

.animate-gradient-x {
	background-size: 200% 200%;
	animation: gradient-x 3s ease-in-out infinite;
}

@media (max-width: 768px) {
	.hover\:-translate-y-0\.5:hover {
		transform: translateY(-1px);
	}
}
</style>
