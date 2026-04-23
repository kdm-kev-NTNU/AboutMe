<script setup lang="ts">
import { computed } from 'vue'
import { useLangStore } from '../stores/lang'
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
		category: 'Personal',
		title: 'Local model and Obsidian journal',
		intro:
			'I want a local model with my Obsidian journal as context, similar to a small journal-focused bot in the vault. The idea is to generate plain .txt drafts about me, not to publish the whole journal raw online.',
		points: [
			'Pick a local setup that fits my hardware and what I am comfortable sharing.',
			'Feed the model curated journal context (themes, timelines, decisions) so it can write useful .txt drafts.',
			'Read and trim each .txt while it still lives in that private loop.',
		],
		refs: [],
	},
	{
		category: 'Pipeline',
		title: 'From curated drafts to Kevin’s AI',
		intro:
			'After review, those .txt files should be curated and sanitized, then ingested into the same document corpus the portfolio RAG stack already searches, so Kevin’s AI can ground answers in them.',
		points: [
			'Keep the hand-off as simple .txt files (or convert to whatever shape ingestion expects) and upload through the existing document pipeline and admin tools.',
			'Apply the same hygiene as the rest of the app: PII redaction, tone, and versioning before anything lands in the corpus.',
			'Re-ingest with clear provenance, then sanity-check retrieval on the new chunks before they power live chat.',
		],
		refs: [
			{
				label: 'Wang et al. (2024): Searching for Best Practices in Retrieval-Augmented Generation',
				href: 'https://arxiv.org/abs/2407.01219',
			},
			{
				label: 'Gao et al. (2024): Retrieval-Augmented Generation for Large Language Models: A Survey',
				href: 'https://arxiv.org/abs/2312.10997',
			},
		],
	},
	{
		category: 'Retrieval',
		title: 'Retrieval pipeline improvements',
		intro:
			'The current RAG stack already embeds documents, stores vectors, and runs similarity search before generation. The next incremental gains typically come from ranking quality and embedding alignment, not from replacing the whole architecture.',
		points: [
			'Add a reranking stage (e.g. monoT5-style cross-encoders) on top of vector search results to reorder chunks by semantic relevance and reduce noise in the context window.',
			'Experiment with domain-adapted or fine-tuned embeddings so user questions match chunk representations more reliably, especially when terminology is specialized.',
		],
		refs: [
			{
				label: 'Wang et al. (2024): Searching for Best Practices in Retrieval-Augmented Generation',
				href: 'https://arxiv.org/abs/2407.01219',
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
		category: 'Retrieval',
		title: 'Adaptive retrieval strategies',
		intro:
			'Today the assistant always retrieves a fixed slice of the corpus. Selective retrieval can cut latency when the model already knows the answer and improve focus when extra evidence is required.',
		points: [
			'Let the system decide when to retrieve, skip retrieval, or retrieve again after a first draft, using patterns inspired by self-reflective RAG that critique their own need for evidence.',
		],
		refs: [
			{
				label: 'Asai et al. (2023): Self-RAG: Learning to Retrieve, Generate, and Critique through Self-Reflection',
				href: 'https://arxiv.org/abs/2310.11511',
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
	{
		category: 'Operations',
		title: 'Continuous model lifecycle management',
		intro:
			'Multiple chat models and prompt versions already coexist. Formalizing evaluation makes upgrades safe instead of anecdotal.',
		points: [
			'Run structured benchmarks (quality, cost, latency, stability) before flipping defaults, reusing the experiments and prompt-version workflows in the admin area.',
			'Version retrieval settings alongside prompts so embeddings, top-k, and model IDs stay in sync across releases.',
			'Track determinism: stabilize retrieval parameters and decoding choices so analytical answers remain auditable over time.',
		],
		refs: [
			{
				label: 'Gao et al. (2024): Retrieval-Augmented Generation for Large Language Models: A Survey',
				href: 'https://arxiv.org/abs/2312.10997',
			},
		],
	},
]

const sectionsNo: SectionCopy[] = [
	{
		category: 'Personlig',
		title: 'Lokal modell og Obsidian-journal',
		intro:
			'Jeg vil bruke en lokal modell med Obsidian-journalen som kontekst, omtrent som en enkel «journal-bot» i vaulten. Målet er å lage .txt-filer om meg selv, uten å legge ut hele journalen rått på nett.',
		points: [
			'Finne en lokal løsning som passer maskinen og personvernbehovet mitt.',
			'Gi modellen kuratert journalinnhold (tema, tidslinjer, valg) så den kan skrive nyttige .txt-utkast.',
			'Lese gjennom og trimme hver .txt mens den fortsatt bare lever i det private løpet mitt.',
		],
		refs: [],
	},
	{
		category: 'Pipeline',
		title: 'Fra kuraterte utkast til Kevin sin AI',
		intro:
			'Etter gjennomgang skal .txt-filene kurateres og saniteres, og deretter inn i dokumentkorpuset som RAG-løsningen allerede henter fra, slik at Kevin sin AI kan bruke dem som kontekst.',
		points: [
			'Beholde overleveringen som enkle .txt-filer (eller konvertere til formatet ingest forventer) og laste opp via dokumentpipelinen og admin som finnes i dag.',
			'Samme hygiene som ellers i appen: PII, tone og versjonering før noe havner i korpuset.',
			'Re-ingest med tydelig proveniens, deretter en rask sjekk av retrieval mot nye chunks før de brukes i live chat.',
		],
		refs: [
			{
				label: 'Wang et al. (2024): Searching for Best Practices in Retrieval-Augmented Generation',
				href: 'https://arxiv.org/abs/2407.01219',
			},
			{
				label: 'Gao et al. (2024): Retrieval-Augmented Generation for Large Language Models: A Survey',
				href: 'https://arxiv.org/abs/2312.10997',
			},
		],
	},
	{
		category: 'Retrieval',
		title: 'Forbedringer i retrieval-pipelinen',
		intro:
			'Den eksisterende RAG-løsningen embedder dokumenter, lagrer vektorer og kjører likhetssøk før generering. Neste steg handler ofte om bedre rangering og bedre innbyrdes samsvar mellom spørsmål og dokumenter – ikke om å bytte ut hele arkitekturen.',
		points: [
			'Legg inn en reranking-fase (f.eks. monoT5-lignende krysskodere) oppå vektorsøk for å sortere treffene semantisk og redusere støy i kontekstvinduet.',
			'Test domenetilpassede eller finjusterte embeddings slik at brukerspørsmål treffer chunk-representasjonene mer stabilt, særlig ved fagterminologi.',
		],
		refs: [
			{
				label: 'Wang et al. (2024): Searching for Best Practices in Retrieval-Augmented Generation',
				href: 'https://arxiv.org/abs/2407.01219',
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
		category: 'Retrieval',
		title: 'Adaptive retrieval-strategier',
		intro:
			'I dag hentes alltid et fast utsnitt av korpuset. Selektiv retrieval kan kutte latency når modellen allerede kan svare, og skjerpe fokus når ekstra evidens trengs.',
		points: [
			'La systemet avgjøre når det skal hente, hoppe over retrieval, eller hente på nytt etter et første utkast, inspirert av selvreflekterende RAG som vurderer eget behov for evidens.',
		],
		refs: [
			{
				label: 'Asai et al. (2023): Self-RAG: Learning to Retrieve, Generate, and Critique through Self-Reflection',
				href: 'https://arxiv.org/abs/2310.11511',
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
	{
		category: 'Drift',
		title: 'Kontinuerlig modell-livssyklus',
		intro:
			'Flere chatmodeller og promptversjoner finnes allerede. Strukturert evaluering gjør oppgraderinger trygge i stedet for anekdotiske.',
		points: [
			'Kjør benchmarks (kvalitet, kost, latency, stabilitet) før standardvalg endres, og gjenbruk eksperiment- og promptversjonsflyten i admin.',
			'Versjoner retrieval-innstillinger sammen med promptene slik at embeddings, top-k og modell-ID-er henger sammen på tvers av releaser.',
			'Følg determinisme: stabiliser retrieval-parametre og dekoding slik at analytiske svar forblir etterprøvbare over tid.',
		],
		refs: [
			{
				label: 'Gao et al. (2024): Retrieval-Augmented Generation for Large Language Models: A Survey',
				href: 'https://arxiv.org/abs/2312.10997',
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
					'Dette er en forskningsforankret roadmap for neste iterasjoner i en portefølje som er under aktiv tilpasning: RAG med vektorlagring, chat, flere modeller, dokument- og chunk-administrasjon, samt promptversjoner.',
				contextBefore:
					'Roadmapen viser hva jeg prioriterer mens jeg kontinuerlig tilpasser porteføljen med læring fra bacheloroppgaven (2026) hos Piscada AS i Trondheim og arbeid på ',
				contextLinkText: 'Foresight AI',
				contextAfter: ', som jeg utvikler sammen med en annen i teamet.',
				contextLinkHref: foresightAiProductUrl,
			}
		: {
				title: 'Future work and improvements',
				lead:
					'A research-backed roadmap for the next iterations of a portfolio that is being actively adapted: RAG with a vector store, chat, multiple models, document and chunk administration, and prompt versioning.',
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
	<main class="min-h-screen pt-20 bg-gradient-to-br from-slate-50 to-slate-100 relative">
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
