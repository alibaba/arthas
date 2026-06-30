<template>
  <a class="my-badge" :href="URL" target="_blank">
    <component :is="comp" />
    &nbsp;
    <CountTo :startVal="0" :endVal="data" :duration="500" />
  </a>
</template>

<script setup>
import { useThemeLocaleData } from "@vuepress/plugin-theme-data/client";

const props = defineProps({
  comp: {
    type: Object,
    required: true,
  },
  data: {
    type: Number,
    required: true,
  },
});

const themeData = useThemeLocaleData();
const repoURL = `https://github.com/${themeData.value.repo}`;
const repoStarsURL = repoURL;
const repoForksURL = `${repoURL}/fork`;

let URL = repoURL;
switch (props.comp.name) {
  case "Star":
    URL = repoStarsURL;
    break;
  case "Fork":
    URL = repoForksURL;
    break;
}
</script>

<style lang="scss" scoped>
.my-badge {
  width: fit-content;
  width: -webkit-fit-content;
  width: -moz-fit-content;
  padding: 5px;
  margin: 0 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 5px;
  color: var(--c-text);
  background-color: rgba(89, 95, 101, 0.2);
  user-select: none;
}

@media (max-width: 719px) {
  .my-badge {
    margin: 0 5px;
  }
}
</style>
