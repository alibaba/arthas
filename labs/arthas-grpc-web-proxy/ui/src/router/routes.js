const routes = [
    {
        name: 'watch',
        path: '/watch',
        component: () => import('@/view/watchView')
    },
    {
        name: 'vmtool',
        path: '/vmtool',
        component: () => import('@/view/vmtoolView')
    },
    {
        name: 'pwd',
        path: '/pwd',
        component: () => import('@/view/pwdView')
    },
    {
        name: 'sysprop',
        path: '/sysprop',
        component: () => import('@/view/syspropView')
    },

];

export default routes
