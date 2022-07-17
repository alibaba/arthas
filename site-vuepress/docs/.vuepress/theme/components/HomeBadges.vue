<template>
  <div class="badges">
    <Badge :comp="Star" :data="star" />
    <Badge :comp="Fork" :data="fork" />
  </div>
</template>

<script setup>
import Star from "./icons/Star.vue";
import Fork from "./icons/Fork.vue";
import Badge from "./Badge.vue";

import { ref, onBeforeMount } from "vue";

const star = ref(29582);
const fork = ref(6494);

const getData = async () => {
  const { forks, stargazers_count } = await fetch(
    "https://api.github.com/repos/alibaba/arthas"
  ).then((res) => res.json());

  star.value = stargazers_count;
  fork.value = forks;
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
