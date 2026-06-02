import { createRouter, createWebHistory, createMemoryHistory } from 'vue-router'
import type { Router } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ChatView from '../views/ChatView.vue'
import { registerAdminRouteGuard } from './guards'

export type PortfolioRouterOptions = {
	/** Use in unit tests; `createWebHistory` can hang under jsdom. */
	useMemoryHistory?: boolean
}

// Public routes load eagerly; content-heavy portfolio pages use lazy imports to split JS bundles.
export function createPortfolioRouter(opts?: PortfolioRouterOptions): Router {
	const history =
		opts?.useMemoryHistory === true
			? createMemoryHistory(import.meta.env.BASE_URL)
			: createWebHistory(import.meta.env.BASE_URL)

	const router = createRouter({
		history,
		routes: [
			{
				path: '/',
				name: 'home',
				component: HomeView,
			},
			{
				path: '/chat',
				name: 'chat',
				component: ChatView,
			},
			{
				path: '/voice',
				name: 'voice',
				component: () => import('../views/VoiceView.vue'),
			},
			{
				path: '/projects',
				redirect: { path: '/reason', hash: '#projects' },
			},
			{
				path: '/projects/heathen-army',
				name: 'project-heathen-army',
				component: () => import('../views/HeathenArmyView.vue'),
			},
			{
				path: '/career',
				redirect: '/reason',
			},
			{
				path: '/reason',
				name: 'reason',
				component: () => import('../views/ReasonView.vue'),
			},
			{
				path: '/work-experience',
				redirect: '/reason',
			},
			{
				path: '/education',
				redirect: '/reason',
			},
			{
				path: '/how',
				name: 'how',
				component: () => import('../views/HowView.vue'),
			},
			{
				path: '/project',
				redirect: { path: '/reason', hash: '#projects' },
			},
			{
				path: '/bachelor',
				redirect: { path: '/how', hash: '#bachelor' },
			},
			{
				path: '/tech-stack',
				redirect: '/how',
			},
			{
				path: '/feedback',
				name: 'feedback',
				component: () => import('../views/FeedbackView.vue'),
			},
			{
				path: '/privacy-policy',
				name: 'privacy-policy',
				component: () => import('../views/PrivacyPolicyView.vue'),
			},
			{
				path: '/accessibility',
				name: 'accessibility',
				component: () => import('../views/AccessibilityView.vue'),
			},
			{
				path: '/future-work',
				redirect: { path: '/how', hash: '#future-work' },
			},
			{
				path: '/admin/tools',
				name: 'admin-tools',
				meta: { requiresAdmin: true },
				component: () => import('../views/AdminToolsView.vue'),
			},
			{
				path: '/admin/pipeline',
				name: 'admin-pipeline',
				meta: { requiresAdmin: true },
				component: () => import('../views/AdminPipelineView.vue'),
			},
			{
				path: '/admin/chunks',
				name: 'admin-chunks',
				meta: { requiresAdmin: true },
				component: () => import('../views/AdminChunksView.vue'),
			},
			{
				path: '/admin/question-suggestions',
				name: 'admin-question-suggestions',
				meta: { requiresAdmin: true },
				component: () => import('../views/AdminQuestionSuggestionsView.vue'),
			},
			{
				path: '/admin/prompts',
				name: 'admin-prompts',
				meta: { requiresAdmin: true },
				component: () => import('../views/AdminPromptsView.vue'),
			},
			{
				path: '/admin/experiments',
				name: 'admin-experiments',
				meta: { requiresAdmin: true },
				component: () => import('../views/AdminExperimentsView.vue'),
			},
		],
	})

	registerAdminRouteGuard(router)
	return router
}
