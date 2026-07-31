import { createRouter, createWebHistory } from 'vue-router'
import RecallDashboard from '@/pages/RecallDashboard.vue'

const routes = [
  {
    path: '/',
    name: 'dashboard',
    component: RecallDashboard
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
