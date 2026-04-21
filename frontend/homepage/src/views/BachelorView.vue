<script setup lang="ts">
import { computed, ref } from 'vue'
import { useLangStore } from '../stores/lang'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { GraduationCap, Sparkles, Link2, Play } from 'lucide-vue-next'
import { buildCloudinaryImageUrl, buildCloudinarySrcSet } from '@/utils/cloudinary'

const langStore = useLangStore()
const isNo = computed(() => langStore.language === 'no')

const YOUTUBE_EMBED_ID = 'YHdEJhM-J2o'
const YOUTUBE_WATCH_URL = `https://www.youtube.com/watch?v=${YOUTUBE_EMBED_ID}`
const videoLoaded = ref(false)
const videoPosterId = `portfolio/bachelor/${YOUTUBE_EMBED_ID}-poster`
const videoPosterSrc = computed(() =>
  buildCloudinaryImageUrl(videoPosterId, ['f_auto', 'q_auto', 'c_fill', 'g_auto', 'ar_16:9', 'w_1280']),
)
const videoPosterSrcSet = computed(() =>
  buildCloudinarySrcSet(videoPosterId, [480, 768, 1024, 1280], ['c_fill', 'g_auto', 'ar_16:9']),
)

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
					'Denne porteføljen bygger videre på arbeidet jeg har gjort i bacheloroppgaven (2026) hos Piscada AS i Trondheim. Oppgaven har gitt meg en solid forståelse for hvordan RAG, språkmodeller og produksjonsklar programvare henger sammen — og det er den kompetansen jeg bruker når jeg videreutvikler nettsiden over tid.',
				lastUpdated: 'Sist oppdatert: april 2026',
			}
		: {
				title: "Bachelor's thesis",
				lead:
					"This portfolio builds on work from my bachelor's thesis (2026) at Piscada AS in Trondheim. The thesis gave me a solid grasp of how RAG, language models, and production-ready software fit together — and that's the foundation I use when I keep evolving this site over time.",
				lastUpdated: 'Last updated: April 2026',
			},
)

const videoSection = computed(() =>
	isNo.value
		? {
				heading: 'Hva vi har jobbet med',
				description:
					'I videoen under forklarer jeg og samarbeidspartneren min kort hva vi har jobbet med de siste månedene i forbindelse med bacheloroppgaven og det tilhørende prosjektet.',
				badge: 'Demo',
				iframeTitle: 'YouTube-video om bachelorprosjektet',
				watchLabel: 'Åpne på YouTube (ny fane)',
        playLabel: 'Spill av video',
			}
		: {
				heading: 'What we have been working on',
				description:
					'In the video below, my collaboration partner and I explain what we have been working on over the past few months as part of the bachelor thesis and related project.',
				badge: 'Demo',
				iframeTitle: "YouTube video about the bachelor's thesis project",
				watchLabel: 'Watch on YouTube (opens in a new tab)',
        playLabel: 'Play video',
			},
)

const narrativeCards = computed<NarrativeCard[]>(() =>
	isNo.value
		? [
				{
					category: 'Bakgrunn',
					title: 'Hva oppgaven handler om',
					body: [
						'Oppgaven er knyttet til industriell kontekst og utforsker hvordan retrieval-augmented generation og tilhørende verktøy kan brukes i praksis.',
						'Målet har vært å kombinere teori med konkret implementasjon, slik at resultatene kan diskuteres ut fra både kodekvalitet og brukeropplevelse.',
					],
				},
				{
					category: 'Ferdigheter',
					title: 'Det jeg har lært',
					body: [
						'Jeg har jobbet tett med API-design, dokumentpipelines, vektorlagring og evaluering av språkmodeller — områder som direkte gjenbrukes i porteføljen.',
						'Samarbeid med veileder og team hos Piscada har også trent meg i å scope arbeid, dokumentere beslutninger og levere i iterasjoner.',
					],
				},
				{
					category: 'Porteføljen',
					title: 'Hvorfor dette gir mening her',
					body: [
						'Nettsiden er et levende sted der jeg kan teste ideer fra oppgaven, holde teknologistakken oppdatert og vise hvordan jeg tenker om AI-produkter.',
						'Du finner mer teknisk detalj på siden Teknologistakk, og en forskningsforankret roadmap på Videre arbeid.',
					],
				},
			]
		: [
				{
					category: 'Background',
					title: 'What the thesis is about',
					body: [
						'The thesis sits in an industrial setting and explores how retrieval-augmented generation and related tooling work in practice.',
						'The aim has been to pair theory with concrete implementation so results can be discussed in terms of both code quality and user experience.',
					],
				},
				{
					category: 'Skills',
					title: 'What I took away',
					body: [
						'I worked closely with API design, document pipelines, vector storage, and evaluation of language models — areas that map directly onto this portfolio.',
						'Collaboration with supervisors and the team at Piscada also trained me to scope work, document decisions, and ship in iterations.',
					],
				},
				{
					category: 'Portfolio',
					title: 'Why this connects to the site',
					body: [
						'The site is a living place to try ideas from the thesis, keep the tech stack current, and show how I think about AI-powered products.',
						'For more technical depth, see Tech stack; for a research-backed roadmap, see Future work.',
					],
				},
			],
)
</script>

<template>
	<main class="min-h-screen pt-20 bg-gradient-to-br from-slate-50 to-slate-100 relative pb-16">
		<div class="absolute inset-0 pointer-events-none">
			<div
				class="absolute top-0 left-0 w-full h-full"
				style="
					background: radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.08) 0%, transparent 50%),
						radial-gradient(circle at 80% 20%, rgba(37, 99, 235, 0.08) 0%, transparent 50%),
						radial-gradient(circle at 50% 50%, rgba(96, 165, 250, 0.05) 0%, transparent 70%);
				"
			></div>
		</div>

		<div class="relative z-10 mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
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
              <template v-if="videoLoaded">
                <iframe
                  class="absolute inset-0 h-full w-full"
                  :src="`https://www.youtube.com/embed/${YOUTUBE_EMBED_ID}?autoplay=1`"
                  :title="videoSection.iframeTitle"
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                  allowfullscreen
                  referrerpolicy="strict-origin-when-cross-origin"
                ></iframe>
              </template>
              <template v-else>
                <img
                  :src="videoPosterSrc"
                  :srcset="videoPosterSrcSet || undefined"
                  sizes="(max-width: 768px) 100vw, 896px"
                  :alt="videoSection.iframeTitle"
                  class="absolute inset-0 h-full w-full object-cover"
                  loading="lazy"
                  decoding="async"
                />
                <button
                  type="button"
                  class="absolute inset-0 flex items-center justify-center bg-slate-900/30 transition-colors duration-300 hover:bg-slate-900/45"
                  @click="videoLoaded = true"
                >
                  <span
                    class="inline-flex items-center gap-2 rounded-full bg-white/95 px-4 py-2 text-sm font-semibold text-blue-700 shadow-lg"
                  >
                    <Play class="h-4 w-4 fill-current" aria-hidden="true" />
                    {{ videoSection.playLabel }}
                  </span>
                </button>
              </template>
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
								{{ card.category }}
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
	</main>
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
