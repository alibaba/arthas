import { RouteRecordRaw } from "vue-router"

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import("@/views/Index.vue"),
  },
  {
    path: '/console',
    component: () => import("@/views/Console.vue")
  },
  {
    path: '/terminal',
    component: () => import("@/views/Terminal.vue")
  },
]
export default routes