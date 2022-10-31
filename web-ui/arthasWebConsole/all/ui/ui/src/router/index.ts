import { fetchStore } from "@/stores/fetch";
import {
  createRouter,
  createWebHashHistory,
} from "vue-router";
import routes from "./routes";

const router = createRouter({
  history: createWebHashHistory(),
  routes,
});
router.beforeEach((to, from, next) => {
  fetchStore()
    .interruptJob()
    .catch(_=>{
      // console.error(e)
      // 拦截调试台的错误
    })
    .finally(() => {
      next();
    });
});
export default router;
