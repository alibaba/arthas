<script setup>
import {
  ClientOnly,
  usePageData,
  useRouteLocale,
  useSiteLocaleData,
  withBase,
} from "@vuepress/client";
import { computed, h, ref } from "vue";
import { useThemeLocaleData } from "@vuepress/plugin-theme-data/client";
import { useDarkMode } from "@vuepress/theme-default/client";

const pageData = usePageData();
const isDarkMode = useDarkMode();
const routeLocale = useRouteLocale();
const siteLocale = useSiteLocaleData();
const themeLocale = useThemeLocaleData();

const version = ref(pageData.value.version);

const navbarBrandLink = computed(
  () => themeLocale.value.home || routeLocale.value,
);
const navbarBrandTitle = computed(() => siteLocale.value.title);
const navbarBrandLogo = computed(() => {
  if (isDarkMode.value && themeLocale.value.logoDark !== undefined) {
    return themeLocale.value.logoDark;
  }
  return themeLocale.value.logo;
});

const NavbarBrandVersion = () =>
  h(
    "span",
    {
      class: "navbar-version",
    },
    `v${version.value}`,
  );

const NavbarBrandLogo = () => {
  if (!navbarBrandLogo.value) return null;
  const img = h("img", {
    class: "logo",
    src: withBase(navbarBrandLogo.value),
    alt: navbarBrandTitle.value,
  });
  if (themeLocale.value.logoDark === undefined) {
    return img;
  }
  // wrap brand logo with <ClientOnly> to avoid ssr-mismatch
  // when using a different brand logo in dark mode
  return h(ClientOnly, () => img);
};
</script>

<template>
  <RouterLink :to="navbarBrandLink">
    <NavbarBrandLogo />

    <span
      v-if="navbarBrandTitle"
      class="site-name"
      :class="{ 'can-hide': navbarBrandLogo }"
    >
      {{ navbarBrandTitle }}
    </span>
    <NavbarBrandVersion />
  </RouterLink>
</template>

<style lang="scss" scoped>
.navbar-version {
  line-height: var(--navbar-height);
  color: var(--c-text-lighter);
}
</style>
