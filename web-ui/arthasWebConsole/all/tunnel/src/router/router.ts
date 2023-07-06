import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import Console from '../../../share/component/Console.vue'
import Login from '../../../share/component/Login.vue'

const routes: Array<RouteRecordRaw> = [
    {
        path: '/',
        name: 'Home',
        component: Console,
        beforeEnter: (to, from, next) => {
            const token = sessionStorage.getItem('token')
            if (token) {
                next()
            } else {
                next('/login')
            }
        }
    },
    {
        path: '/login',
        name: 'Login',
        component: Login
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router