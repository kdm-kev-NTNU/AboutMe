import { createRouter, createWebHistory, createMemoryHistory } from 'vue-router'
import type { Router } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ChatView from '../views/ChatView.vue'
import ChatHistory from '../views/ChatHistory.vue'
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
				name: 'projects',
				component: () => import('../views/ProjectsView.vue'),
			},
			{
				path: '/projects/heathen-army',
				name: 'project-heathen-army',
				component: () => import('../views/HeathenArmyView.vue'),
			},
			{
				path: '/career',
				name: 'career',
				component: () => import('../views/CareerView.vue'),
			},
			{
				path: '/work-experience',
				redirect: '/career',
			},
			{
				path: '/education',
				redirect: '/career',
			},
			{
				path: '/project',
				name: 'project',
				component: () => import('../views/ProjectPageView.vue'),
			},
			{
				path: '/bachelor',
				redirect: { path: '/project', hash: '#bachelor' },
			},
			{
				path: '/tech-stack',
				redirect: { path: '/project', hash: '#tech-stack' },
			},
			{
				path: '/feedback',
				name: 'feedback',
				component: () => import('../views/FeedbackView.vue'),
			},
			{
				path: '/chat-history',
				name: 'chat-history',
				component: ChatHistory,
			},
			{
				path: '/privacy-policy',
				name: 'privacy-policy',
				component: () => import('../views/PrivacyPolicyView.vue'),
			},
			{
				path: '/future-work',
				redirect: { path: '/project', hash: '#future-work' },
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
