<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
	ArrowLeft,
	BookOpen,
	Globe2,
	ImageIcon,
	Sparkles,
	TrendingUp,
	Users,
} from 'lucide-vue-next'
import { useLangStore } from '@/stores/lang'
import { Button } from '@/components/ui/button'
import {
	Dialog,
	DialogContent,
	DialogHeader,
	DialogTitle,
} from '@/components/ui/dialog'

const langStore = useLangStore()
const isNo = computed(() => langStore.language === 'no')

const base = () => `${import.meta.env.BASE_URL}images/heathenarmy/`

type GalleryItem = {
	src: string
	altEn: string
	altNo: string
	titleEn: string
	titleNo: string
	captionEn: string
	captionNo: string
}

const gallery = computed<GalleryItem[]>(() => [
	{
		src: `${base()}patreon.png`,
		altEn: 'Tweet promoting Patreon support for the Vikings blog',
		altNo: 'Tweet som oppfordrer til Patreon-støtte for Vikings-bloggen',
		titleEn: 'Community support',
		titleNo: 'Støtte fra fellesskapet',
		captionEn: 'Invited readers to Patreon to help keep long-form Vikings coverage sustainable.',
		captionNo:
			'Oppmuntret lesere til Patreon for å holde liv i lengre Vikings-innhold over tid.',
	},
	{
		src: `${base()}analytics-users.png`,
		altEn: 'Google Analytics chart showing users over time',
		altNo: 'Google Analytics-graf som viser brukere over tid',
		titleEn: 'Audience growth',
		titleNo: 'Vekst i publikum',
		captionEn: 'Roughly 977 users and 1.1K sessions over the tracked period — proof the niche had traction.',
		captionNo:
			'Omtrent 977 brukere og 1,1K økter i perioden — et tydelig tegn på at nisjen traff.',
	},
	{
		src: `${base()}homepage.png`,
		altEn: 'Heathen Army blog homepage on a monitor',
		altNo: 'Heathen Army-bloggens forsiden på en skjerm',
		titleEn: 'Site experience',
		titleNo: 'Opplevelsen på siden',
		captionEn: 'Dark, editorial layout focused on episode write-ups and discovery.',
		captionNo:
			'Mørkt, redaksjonelt uttrykk med fokus på episodeskildringer og utforsking.',
	},
	{
		src: `${base()}seo-yoast.png`,
		altEn: 'Yoast SEO dashboard showing post scores',
		altNo: 'Yoast SEO-dashbord som viser poengsum for innlegg',
		titleEn: 'SEO hygiene',
		titleNo: 'SEO-rutiner',
		captionEn: 'Yoast helped keep titles, snippets, and readability on track — many posts reached “good” SEO scores.',
		captionNo:
			'Yoast holdt titler, utdrag og lesbarhet i sjakk — mange innlegg fikk «god» SEO-score.',
	},
	{
		src: `${base()}analytics-map.png`,
		altEn: 'Google Analytics map of sessions by geography',
		altNo: 'Google Analytics-kart med økter fordelt geografisk',
		titleEn: 'Global reach',
		titleNo: 'Global rekkevidde',
		captionEn: 'Traffic skewed heavily toward North America and Europe, with readers spread worldwide.',
		captionNo:
			'Trafikk dominerte i Nord-Amerika og Europa, med lesere spredt globalt.',
	},
])

