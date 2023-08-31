import { defaultTheme } from "@vuepress/theme-default";
import { getDirname, path } from "@vuepress/utils";

const __dirname = getDirname(import.meta.url);

export function localTheme(options) {
  return {
    name: "vuepress-theme-arthas",
    extends: defaultTheme(options),

    alias: {
      "@theme/Home.vue": path.resolve(__dirname, "components/Home.vue"),
      "@theme/NavbarDropdown.vue": path.resolve(
        __dirname,
        "components/NavbarDropdown.vue",
      ),
      "@theme/AutoLink.vue": path.resolve(__dirname, "components/AutoLink.vue"),
      "@theme/Page.vue": path.resolve(__dirname, "components/Page.vue"),
      "@theme/NavbarBrand.vue": path.resolve(
        __dirname,
        "components/NavbarBrand.vue",
      ),
    },
  };
}
