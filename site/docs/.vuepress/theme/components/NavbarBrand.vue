<script setup>
import {
  ClientOnly,
  useRouteLocale,
  useSiteLocaleData,
  withBase,
} from "@vuepress/client";
import { computed, h, ref, onBeforeMount } from "vue";
import {
  useDarkMode,
  useThemeLocaleData,
} from "@vuepress/theme-default/lib/client";

const routeLocale = useRouteLocale();
const siteLocale = useSiteLocaleData();
const themeLocale = useThemeLocaleData();
const isDarkMode = useDarkMode();

const version = ref("3.6.1");

const navbarBrandLink = computed(
  () => themeLocale.value.home || routeLocale.value
);
const navbarBrandTitle = computed(() => siteLocale.value.title);
const navbarBrandLogo = computed(() => {
  if (isDarkMode.value && themeLocale.value.logoDark !== undefined) {
    return themeLocale.value.logoDark;
  }
  return themeLocale.value.logo;
});

const getVersion = async () => {
  version.value = await fetch(
    "https://api.github.com/repos/alibaba/arthas/releases"
  )
    .then((res) => res.json())
    .then((res) => res[0].tag_name.split("-")[2]);
};
onBeforeMount(getVersion);

const NavbarBrandVersion = () =>
  h(
    "span",
    {
      class: "navbar-version",
    },
    `v${version.value}`
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
