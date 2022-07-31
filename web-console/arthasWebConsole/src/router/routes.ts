import { RouteRecordRaw } from "vue-router"

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect:'/config'
  },
  {
    path: '/config',
    component: () => import("@/views/Config.vue"),
  },
  {
    path: '/console',
    component: () => import("@/views/Console.vue")
  },
  // {
  //   path: '/terminal',
  //   component: () => import("@/views/Terminal.vue")
  // },

]
export default routes