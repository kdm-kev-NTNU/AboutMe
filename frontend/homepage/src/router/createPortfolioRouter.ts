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
				path: '/projects',
				name: 'projects',
				component: () => import('../views/ProjectsView.vue'),
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
				path: '/bachelor',
				name: 'bachelor',
				component: () => import('../views/BachelorView.vue'),
			},
			{
				path: '/tech-stack',
				name: 'tech-stack',
				component: () => import('../views/TechStackView.vue'),
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
				name: 'future-work',
				component: () => import('../views/FutureWorkView.vue'),
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
