<script setup lang="ts">
import { computed, nextTick, provide, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ChevronDown, FolderKanban } from 'lucide-vue-next'
import { useLangStore } from '@/stores/lang'
import ProjectTechStackSection from '@/components/project/ProjectTechStackSection.vue'
import ProjectBachelorSection from '@/components/project/ProjectBachelorSection.vue'
import ProjectFutureWorkSection from '@/components/project/ProjectFutureWorkSection.vue'

const langStore = useLangStore()
const route = useRoute()
const isNo = computed(() => langStore.language === 'no')

const hero = computed(() =>
	isNo.value
		? {
				title: 'Prosjektet',
				lead:
					'Bacheloroppgaven (2026), teknologistakk og videre arbeid samlet på én side. Utvid seksjonene under for detaljer.',
			}
		: {
				title: 'The project',
				lead:
					"Bachelor's thesis (2026), tech stack, and future work in one place. Expand the sections below for details.",
			},
)

const labels = computed(() =>
	isNo.value
		? {
				tech: 'Teknologistakk',
				bachelor: 'Bacheloroppgaven',
				future: 'Videre arbeid',
			}
		: {
				tech: 'Tech stack',
				bachelor: "Bachelor's thesis",
				future: 'Future work',
			},
)

const techOpen = ref(false)
const bachelorOpen = ref(false)
const futureOpen = ref(false)

function scrollToSection(elementId: string) {
	nextTick(() => {
		document.getElementById(elementId)?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
	})
}

function toggleTech() {
	techOpen.value = !techOpen.value
	if (techOpen.value) scrollToSection('accordion-tech-stack')
}
function toggleBachelor() {
	bachelorOpen.value = !bachelorOpen.value
	if (bachelorOpen.value) scrollToSection('accordion-bachelor')
}
function toggleFuture() {
	futureOpen.value = !futureOpen.value
	if (futureOpen.value) scrollToSection('accordion-future-work')
}

function openTech() {
	techOpen.value = true
	scrollToSection('accordion-tech-stack')
}
function openBachelor() {
	bachelorOpen.value = true
	scrollToSection('accordion-bachelor')
}
function openFutureWork() {
	futureOpen.value = true
	scrollToSection('accordion-future-work')
}

provide('projectSectionNav', {
	openBachelor: openBachelor,
})

function applyRouteHash(hash: string | undefined) {
	if (!hash) return
	const h = hash.replace(/^#/, '').toLowerCase()
	if (h === 'tech-stack') openTech()
	else if (h === 'bachelor') openBachelor()
	else if (h === 'future-work') openFutureWork()
}

watch(
	() => route.hash,
	(h) => {
		applyRouteHash(h)
	},
	{ immediate: true },
)
</script>

<template>
	<main
		class="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 relative pb-20 pt-20"
		aria-labelledby="project-page-title"
	>
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

		<div class="relative z-10 mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
			<div
				v-motion
				:initial="{ opacity: 0, y: 24 }"
				:visible-once="{ opacity: 1, y: 0, transition: { duration: 550 } }"
				class="mb-10 text-center"
			>
				<div
					class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-gradient-to-br from-blue-600 to-indigo-700 text-white shadow-lg shadow-blue-500/25 mb-5"
				>
					<FolderKanban class="w-8 h-8" aria-hidden="true" />
				</div>
				<h1
					id="project-page-title"
					class="text-3xl sm:text-4xl font-bold mb-3 bg-gradient-to-r from-blue-600 via-blue-700 to-blue-800 bg-clip-text text-transparent"
				>
					{{ hero.title }}
				</h1>
				<p class="text-gray-600 max-w-3xl mx-auto leading-relaxed text-sm sm:text-base">
					{{ hero.lead }}
				</p>
			</div>

			<div class="space-y-4">
				<!-- Tech stack -->
				<section
					id="accordion-tech-stack"
					class="rounded-2xl border border-slate-200/80 bg-white/90 shadow-lg shadow-slate-900/5 backdrop-blur-sm overflow-hidden"
				>
					<button
						type="button"
						class="flex w-full items-center justify-between gap-3 px-5 py-4 text-left transition hover:bg-slate-50/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-blue-600"
						:aria-expanded="techOpen"
						aria-controls="panel-tech-stack"
						id="section-tech-stack"
						@click="toggleTech"
					>
						<span class="text-lg font-semibold text-slate-900">{{ labels.tech }}</span>
						<ChevronDown
							class="h-5 w-5 shrink-0 text-slate-500 transition-transform duration-200"
							:class="{ 'rotate-180': techOpen }"
							aria-hidden="true"
						/>
					</button>
					<div
						v-show="techOpen"
						id="panel-tech-stack"
						role="region"
						:aria-labelledby="'section-tech-stack'"
						class="border-t border-slate-200/80"
					>
						<ProjectTechStackSection />
					</div>
				</section>

				<!-- Bachelor -->
				<section
					id="accordion-bachelor"
					class="rounded-2xl border border-slate-200/80 bg-white/90 shadow-lg shadow-slate-900/5 backdrop-blur-sm overflow-hidden"
				>
					<button
						type="button"
						class="flex w-full items-center justify-between gap-3 px-5 py-4 text-left transition hover:bg-slate-50/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-blue-600"
						:aria-expanded="bachelorOpen"
						aria-controls="panel-bachelor"
						id="section-bachelor"
						@click="toggleBachelor"
					>
						<span class="text-lg font-semibold text-slate-900">{{ labels.bachelor }}</span>
						<ChevronDown
							class="h-5 w-5 shrink-0 text-slate-500 transition-transform duration-200"
							:class="{ 'rotate-180': bachelorOpen }"
							aria-hidden="true"
						/>
					</button>
					<div
						v-show="bachelorOpen"
						id="panel-bachelor"
						role="region"
						:aria-labelledby="'section-bachelor'"
						class="border-t border-slate-200/80"
					>
						<ProjectBachelorSection />
					</div>
				</section>

				<!-- Future work -->
				<section
					id="accordion-future-work"
					class="rounded-2xl border border-slate-200/80 bg-white/90 shadow-lg shadow-slate-900/5 backdrop-blur-sm overflow-hidden"
				>
					<button
						type="button"
						class="flex w-full items-center justify-between gap-3 px-5 py-4 text-left transition hover:bg-slate-50/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-blue-600"
						:aria-expanded="futureOpen"
						aria-controls="panel-future-work"
						id="section-future-work"
						@click="toggleFuture"
					>
						<span class="text-lg font-semibold text-slate-900">{{ labels.future }}</span>
						<ChevronDown
							class="h-5 w-5 shrink-0 text-slate-500 transition-transform duration-200"
							:class="{ 'rotate-180': futureOpen }"
							aria-hidden="true"
						/>
					</button>
					<div
						v-show="futureOpen"
						id="panel-future-work"
						role="region"
						:aria-labelledby="'section-future-work'"
						class="border-t border-slate-200/80"
					>
						<ProjectFutureWorkSection />
					</div>
				</section>
			</div>
		</div>
	</main>
</template>
