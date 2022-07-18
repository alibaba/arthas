<script setup>
import RightMenu from "./RightMenu.vue";

import PageMeta from "@theme/PageMeta.vue";
import PageNav from "@theme/PageNav.vue";
import { usePageData } from "@vuepress/client";

function showRightMenu() {
  const pages = usePageData();
  return pages.value.headers.length > 1;
}
</script>

<template>
  <main class="page">
    <slot name="top" />

    <div
      :class="showRightMenu() && 'right-menu-padding'"
      class="theme-default-content"
    >
      <slot name="content-top" />

      <RightMenu v-if="showRightMenu()" />
      <Content />

      <slot name="content-bottom" />
    </div>

    <PageMeta :class="showRightMenu() && 'right-menu-padding'" />

    <PageNav :class="showRightMenu() && 'right-menu-padding'" />

    <slot name="bottom" />
  </main>
</template>

<style lang="scss" scoped>
@media (min-width: 1279px) {
  .page {
    .theme-default-content.right-menu-padding {
      padding-right: 260px;
    }

    .page-meta.right-menu-padding {
      padding-right: 260px;
    }
    .page-nav.right-menu-padding {
      padding-right: 260px;
    }
  }
}
</style>
