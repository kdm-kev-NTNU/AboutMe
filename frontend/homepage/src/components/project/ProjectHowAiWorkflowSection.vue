<script setup lang="ts">
import { computed } from 'vue'
import { useLangStore } from '@/stores/lang'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Bot, Sparkles, Link2 } from 'lucide-vue-next'

const langStore = useLangStore()
const isNo = computed(() => langStore.language === 'no')

const YOUTUBE_EMBED_ID = 'C87ITeVS9hs'
const YOUTUBE_WATCH_URL = `https://www.youtube.com/watch?v=${YOUTUBE_EMBED_ID}`

const hero = computed(() =>
	isNo.value
		? {
				title: 'Slik bruker jeg AI uten å slippe ansvaret',
				lead:
					'Målet er ikke å la AI tenke for meg, men å bruke den som støtte mens jeg beholder ansvar for arkitektur, beslutninger og kvalitet.',
			}
		: {
				title: 'How I use AI without outsourcing the thinking',
				lead:
					'The goal is not to let AI think for me, but to use it as support while I stay responsible for architecture, decisions, and quality.',
			},
)

const videoSection = computed(() =>
	isNo.value
		? {
				heading: 'Fra idé til kode med struktur',
				description:
					'Videoen viser en strukturert utviklingsflyt: problem og brukere først, deretter use case og domenemodell, BDD med Gherkin og TDD — og hvordan AI-verktøy som Claude, Cursor og Codex kobles inn via MCP, skills og subagents uten at design og kvalitet settes bort.',
				badge: 'Metode',
				iframeTitle: 'YouTube-video om AI-støttet utviklingsflyt',
				watchLabel: 'Åpne på YouTube (ny fane)',
			}
		: {
				heading: 'From idea to code with structure',
				description:
					'The video walks through a structured development workflow: problem and users first, then use cases and domain modeling, BDD with Gherkin and TDD — and how AI tools such as Claude, Cursor, and Codex connect through MCP, skills, and subagents without handing off design or quality.',
				badge: 'Method',
				iframeTitle: 'YouTube video about AI-supported development workflow',
				watchLabel: 'Watch on YouTube (opens in a new tab)',
			},
)
</script>

<template>
	<div class="relative pb-8">
		<div class="relative z-10 mx-auto max-w-4xl px-4 py-4 sm:px-6 lg:px-8">
			<div
				v-motion
				:initial="{ opacity: 0, y: 24 }"
				:visible-once="{ opacity: 1, y: 0, transition: { duration: 550 } }"
				class="text-center mb-10"
			>
				<div
					class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-gradient-to-br from-blue-600 to-indigo-700 text-white shadow-lg shadow-blue-500/25 mb-5"
				>
					<Bot class="w-8 h-8" aria-hidden="true" />
				</div>
				<h2
					class="text-3xl sm:text-4xl font-bold mb-3 bg-gradient-to-r from-blue-600 via-blue-700 to-blue-800 bg-clip-text text-transparent"
				>
					{{ hero.title }}
				</h2>
				<p class="text-gray-600 max-w-3xl mx-auto leading-relaxed text-sm sm:text-base">
					{{ hero.lead }}
				</p>
			</div>

			<div
				v-motion
				:initial="{ opacity: 0, y: 20 }"
				:visible-once="{ opacity: 1, y: 0, transition: { duration: 550, delay: 120 } }"
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
		</div>
	</div>
</template>
