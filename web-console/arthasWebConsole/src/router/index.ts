import { fetchStore } from "@/stores/fetch"
import {createRouter,createWebHashHistory, onBeforeRouteLeave} from "vue-router"
import routes from "./routes"


const router = createRouter({
  history: createWebHashHistory(),
  routes, 
})
router.beforeEach((to,from,next)=>{
  fetchStore().waitDone()
  next()
})
export default router