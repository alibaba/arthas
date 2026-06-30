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

const getStarForkData = async () => {
  const stars = await fetch("https://arthas.aliyun.com/api/starCount").then(
    (res) => res.json(),
  );

  const forks = await fetch("https://arthas.aliyun.com/api/forkCount").then(
    (res) => res.json(),
  );

  star.value = stars || star.value;
  fork.value = forks || fork.value;
};

onBeforeMount(getStarForkData);
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
