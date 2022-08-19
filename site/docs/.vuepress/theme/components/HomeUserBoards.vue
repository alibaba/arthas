<template>
  <div class="user-boards" v-if="imgs">
    <h1>{{ pageData.users_title }}</h1>
    <p v-html="pageData.users_details"></p>
    <div class="user-logos">
      <UserBoard v-for="img in imgs" :key="img.logo" :img="img" />
    </div>
  </div>
</template>

<script setup>
import UserBoard from "./UserBoard.vue";

import { toRaw } from "vue";
import { usePageFrontmatter } from "@vuepress/client";

const pageData = usePageFrontmatter();
const imgs = toRaw(pageData.value.users) || [];
</script>

<style lang="scss" scoped>
.user-boards {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border-top: 1px solid var(--c-border);
  padding: 1.2rem 0;
  margin-top: 20px;

  .user-logos {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>
