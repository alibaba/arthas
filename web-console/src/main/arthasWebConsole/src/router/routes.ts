import { RouteRecordRaw } from "vue-router"

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import("@/views/Index.vue"),
  },
  {
    path: '/about',
    component: () => import("@/components/HelloWorld.vue")
  }
]
export default routes