<template>
  <div class="badges">
    <Badge :comp="Star" :data="star" />
    <Badge :comp="Fork" :data="fork" />
  </div>
</template>

<script setup>
import Fork from "./icons/Fork.vue";
import Star from "./icons/Star.vue";
import Badge from "./Badge.vue";

import { ref, onBeforeMount } from "vue";

const fork = ref(0);
const star = ref(0);

const getData = async () => {
  const { forks, stargazers_count } = await fetch(
    "https://api.github.com/repos/alibaba/arthas"
  ).then((res) => res.json());

  fork.value = forks;
  star.value = stargazers_count;
};

onBeforeMount(getData);
</script>

<style scoped>
.badges {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
}
</style>