const copy = computed(() =>
	isNo.value
		? {
				back: 'Tilbake til prosjekter',
				heroBadge: '12–14 år · Innhold & fellesskap',
				title: 'Heathen Army',
				subtitle:
					'Blogg og fanside for TV-serien Vikings — publisering, SEO og analyse før dagens AI-verktøy fanget opp.',
				statsTitle: 'Noen tall fra perioden',
				statUsers: 'Brukere',
				statSessions: 'Økter',
				statSeo: 'Innlegg med «god» SEO-score',
				statReach: 'Geografisk spredning',
				statUsersVal: '977',
				statSessionsVal: '1,1k',
				statSeoVal: '30',
				statReachVal: 'Globalt',
				storyTitle: 'Historien',
				storyLead:
					'Da jeg var 12–14 år laget og drev jeg en blogg og fanside for Vikings. Det som begynte som et personlig prosjekt ble en målrettet innsats for å bygge et ekte fellesskap rundt innhold av høy kvalitet.',
				storyP2:
					'Den største utfordringen var å holde momentum gjennom jevnlig innhold. Før AI-verktøy fantes måtte jeg researche, skrive og publisere alt selv — samtidig som jeg holdt en plan som holdt publikum engasjert. Motivasjonen kom av konkrete signaler: meldinger fra følgere, analytics som visste vekst, og et publikum på tvers av kontinenter.',
				storyP3:
					'Det som overrasket meg mest var hvor mye enkle ting ville blitt med en partner. Å drive alt alene — research, tekst, design, sosiale medier og analyse — ga få muligheter til å avdekke blindsoner. Samarbeid multipliserer effekt på måter solo-innsats ikke gjør alene.',
				storyP4:
					'Dypere forsto jeg dynamikken i fellesskap: folk konsumerte ikke bare innhold — de delte, diskuterte og ble investert. Patreon ble et forsøk på å gjøre prosjektet mer bærekraftig; tilkoblingen til leserne ble viktigere enn selve bloggen som medium.',
				storyP5:
					'Til slutt skiftet interessene mine. Innholdsproduksjon hadde lært meg mye om konsistens og fellesskap, men jeg ble mer tent på programvareutvikling. Lærdommen om utholdenhet og at passion kan bygge noe ekte tar jeg med videre.',
				learnTitle: 'Dette tok jeg med meg',
				learnItems: [
					'Bygge og holde liv i et publikum i en nisje — kvalitet + strategi gir tyngde.',
					'Operasjonell rutine: publiseringskalender, Yoast SEO og lesbarhet på WordPress.',
					'Beslutninger styrt av data: Google Analytics som tilbakemeldingssløyfe.',
					'Monetisering og tillit: Patreon og åpen kommunikasjon med lesere.',
					'Solo vs team: bredde i alt fra tekst til analyse er krevende uten partner.',
				],
				stackTitle: 'Verktøy og plattformer',
				stackBody:
					'WordPress som CMS, Yoast SEO for kvalitetssjekk av innlegg, Google Analytics for adferd og geografisk innsikt, Patreon for støtte, og sosiale kanaler (blant annet Twitter/X) for distribusjon og samtale.',
				galleryTitle: 'Skjermbilder',
				galleryLead:
					'Utvalgte bilder fra bloggen, sosiale medier og analyse — historiske snapshots fra prosjektperioden.',
				dialogHint: 'Trykk på et bilde for å forstørre.',
				openGallery: 'Åpne bilde',
			}
		: {
				back: 'Back to projects',
				heroBadge: 'Ages 12–14 · Content & community',
				title: 'Heathen Army',
				subtitle:
					'A Vikings TV series fan blog — publishing, SEO, and analytics long before today’s AI-assisted workflows.',
				statsTitle: 'Signals from the era',
				statUsers: 'Users',
				statSessions: 'Sessions',
				statSeo: 'Posts rated “good” (Yoast)',
				statReach: 'Geographic reach',
				statUsersVal: '977',
				statSessionsVal: '1.1k',
				statSeoVal: '30',
				statReachVal: 'Global',
				storyTitle: 'The story',
				storyLead:
					'Between ages 12 and 14 I built and ran a blog and fan page dedicated to the Vikings TV series. What started as a personal project became a relentless pursuit to build a genuine community around quality content.',
				storyP2:
					'The core challenge was sustaining momentum through consistent publishing. Before AI tooling existed I researched, wrote, and shipped everything myself while keeping a cadence that kept readers engaged. Tangible proof — follower messages, climbing analytics, and a global audience — became the fuel.',
				storyP3:
					'What surprised me most was how much easier the work would have been with a partner. Solo execution meant covering research, writing, design, social media, and analytics without another perspective to remove blind spots.',
				storyP4:
					'The deeper lesson was community dynamics: people didn’t only consume — they shared, debated, and invested emotionally. Patreon was an attempt to make the project sustainable; the connection mattered more than the blog as a medium.',
				storyP5:
					'Eventually my interests shifted toward software development, but the lesson stuck — passion and persistence can build real things.',
				learnTitle: 'What stuck with me',
				learnItems: [
					'Growing a niche audience through quality plus deliberate distribution.',
					'Operational hygiene: Yoast SEO checks and readable WordPress posts.',
					'Closing the loop with Google Analytics on behavior and geography.',
					'Monetization and trust via Patreon and transparent reader communication.',
					'Solo breadth vs collaboration: covering everything alone has real limits.',
				],
				stackTitle: 'Tools & platforms',
				stackBody:
					'WordPress as the CMS, Yoast SEO for on-page quality, Google Analytics for behavioral insight, Patreon for supporter-backed sustainability, and social channels (including Twitter/X) for distribution and conversation.',
				galleryTitle: 'Screenshots',
				galleryLead:
					'A handful of historical snapshots from the blog, social posts, and analytics dashboards.',
				dialogHint: 'Click an image to enlarge.',
				openGallery: 'Open image',
			},
)

