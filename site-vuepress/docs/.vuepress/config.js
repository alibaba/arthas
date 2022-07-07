const { defaultTheme } = require("vuepress");

module.exports = {
  lang: 'en-US',
  title: 'arthas',
  description: 'arthas user document',
  head: [
    ['link', { rel: 'icon', href: '/images/favicon.ico' }],
    ['meta', { name: 'viewport', content: 'width=device-width, initial-scale=1.0' }],
  ],
  locales: {
    '/': {
      lang: 'zh-CN',
      title: 'arthas',
      description: 'arthas 使用文档'
    },
    '/en/': {
      lang: 'en-US',
      title: 'arthas',
      description: 'arthas user document'
    },
  },
  theme: defaultTheme({
    logo: '/images/arthas.png',

    repo: 'alibaba/arthas',
    docsDir: 'site-vuepress/docs',
    docsBranch: 'master',

    locales: {
      '/': {
        selectLanguageName: '简体中文',
        selectLanguageText: '选择语言',
        editLinkText: '在 GitHub 上编辑此页',
        lastUpdated: '上次更新',
      },
      '/en/': {
        selectLanguageName: 'English',
        selectLanguageText: 'Languages',
        editLinkText: 'Edit this page on GitHub',
        lastUpdated: 'Last Updated',
      },
    }
  })
}