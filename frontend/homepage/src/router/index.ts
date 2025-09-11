import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ChatView from '../views/ChatView.vue'

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
  ],
})

export default router
