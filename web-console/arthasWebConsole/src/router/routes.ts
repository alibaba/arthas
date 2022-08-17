import { RouteRecordRaw } from "vue-router";

const routes: RouteRecordRaw[] = [
  {
    path: "/",
    redirect: "/dashboard",
  },
  {
    path: "/config",
    component: () => import("@/views/Config.vue"),
  },
  {
    path: "/console",
    component: () => import("@/views/Console.vue"),
  },
  {
    path:"/dashboard",
    component:()=>import("@/views/DashBoard.vue")
  },
  {
    path: "/synchronize",
    component: () => import("@/views/Synchronize.vue"),
    children: [
      {
        path: "",
        redirect: "/synchronize/jvm",
      },
      {
        path: "thread",
        component: () => import("@/views/sync/Thread.vue"),
      },{
        path:"jvm",
        component: ()=> import("@/views/sync/Jvm.vue")
      },
      {
        path:"memory",
        component: ()=> import("@/views/sync/Memory.vue")
      },
      {
        path:"perfcounter",
        component: ()=> import("@/views/sync/Perfcounter.vue")
      },      {
        path:"jad",
        component: ()=> import("@/views/sync/Jad.vue")
      },{
        path:"retransform",
        component: ()=> import("@/views/sync/Retransform.vue")
      },
      {
        path:"classLoader",
        component: ()=> import("@/views/sync/ClassLoader.vue")
      },
      {
        path: "/synchronize/synchronize",
        redirect: "",
      },
    ],
  },
];
export default routes;
