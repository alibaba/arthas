<script setup>
import HomeBadges from "./HomeBadges.vue";

import AutoLink from "@theme/AutoLink.vue";
import {
  ClientOnly,
  usePageFrontmatter,
  useSiteLocaleData,
  withBase,
} from "@vuepress/client";
import { isArray } from "@vuepress/shared";
import { computed, h } from "vue";
import { useDarkMode } from "@vuepress/theme-default/client";

const frontmatter = usePageFrontmatter();
const siteLocale = useSiteLocaleData();
const isDarkMode = useDarkMode();

const heroImage = computed(() => {
  if (isDarkMode.value && frontmatter.value.heroImageDark !== undefined) {
    return frontmatter.value.heroImageDark;
  }
  return frontmatter.value.heroImage;
});

const heroText = computed(() => {
  if (frontmatter.value.heroText === null) {
    return null;
  }
  return frontmatter.value.heroText || siteLocale.value.title || "Hello";
});

const heroAlt = computed(
  () => frontmatter.value.heroAlt || heroText.value || "hero",
);

const tagline = computed(() => {
  if (frontmatter.value.tagline === null) {
    return null;
  }
  return (
    frontmatter.value.tagline ||
    siteLocale.value.description ||
    "Welcome to your VuePress site"
  );
});

const actions = computed(() => {
  if (!isArray(frontmatter.value.actions)) {
    return [];
  }

  return frontmatter.value.actions.map(({ text, link, type = "primary" }) => ({
    text,
    link,
    type,
  }));
});

const HomeHeroImage = () => {
  if (!heroImage.value) return null;
  const img = h("img", {
    src: withBase(heroImage.value),
    style: "width: 60%;",
    alt: heroAlt.value,
  });
  if (frontmatter.value.heroImageDark === undefined) {
    return img;
  }
  // wrap hero image with <ClientOnly> to avoid ssr-mismatch
  // when using a different hero image in dark mode
  return h(ClientOnly, () => img);
};
</script>

<template>
  <header class="hero">
    <HomeHeroImage />

    <div>
      <h1 v-if="heroText" id="main-title">
        {{ heroText }}
      </h1>

      <p v-if="tagline" class="description">
        {{ tagline }}
      </p>

      <HomeBadges />

      <p v-if="actions.length" class="actions">
        <AutoLink
          v-for="action in actions"
          :key="action.text"
          class="action-button"
          :class="[action.type]"
          :item="action"
        />
      </p>
    </div>
  </header>
</template>
