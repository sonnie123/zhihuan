import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { title: '首页' } },
      { path: 'product/:id', name: 'product', component: () => import('@/views/ProductView.vue'), meta: { title: '商品详情' } },
      { path: 'publish', name: 'publish', component: () => import('@/views/PublishView.vue'), meta: { title: 'AI 一键发布' } },
      { path: 'user', name: 'user', component: () => import('@/views/user/UserView.vue'), meta: { title: '个人中心' } },
    ]
  },
  { path: '/im', component: () => import('@/layouts/IMLayout.vue'),
    children: [
      { path: '', name: 'im', component: () => import('@/views/im/IMView.vue') },
      { path: 'chat/:id', name: 'im-chat', component: () => import('@/views/im/IMView.vue'), meta: { chat: true } },
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/misc/NotFoundView.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() { return { top: 0 } }
})

router.afterEach((to) => {
  if (to.meta?.title) document.title = (to.meta.title as string) + ' · 智换'
})

export default router
