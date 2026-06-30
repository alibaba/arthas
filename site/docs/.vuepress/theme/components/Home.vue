<script setup>
import HomeUserBoards from "./HomeUserBoards.vue";
import HomeFeatures from "./HomeFeatures.vue";
import HomeHero from "./HomeHero.vue";

import HomeContent from "@theme/HomeContent.vue";
import HomeFooter from "@theme/HomeFooter.vue";
</script>

<template>
  <main class="home">
    <HomeHero />
    <HomeFeatures />
    <HomeContent />
    <HomeUserBoards />
    <HomeFooter />
  </main>
</template>
