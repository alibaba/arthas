<script setup>
import { usePageFrontmatter } from "@vuepress/client";
import { isArray } from "@vuepress/shared";
import { computed } from "vue";

const frontmatter = usePageFrontmatter();
const features = computed(() => {
  if (isArray(frontmatter.value.features)) {
    return frontmatter.value.features;
  }
  return [];
});
</script>

<template>
  <div v-if="features.length" class="features">
    <div v-for="feature in features" :key="feature.title" class="feature">
      <div v-if="feature.icon" class="icon">{{ feature.icon }}</div>
      <h2>{{ feature.title }}</h2>
      <p>{{ feature.details }}</p>
    </div>
  </div>
</template>

<style scoped>
.icon {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20px;
  border-radius: 6px;
  width: 48px;
  height: 48px;
  font-size: 30px;
}
</style>
