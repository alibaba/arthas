import { localTheme } from "./theme/index";
import { loadVersionPlugin } from "./plugins/vuepress-plugin-loadVersion";
import { head, navbarEN, navbarZH, sidebarEN, sidebarZH } from "./configs";

import { activeHeaderLinksPlugin } from "@vuepress/plugin-active-header-links";
import { copyCodePlugin } from "vuepress-plugin-copy-code2";
import { docsearchPlugin } from "@vuepress/plugin-docsearch";
import { redirectPlugin } from "vuepress-plugin-redirect";
import { defineUserConfig } from "vuepress";

export default defineUserConfig({
  title: "arthas",
  description: "arthas user document",
  head,
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
  theme: localTheme({
    logo: "/images/arthas_light.png",
    logoDark: "/images/arthas_dark.png",
    repo: "alibaba/arthas",
    docsDir: "site/docs",
    docsBranch: "master",
    themePlugins: {
      activeHeaderLinks: false,
    },

    locales: {
      "/": {
        selectLanguageName: "简体中文",
        selectLanguageText: "Languages",
        editLinkText: "在 GitHub 上编辑此页",
        lastUpdated: "上次更新",
        contributorsText: "贡献者",
        backToHome: "回到首页",
        rightMenuText: "目录",
        warning: "注意",
        tip: "提示",
        danger: "警告",
        // 404 page
        notFound: [
          "这里什么都没有",
          "我们怎么到这来了？",
          "这是一个 404 页面",
          "看起来我们进入了错误的链接",
        ],
        openInNewWindow: "在新窗口打开",
        toggleColorMode: "切换颜色模式",
        toggleSidebar: "切换侧边栏",
        navbar: navbarZH,
        sidebar: sidebarZH,
        sidebarDepth: 0,
      },
      "/en/": {
        selectLanguageName: "English",
        selectLanguageText: "Languages",
        editLinkText: "Edit this page on GitHub",
        navbar: navbarEN,
        sidebar: sidebarEN,
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
    redirectPlugin({
      config: (app) => {
        const redirects = Object.fromEntries(
          app.pages
            .filter((page) => page.path.startsWith("/en/doc/"))
            .map((page) => [
              page.path.replace(/^\/en\/doc\//, "/doc/en/"),
              page.path,
            ]),
        );

        delete redirects["/doc/en/"];
        redirects["/doc/en/index.html"] = "/en/doc/index.html";
        redirects["/en-us/index.html"] = "/en/index.html";
        redirects["/zh-cn/index.html"] = "/index.html";

        return redirects;
      },
    }),
    activeHeaderLinksPlugin({
      headerLinkSelector: "div.right-menu-item > a",
    }),
    docsearchPlugin({
      apiKey: "30c521836bfc8e97915576e11ac2cebc",
      indexName: "arthas",
      appId: "UX8WBNVHHR",
      locales: {
        "/": {
          placeholder: "搜索文档",
          translations: {
            button: {
              buttonText: "搜索文档",
              buttonAriaLabel: "搜索文档",
            },
            modal: {
              searchBox: {
                resetButtonTitle: "清除查询条件",
                resetButtonAriaLabel: "清除查询条件",
                cancelButtonText: "取消",
                cancelButtonAriaLabel: "取消",
              },
              startScreen: {
                recentSearchesTitle: "搜索历史",
                noRecentSearchesText: "没有搜索历史",
                saveRecentSearchButtonTitle: "保存至搜索历史",
                removeRecentSearchButtonTitle: "从搜索历史中移除",
                favoriteSearchesTitle: "收藏",
                removeFavoriteSearchButtonTitle: "从收藏中移除",
              },
              errorScreen: {
                titleText: "无法获取结果",
                helpText: "你可能需要检查你的网络连接",
              },
              footer: {
                selectText: "选择",
                navigateText: "切换",
                closeText: "关闭",
                searchByText: "搜索提供者",
              },
              noResultsScreen: {
                noResultsText: "无法找到相关结果",
                suggestedQueryText: "你可以尝试查询",
                reportMissingResultsText: "你认为该查询应该有结果？",
                reportMissingResultsLinkText: "点击反馈",
              },
            },
          },
        },
      },
    }),
    // Local plugin
    loadVersionPlugin(),
  ],
});