const lightboxOpen = ref(false)
const activeImage = ref<GalleryItem | null>(null)

function openLightbox(item: GalleryItem) {
	activeImage.value = item
	lightboxOpen.value = true
}

function altFor(item: GalleryItem) {
	return isNo.value ? item.altNo : item.altEn
}

function titleFor(item: GalleryItem) {
	return isNo.value ? item.titleNo : item.titleEn
}

function captionFor(item: GalleryItem) {
	return isNo.value ? item.captionNo : item.captionEn
}
</script>

<template>
	<main
		class="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 pb-20 pt-20"
		aria-labelledby="heathen-army-title"
	>
		<div class="pointer-events-none absolute inset-0">
			<div
				class="absolute inset-0"
				style="
					background: radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.08) 0%, transparent 50%),
						radial-gradient(circle at 80% 20%, rgba(37, 99, 235, 0.08) 0%, transparent 50%),
						radial-gradient(circle at 50% 50%, rgba(96, 165, 250, 0.05) 0%, transparent 70%);
				"
			/>
		</div>

		<div class="relative z-10 mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
			<div class="mb-8">
				<Button variant="outline" size="sm" as-child class="gap-2 border-slate-300 bg-white/80">
					<RouterLink to="/reason#projects">
						<ArrowLeft class="h-4 w-4" aria-hidden="true" />
						{{ copy.back }}
					</RouterLink>
				</Button>
			</div>

			<header
				class="mb-12 rounded-2xl border border-slate-200/80 bg-white/90 p-8 shadow-lg shadow-slate-900/5 backdrop-blur-sm sm:p-10"
			>
				<p
					class="mb-3 inline-flex items-center gap-2 rounded-full bg-blue-50 px-3 py-1 text-sm font-medium text-blue-800"
				>
					<BookOpen class="h-4 w-4 shrink-0" aria-hidden="true" />
					{{ copy.heroBadge }}
				</p>
				<h1
					id="heathen-army-title"
					class="mb-4 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl"
				>
					<span
						class="bg-gradient-to-r from-blue-600 via-blue-700 to-blue-800 bg-clip-text text-transparent"
					>
						{{ copy.title }}
					</span>
				</h1>
				<p class="max-w-3xl text-lg leading-relaxed text-slate-600">
					{{ copy.subtitle }}
				</p>
			</header>

			<section
				class="mb-12 rounded-2xl border border-slate-200/80 bg-white/90 p-6 shadow-md backdrop-blur-sm sm:p-8"
				aria-labelledby="heathen-stats-heading"
			>
				<h2 id="heathen-stats-heading" class="mb-6 flex items-center gap-2 text-xl font-semibold text-slate-900">
					<TrendingUp class="h-5 w-5 text-blue-600" aria-hidden="true" />
					{{ copy.statsTitle }}
				</h2>
				<div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
					<div class="rounded-xl border border-slate-200 bg-gradient-to-br from-white to-slate-50 p-5 shadow-sm">
						<div class="mb-2 flex items-center gap-2 text-slate-500">
							<Users class="h-4 w-4" aria-hidden="true" />
							<span class="text-xs font-medium uppercase tracking-wide">{{ copy.statUsers }}</span>
						</div>
						<p class="text-2xl font-bold text-slate-900">{{ copy.statUsersVal }}</p>
					</div>
					<div class="rounded-xl border border-slate-200 bg-gradient-to-br from-white to-slate-50 p-5 shadow-sm">
						<div class="mb-2 flex items-center gap-2 text-slate-500">
							<Sparkles class="h-4 w-4" aria-hidden="true" />
							<span class="text-xs font-medium uppercase tracking-wide">{{ copy.statSessions }}</span>
						</div>
						<p class="text-2xl font-bold text-slate-900">{{ copy.statSessionsVal }}</p>
					</div>
					<div class="rounded-xl border border-slate-200 bg-gradient-to-br from-white to-slate-50 p-5 shadow-sm">
						<div class="mb-2 flex items-center gap-2 text-slate-500">
							<BookOpen class="h-4 w-4" aria-hidden="true" />
							<span class="text-xs font-medium uppercase tracking-wide">{{ copy.statSeo }}</span>
						</div>
						<p class="text-2xl font-bold text-slate-900">{{ copy.statSeoVal }}</p>
					</div>
					<div class="rounded-xl border border-slate-200 bg-gradient-to-br from-white to-slate-50 p-5 shadow-sm">
						<div class="mb-2 flex items-center gap-2 text-slate-500">
							<Globe2 class="h-4 w-4" aria-hidden="true" />
							<span class="text-xs font-medium uppercase tracking-wide">{{ copy.statReach }}</span>
						</div>
						<p class="text-2xl font-bold text-slate-900">{{ copy.statReachVal }}</p>
					</div>
				</div>
			</section>

			<section
				class="mb-12 rounded-2xl border border-slate-200/80 bg-white/90 p-6 shadow-md backdrop-blur-sm sm:p-8"
				aria-labelledby="heathen-story-heading"
			>
				<h2 id="heathen-story-heading" class="mb-4 text-xl font-semibold text-slate-900">
					{{ copy.storyTitle }}
				</h2>
				<div class="space-y-4 text-slate-700 leading-relaxed">
					<p>{{ copy.storyLead }}</p>
					<p>{{ copy.storyP2 }}</p>
					<p>{{ copy.storyP3 }}</p>
					<p>{{ copy.storyP4 }}</p>
					<p>{{ copy.storyP5 }}</p>
				</div>
			</section>

			<section
				class="mb-12 rounded-2xl border border-slate-200/80 bg-white/90 p-6 shadow-md backdrop-blur-sm sm:p-8"
				aria-labelledby="heathen-learn-heading"
			>
				<h2 id="heathen-learn-heading" class="mb-4 text-xl font-semibold text-slate-900">
					{{ copy.learnTitle }}
				</h2>
				<ul class="list-inside list-disc space-y-2 text-slate-700 marker:text-blue-600">
					<li v-for="(item, idx) in copy.learnItems" :key="idx">{{ item }}</li>
				</ul>
			</section>

			<section
				class="mb-12 rounded-2xl border border-slate-200/80 bg-white/90 p-6 shadow-md backdrop-blur-sm sm:p-8"
				aria-labelledby="heathen-stack-heading"
			>
				<h2 id="heathen-stack-heading" class="mb-3 text-xl font-semibold text-slate-900">
					{{ copy.stackTitle }}
				</h2>
				<p class="text-slate-700 leading-relaxed">{{ copy.stackBody }}</p>
			</section>

			<section class="rounded-2xl border border-slate-200/80 bg-white/90 p-6 shadow-md backdrop-blur-sm sm:p-8" aria-labelledby="heathen-gallery-heading">
				<h2 id="heathen-gallery-heading" class="mb-2 flex items-center gap-2 text-xl font-semibold text-slate-900">
					<ImageIcon class="h-5 w-5 text-blue-600" aria-hidden="true" />
					{{ copy.galleryTitle }}
				</h2>
				<p class="mb-6 text-sm text-slate-600">{{ copy.galleryLead }}</p>
				<p class="mb-6 text-xs text-slate-500">{{ copy.dialogHint }}</p>

				<div class="grid gap-6 sm:grid-cols-2">
					<figure
						v-for="item in gallery"
						:key="item.src"
						class="overflow-hidden rounded-xl border border-slate-200 bg-slate-50/80 shadow-sm transition hover:border-blue-200 hover:shadow-md"
					>
						<button
							type="button"
							class="group block w-full text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2"
							@click="openLightbox(item)"
						>
							<img
								:src="item.src"
								:alt="altFor(item)"
								class="h-48 w-full object-cover object-top transition duration-300 group-hover:scale-[1.02] sm:h-56"
								loading="lazy"
								decoding="async"
							/>
						</button>
						<figcaption class="space-y-1 p-4">
							<span class="text-sm font-semibold text-slate-900">{{ titleFor(item) }}</span>
							<span class="block text-xs text-slate-600">{{ captionFor(item) }}</span>
							<button
								type="button"
								class="mt-2 inline-block text-left text-xs font-medium text-blue-700 underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2"
								@click="openLightbox(item)"
							>
								{{ copy.openGallery }}
							</button>
						</figcaption>
					</figure>
				</div>
			</section>
		</div>

		<Dialog v-model:open="lightboxOpen">
			<DialogContent class="max-w-4xl border-slate-200 bg-white sm:max-w-5xl">
				<DialogHeader>
					<DialogTitle v-if="activeImage">{{ titleFor(activeImage) }}</DialogTitle>
				</DialogHeader>
				<div v-if="activeImage" class="space-y-3">
					<img
						:src="activeImage.src"
						:alt="altFor(activeImage)"
						class="max-h-[70vh] w-full rounded-lg object-contain bg-slate-950/5"
					/>
					<p class="text-sm text-slate-600">{{ captionFor(activeImage) }}</p>
				</div>
			</DialogContent>
		</Dialog>
	</main>
</template>
