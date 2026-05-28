<script setup lang="ts">
import { computed } from 'vue'
import { useLangStore } from '@/stores/lang'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'

const langStore = useLangStore()

type RefLink = { label: string; href: string }

type SectionCopy = {
	title: string
	intro: string
	points: string[]
	category: string
	refs: RefLink[]
}

const sectionsEn: SectionCopy[] = [
	{
		category: 'Voice',
		title: "Custom voice clone: Kevin\u2019s AI speaks in his own voice",
		intro:
			"A planned feature where the AI chatbot uses a cloned version of Kevin\u2019s actual voice, so visitors hear Kevin\u2019s tone and cadence when using voice mode \u2014 not a generic TTS voice. The current OpenAI Realtime voice mode stays, but the synthetic voice will be replaced with a personal clone trained on Kevin\u2019s speech.",
		points: [
			"Voice cloning: train a high-fidelity voice model on recordings of Kevin\u2019s speech using a suitable TTS provider, capturing natural intonation and rhythm.",
			'Integration: swap the default TTS output in the existing Realtime voice pipeline with the custom clone, keeping the same conversational AI backend.',
			'Quality control: validate that the clone sounds natural across different answer lengths, languages, and emotional tones before going live.',
			"Consent and transparency: clearly disclose to visitors that the voice is AI-generated from Kevin\u2019s recordings, not a live human conversation.",
		],
		refs: [],
	},
	{
		category: 'Generation',
		title: 'Output consistency and reproducibility',
		intro:
			'Chat answers are still stochastic. For portfolio Q&A this is acceptable, but tightening decoding and caching can make repeated questions feel more stable without sacrificing usefulness.',
		points: [
			'Tune temperature and nucleus sampling (top-p), and pair that with stricter validation when models must emit structured fragments (for example JSON used in internal expansion steps).',
			'Cache answers for repeated or near-duplicate queries so latency drops and users see identical grounding for the same intent.',
			'Explore grammar- or constraint-guided generation when a fixed response shape is required, so formatting stays predictable.',
		],
		refs: [
			{
				label: 'Renze & Guven (2024): The Effect of Sampling Temperature on Problem Solving in Large Language Models',
				href: 'https://arxiv.org/abs/2402.05201',
			},
			{
				label: 'Willard & Louf (2023): Efficient Guided Generation for Large Language Models',
				href: 'https://arxiv.org/abs/2307.09702',
			},
		],
	},
	{
		category: 'Analytics',
		title: 'Query analysis and corpus maintenance',
		intro:
			'Because chat history and admin tooling already exist, query logs can be clustered to spot recurring themes, missing documents, or stale sections of the knowledge base.',
		points: [
			'Cluster historical questions to highlight coverage gaps and prioritize new uploads or re-ingestion of outdated files.',
			'Use those insights together with chunk-level admin views to keep embeddings aligned with what visitors actually ask.',
		],
		refs: [
			{
				label: 'Wang et al. (2024): Searching for Best Practices in Retrieval-Augmented Generation',
				href: 'https://arxiv.org/abs/2407.01219',
			},
		],
	},
]

const sectionsNo: SectionCopy[] = [
	{
		category: 'Stemme',
		title: 'Egen stemmeklone: Kevins AI snakker med hans stemme',
		intro:
			'Planlagt funksjon der AI-chatboten bruker en klonet versjon av Kevins faktiske stemme, slik at bes\u00f8kende h\u00f8rer Kevins tone og rytme i stemmemodus \u2014 ikke en generisk TTS-stemme. Den eksisterende OpenAI Realtime-stemmemodusen beholdes, men den syntetiske stemmen byttes ut med en personlig klone trent p\u00e5 Kevins tale.',
		points: [
			'Stemmekloning: trene en h\u00f8ykvalitets stemmemodell p\u00e5 opptak av Kevins tale via en egnet TTS-leverand\u00f8r, med naturlig intonasjon og rytme.',
			'Integrasjon: bytte standard TTS-utdata i den eksisterende Realtime-stemmepipelinen med den egne klonen, med samme samtale-AI-backend.',
			'Kvalitetskontroll: validere at klonen l\u00e5ter naturlig p\u00e5 tvers av ulike svarlengder, spr\u00e5k og emosjonelle toner f\u00f8r lansering.',
			'Samtykke og \u00e5penhet: tydelig opplyse bes\u00f8kende om at stemmen er AI-generert fra Kevins opptak, ikke en live samtale med et menneske.',
		],
		refs: [],
	},
	{
		category: 'Generering',
		title: 'Konsistens og reproduserbarhet i svar',
		intro:
			'Svar fra spr\u00e5kmodeller er fortsatt stokastiske. For portef\u00f8lje-Q&A er det greit, men strammere dekoding og caching kan gj\u00f8re gjentatte sp\u00f8rsm\u00e5l mer forutsigbare uten \u00e5 miste nytteverdien.',
		points: [
			'Finjuster temperatur og nucleus sampling (top-p), og kombiner med strengere validering n\u00e5r modellen m\u00e5 levere strukturerte fragmenter (for eksempel JSON i interne utvidelsessteg).',
			'Cache svar for gjentatte eller nesten like sp\u00f8rsm\u00e5l for lavere latency og lik begrunnelse for samme intensjon.',
			'Utforsk grammatikk- eller begrenset generering n\u00e5r fast svarstruktur kreves, slik at formattering blir mer stabil.',
		],
		refs: [
			{
				label: 'Renze & Guven (2024): The Effect of Sampling Temperature on Problem Solving in Large Language Models',
				href: 'https://arxiv.org/abs/2402.05201',
			},
			{
				label: 'Willard & Louf (2023): Efficient Guided Generation for Large Language Models',
				href: 'https://arxiv.org/abs/2307.09702',
			},
		],
	},
	{
		category: 'Analyse',
		title: 'Sp\u00f8rringsanalyse og korpusvedlikehold',
		intro:
			'Ettersom chat-historikk og admin-verkt\u00f8y finnes, kan sp\u00f8rsm\u00e5lslogger klustreres for \u00e5 avdekke gjentakende tema, manglende dokumenter eller utdaterte deler av kunnskapsbasen.',
		points: [
			'Klustr historiske sp\u00f8rsm\u00e5l for \u00e5 synliggj\u00f8re dekningsgap og prioritere nye opplastinger eller re-ingest av utdaterte filer.',
			'Bruk innsikten sammen med chunk-visningen i admin for \u00e5 holde embeddings i tr\u00e5d med det bes\u00f8kende faktisk sp\u00f8r om.',
		],
		refs: [
			{
				label: 'Wang et al. (2024): Searching for Best Practices in Retrieval-Augmented Generation',
				href: 'https://arxiv.org/abs/2407.01219',
			},
		],
	},
]

