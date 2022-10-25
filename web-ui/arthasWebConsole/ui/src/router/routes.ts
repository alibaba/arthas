import { RouteRecordRaw } from "vue-router";

const routes: RouteRecordRaw[] = [
  {
    path: "/",
    redirect: "/dashboard",
  },
  {
    path: "/config",
    component: () => import("@/views/Config.vue"),
    children:[
      {
        path:"",
        redirect:"/config/perCounter"
      },  {
        path: "perCounter",
        component:()=>import("@/views/config/PerCounter.vue")
      },{
        path:"sysenv",
        component:()=>import("@/views/config/Sysenv.vue")
      },{
        path:"sysprop",
        component:()=>import("@/views/config/Sysprop.vue")
      },{
        path:"jvm",
        component:()=>import("@/views/config/Jvm.vue")
      },{
        path:"vmoption",
        component:()=>import("@/views/config/Vmoption.vue")
      },{
        path:"options",
        component:()=>import("@/views/config/Options.vue")
      }
    ]
  },
  {
    path: "/dashboard",
    component: () => import("@/views/DashBoard.vue"),
  },
  {
    path: "/synchronize",
    component: () => import("@/views/Synchronize.vue"),
    children: [
      {
        path: "",
        redirect: "/synchronize/mbean",
      },
      {
        path: "thread",
        component: () => import("@/views/sync/Thread.vue"),
      },
      {
        path: "jad",
        component: () => import("@/views/sync/Jad.vue"),
      },
      
      // {
      //   path: "retransform",
      //   component: () => import("@/views/sync/Retransform.vue"),
      // },
      {
        path: "mbean",
        component: () => import("@/views/sync/Mbean.vue"),
      },
      {
        path: "classLoader",
        component: () => import("@/views/sync/ClassLoader.vue"),
      },
      {
        path: "heapdump",
        component: () => import("@/views/sync/HeapDump.vue"),
      },
      {
        path: "vmtool",
        component: () => import("@/views/sync/Vmtool.vue"),
      },
      {
        path: "reset",
        component: () => import("@/views/sync/Reset.vue"),
      },
      {
        path: "/synchronize/synchronize",
        redirect: "",
      },{
        path:"ognl",
        component:()=>import("@/views/sync/Ognl.vue")
      },{
        path:"classInfo",
        component:()=>import("@/views/sync/ClassInfo.vue")
      }
    ],
  },
  {
    path: "/asynchronize",
    component: () => import("@/views/Asynchronize.vue"),
    children: [
      {
        path: "",
        redirect: "/asynchronize/stack",
      },
      {
        path: "tt",
        component: () => import("@/views/async/Tt.vue"),
      },
      {
        path: "ptofiler",
        component: () => import("@/views/async/Profiler.vue"),
      },
      {
        path: "stack",
        component: () => import("@/views/async/Stack.vue"),
      },
      {
        path: "monitor",
        component: () => import("@/views/async/Monitor.vue"),
      },
      {
        path: "trace",
        component: () => import("@/views/async/Trace.vue"),
      },
      {
        path: "watch",
        component: () => import("@/views/async/Watch.vue"),
      },
      {
        path: "profiler",
        component: ()=>import("@/views/async/Profiler.vue")
      }
    ],
  },{
    path:"/console",
    component:()=>import("@/views/Console.vue")
  }
];
export default routes;
