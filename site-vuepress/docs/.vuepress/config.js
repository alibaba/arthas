const { defaultTheme } = require("vuepress");
const { copyCodePlugin } = require("vuepress-plugin-copy-code2");
const { searchPlugin } = require("@vuepress/plugin-search");

module.exports = {
  lang: "en-US",
  title: "arthas",
  description: "arthas user document",
  head: require("./configs/head"),
  locales: {
    "/": {
      lang: "zh-CN",
      title: "arthas",
      description: "arthas 使用文档",
    },
    "/en/": {
      lang: "en-US",
      title: "arthas",
      description: "arthas user document",
    },
  },
  theme: defaultTheme({
    logo: "/images/arthas.png",

    repo: "alibaba/arthas",
    docsDir: "site-vuepress/docs",
    docsBranch: "master",

    locales: {
      "/": {
        selectLanguageName: "简体中文",
        selectLanguageText: "选择语言",
        editLinkText: "在 GitHub 上编辑此页",
        lastUpdated: "上次更新",
        contributorsText: "贡献者",
        backToHome: "回到首页",
        warning: "注意",
        tip: "提示",
        danger: "警告",
        // 404 page
        notFound: [
          '这里什么都没有',
          '我们怎么到这来了？',
          '这是一个 404 页面',
          '看起来我们进入了错误的链接',
        ],
        backToHome: '返回首页',
        openInNewWindow: '在新窗口打开',
        toggleColorMode: '切换颜色模式',
        toggleSidebar: '切换侧边栏',
        navbar: require("./configs/navbar/zh"),
        sidebar: require("./configs/sidebar/zh"),
        sidebarDepth: 0,
      },
      "/en/": {
        selectLanguageName: "English",
        selectLanguageText: "Languages",
        editLinkText: "Edit this page on GitHub",
        navbar: require("./configs/navbar/en"),
        sidebar: require("./configs/sidebar/en"),
        sidebarDepth: 0,
      },
    },
  }),
  plugins: [
    copyCodePlugin({
      showInMobile: false,
      pure: true,
      locales: {
        "/": {
          hint: "复制代码",
        },
        "/en/": {
          hint: "Copy code",
        },
      },
    }),
    searchPlugin({
      locales: {
        "/": {
          placeholder: "搜索文档",
        },
        "/en/": {
          placeholder: "Search Docs",
        },
      },
    }),
  ],
};
