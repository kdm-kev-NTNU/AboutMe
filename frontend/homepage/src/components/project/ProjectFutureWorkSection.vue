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
		title: 'Live voice with Kevin: talk directly, not just the AI',
		intro:
			'A planned feature where visitors can request a short live voice conversation with Kevin himself — not the AI — for Q&A that goes beyond what the chatbot can safely cover, or for human nuance (tone, follow-ups, clarifications). This is separate from the site’s OpenAI Realtime voice mode, which stays as “talk to Kevin’s AI”.',
		points: [
			'Scheduling: clear availability windows (time zones) and a lightweight booking or “request a call” flow with expected response time.',
			'Notifications: email or on-site notice when Kevin can take a slot, plus consent before any call.',
			'Transport: WebRTC peer-to-peer or a managed voice bridge from the browser, with minimal retained metadata and a short retention policy for logs.',
			'Optional multilingual path: experiment with GPT-Realtime-Translate-style live translation so visitors can speak their preferred language while Kevin replies in another, without replacing human judgment.',
		],
		refs: [],
	},
	{
		category: 'Personal',
		title: 'ElevenLabs voice agent: collect, then I process',
		intro:
			'Optional ElevenLabs capture adds spoken detail beyond polished pages alone. Raw exports or transcripts stay private and off the vector store by default. I turn them into notes or .txt drafts, upload through the document pipeline, then re-ingest and check retrieval before live chat. The arXiv links below are about RAG indexing, retrieval, and evaluation, not about picking a voice vendor (that is a product choice). Purpose, consent, and data minimisation follow privacy law, not those papers.',
		points: [
			'Collect with clear purpose, consent, and boundaries on what is stored.',
			'Personally process raw material before anything is ingestible or visitor-facing.',
			'Upload curated files through the admin document pipeline.',
			'Re-ingest and verify retrieval on new chunks before they power Kevin’s AI in live chat.',
		],
		refs: [
			{
				label: 'Wang et al.: Searching for Best Practices in Retrieval-Augmented Generation (arXiv:2407.01219, EMNLP 2024)',
				href: 'https://arxiv.org/abs/2407.01219',
			},
			{
				label: 'Gao et al.: Retrieval-Augmented Generation for Large Language Models: A Survey (arXiv:2312.10997, Dec 2023 preprint; often cited as “2024” elsewhere)',
				href: 'https://arxiv.org/abs/2312.10997',
			},
			{
				label: 'Abbasiantaeb et al.: Conversational Gold: evaluating with human gold nuggets (arXiv:2503.09902)',
				href: 'https://arxiv.org/abs/2503.09902',
			},
			{
				label: 'Retrieval Augmented Generation Evaluation in the Era of Large Language Models: A Comprehensive Survey (arXiv:2504.14891)',
				href: 'https://arxiv.org/abs/2504.14891',
			},
		],
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
		title: 'Live stemme med Kevin: snakk direkte, ikke bare med KI-en',
		intro:
			'Planlagt funksjon der besøkende kan be om en kort live samtale med Kevin selv — ikke KI-en — for spørsmål som passer bedre med menneske, eller for nyanser samtalen ikke bør stole på en modell for. Dette er noe annet enn OpenAI Realtime-stemmepå siden («snakk med Kevin sin AI»).',
		points: [
			'Planlegging: tydelige tidsvinduer (tidssoner) og en enkel booking- eller «be om samtale»-flyt med forventet svartid.',
			'Varsling: e-post eller melding på sida når Kevin har kapasitet, med samtykke før samtalen.',
			'Teknisk: WebRTC fra-til-bruker eller bro, minimale logger og kort lagringspolicy.',
			'Valgfritt flerspråklig spor: teste live oversettelse (i tråd med GPT-Realtime-Translate) slik at besøkende kan snakke sitt språk uten at det erstatter menneskelig vurdering.',
		],
		refs: [],
	},
	{
		category: 'Personlig',
		title: 'ElevenLabs stemmeagent: først inn, så rydder jeg',
		intro:
			'Valgfri samtale via ElevenLabs gir mer muntlig detalj enn bare ferdig skrevne sider. Rå filer eller transkripsjoner er ikke offentlige eller i vektorlageret som standard. Jeg strukturerer, korter ned, redakterer og lager notater eller .txt-utkast, laster opp via dokumentpipelinen, og kjører re-ingest og sjekk av henting før produksjonschat. arXiv-referansene under handler om indeksering, henting og evaluering i RAG, ikke om hvilken stemmeleverandør jeg velger (det er et produktvalg). Formål, samtykke og dataminimering følger personvernregelverket, ikke artiklene.',
		points: [
			'Samle inn med tydelig formål, samtykke og grenser for hva som lagres.',
			'Prosesser råmateriale selv før noe kan ingestes eller vises for besøkende.',
			'Last opp kuraterte filer via admin og dokumentpipelinen.',
			'Re-ingest og verifiser retrieval på nye chunks før de driver Kevin sin AI i live chat.',
		],
		refs: [
			{
				label: 'Wang m.fl.: Searching for Best Practices in Retrieval-Augmented Generation (arXiv:2407.01219, EMNLP 2024)',
				href: 'https://arxiv.org/abs/2407.01219',
			},
			{
				label: 'Gao m.fl.: Retrieval-Augmented Generation for Large Language Models: A Survey (arXiv:2312.10997, des. 2023; ofte sitert som «2024» andre steder)',
				href: 'https://arxiv.org/abs/2312.10997',
			},
			{
				label: 'Abbasiantaeb m.fl.: Conversational Gold: evaluering med menneskelige gull-nugger (arXiv:2503.09902)',
				href: 'https://arxiv.org/abs/2503.09902',
			},
			{
				label: 'Retrieval Augmented Generation Evaluation in the Era of Large Language Models: A Comprehensive Survey (arXiv:2504.14891)',
				href: 'https://arxiv.org/abs/2504.14891',
			},
		],
	},
	{
		category: 'Generering',
		title: 'Konsistens og reproduserbarhet i svar',
		intro:
			'Svar fra språkmodeller er fortsatt stokastiske. For portefølje-Q&A er det greit, men strammere dekoding og caching kan gjøre gjentatte spørsmål mer forutsigbare uten å miste nytteverdien.',
		points: [
			'Finjuster temperatur og nucleus sampling (top-p), og kombiner med strengere validering når modellen må levere strukturerte fragmenter (for eksempel JSON i interne utvidelsessteg).',
			'Cache svar for gjentatte eller nesten like spørsmål for lavere latency og lik begrunnelse for samme intensjon.',
			'Utforsk grammatikk- eller begrenset generering når fast svarstruktur kreves, slik at formattering blir mer stabil.',
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
		title: 'Spørringsanalyse og korpusvedlikehold',
		intro:
			'Ettersom chat-historikk og admin-verktøy finnes, kan spørsmålslogger klustreres for å avdekke gjentakende tema, manglende dokumenter eller utdaterte deler av kunnskapsbasen.',
		points: [
			'Klustr historiske spørsmål for å synliggjøre dekningsgap og prioritere nye opplastinger eller re-ingest av utdaterte filer.',
			'Bruk innsikten sammen med chunk-visningen i admin for å holde embeddings i tråd med det besøkende faktisk spør om.',
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
					'En forskningsforankret roadmap for det som kommer videre: RAG med vektorlagring, chat, flere modeller, dokument- og chunk-administrasjon, og promptversjoner. Porteføljen oppdateres fortløpende.',
				contextBefore:
					'Roadmapen viser hva jeg prioriterer mens jeg kontinuerlig tilpasser porteføljen med læring fra bacheloroppgaven (2026) hos Piscada AS i Trondheim og arbeid på ',
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
							? 'Foresight AI på piscada.com (åpner i ny fane)'
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
