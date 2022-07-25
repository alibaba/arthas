const { defaultTheme } = require("@vuepress/theme-default");
const { path } = require("@vuepress/utils");

exports.localTheme = (options) => {
  return {
    name: "vuepress-theme-arthas",
    extends: defaultTheme(options),

    alias: {
      "@theme/Home.vue": path.resolve(__dirname, "components/Home.vue"),
      "@theme/NavbarDropdown.vue": path.resolve(
        __dirname,
        "components/NavbarDropdown.vue"
      ),
      "@theme/AutoLink.vue": path.resolve(__dirname, "components/AutoLink.vue"),
      "@theme/Page.vue": path.resolve(__dirname, "components/Page.vue"),
      "@theme/NavbarBrand.vue": path.resolve(
        __dirname,
        "components/NavbarBrand.vue"
      ),
    },
  };
};
