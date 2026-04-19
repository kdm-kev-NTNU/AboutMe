import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ChatView from '../views/ChatView.vue'
import ChatHistory from '../views/ChatHistory.vue'
import { registerAdminRouteGuard } from './guards'

// Public routes load eagerly; content-heavy portfolio pages use lazy imports to split JS bundles.
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
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
      // route level code-splitting
      // this generates a separate chunk (Projects.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/ProjectsView.vue'),
    },
    {
      path: '/work-experience',
      name: 'work-experience',
      // route level code-splitting
      // this generates a separate chunk (WorkExperience.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/WorkExperienceView.vue'),
    },
    {
      path: '/education',
      name: 'education',
      // route level code-splitting
      // this generates a separate chunk (Education.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/EducationView.vue'),
    },
    {
      path: '/tech-stack',
      name: 'tech-stack',
      component: () => import('../views/TechStackView.vue'),
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
    // Admin area: meta.requiresAdmin is enforced in ./guards (HTTP Basic session checked via Pinia auth store).
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
  ],
})

registerAdminRouteGuard(router)

export default router
