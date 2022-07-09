const { copyCodePlugin } = require("vuepress-plugin-copy-code2");
const { searchPlugin } = require("@vuepress/plugin-search");

module.exports = {
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
  theme: require("./theme/index"),
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
