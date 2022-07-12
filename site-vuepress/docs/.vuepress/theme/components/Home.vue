<script setup>
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
    <HomeFooter />
  </main>
</template>
