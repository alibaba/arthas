import { defineClientConfig } from '@vuepress/client'

export default defineClientConfig({
  enhance({ router }) {
    router.addRoute({
      path: '/zh-cn',
      redirect: '/'
    })
    router.addRoute({
      path: '/en-us',
      redirect: '/en'
    })
    router.addRoute({
      path: '/doc/en/:path*',
      redirect: to => `/en/doc${to.fullPath.replace('/doc/en', '')}`
    })
  },
})