const foresightAiProductUrl = 'https://piscada.com/foresight-ai'

const hero = computed(() =>
	langStore.language === 'no'
		? {
				title: 'Videre arbeid og forbedringer',
				lead:
					'En forskningsforankret roadmap for det som kommer videre: RAG med vektorlagring, chat, flere modeller, dokument- og chunk-administrasjon, og promptversjoner. Portef\u00f8ljen oppdateres fortl\u00f8pende.',
				contextBefore:
					'Roadmapen viser hva jeg prioriterer mens jeg kontinuerlig tilpasser portef\u00f8ljen med l\u00e6ring fra bacheloroppgaven (2026) hos Piscada AS i Trondheim og arbeid p\u00e5 ',
				contextLinkText: 'Foresight AI',
				contextAfter: ', som jeg utvikler sammen med en annen i teamet.',
				contextLinkHref: foresightAiProductUrl,
			}
		: {
				title: 'Future work and improvements',
				lead:
					'A research-backed roadmap for what comes next: RAG with a vector store, chat, multiple models, document and chunk administration, and prompt versioning. This portfolio keeps evolving.',
				contextBefore:
					"This roadmap reflects what I prioritize while continuously adapting the portfolio based on learnings from my bachelor's thesis (2026) at Piscada AS in Trondheim and work on ",
				contextLinkText: 'Foresight AI',
				contextAfter: ', which I develop with another teammate.',
				contextLinkHref: foresightAiProductUrl,
			},
)

const sections = computed(() => (langStore.language === 'no' ? sectionsNo : sectionsEn))

const referencesHeading = computed(() =>
	langStore.language === 'no' ? 'Referanser (arXiv)' : 'References (arXiv)',
)
</script>

<template>
	<div class="relative pb-8">
		<div class="relative z-10 mx-auto max-w-4xl px-4 py-4 sm:px-6 lg:px-8">
			<h1
				class="text-3xl font-bold mb-4 text-center bg-gradient-to-r from-blue-600 via-blue-700 to-blue-800 bg-clip-text text-transparent animate-gradient-x"
			>
				{{ hero.title }}
			</h1>
			<p class="text-center text-gray-600 max-w-3xl mx-auto mb-4 leading-relaxed">
				{{ hero.lead }}
			</p>
			<p class="text-center text-sm text-slate-600 max-w-3xl mx-auto mb-12 leading-relaxed">
				{{ hero.contextBefore }}<a
					:href="hero.contextLinkHref"
					class="font-medium text-blue-700 underline underline-offset-2 hover:text-blue-900"
					target="_blank"
					rel="noopener noreferrer"
					:aria-label="
						langStore.language === 'no'
							? 'Foresight AI p\u00e5 piscada.com (\u00e5pner i ny fane)'
							: 'Foresight AI on piscada.com (opens in a new tab)'
					"
					>{{ hero.contextLinkText }}</a>{{ hero.contextAfter }}
			</p>

			<div class="space-y-8">
				<Card
					v-for="(section, index) in sections"
					:key="`${section.title}-${index}`"
					class="relative border-2 border-transparent transition-all duration-300 bg-white/90 backdrop-blur-sm hover:border-blue-300/30 hover:bg-white/95 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-blue-500/15"
				>
					<CardHeader class="space-y-3">
						<div class="flex flex-wrap items-center gap-2">
							<Badge
								variant="secondary"
								class="text-xs border border-blue-300/30 text-blue-700 bg-blue-50/70"
							>
								{{ section.category }}
							</Badge>
						</div>
						<CardTitle class="text-xl text-gray-900">{{ section.title }}</CardTitle>
						<p class="text-sm text-gray-600 leading-relaxed">{{ section.intro }}</p>
					</CardHeader>
					<CardContent class="space-y-6">
						<ul class="list-disc pl-5 space-y-2 text-gray-700 text-sm leading-relaxed">
							<li v-for="(point, pIndex) in section.points" :key="pIndex">{{ point }}</li>
						</ul>
						<div
							v-if="section.refs.length > 0"
							class="rounded-lg border border-blue-100 bg-blue-50/40 p-4"
						>
							<p class="text-xs font-semibold uppercase tracking-wide text-blue-800 mb-2">
								{{ referencesHeading }}
							</p>
							<ul class="space-y-2">
								<li v-for="(ref, rIndex) in section.refs" :key="rIndex" class="text-sm">
									<a
										:href="ref.href"
										class="text-blue-700 hover:text-blue-900 underline underline-offset-2 break-words"
										target="_blank"
										rel="noopener noreferrer"
									>
										{{ ref.label }}
									</a>
								</li>
							</ul>
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
