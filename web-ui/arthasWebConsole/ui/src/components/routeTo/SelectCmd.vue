<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
defineProps<{
  routes: { cmd: string, url: string }[]
}>()

const router = useRouter()
const route = useRoute()
const routePath = computed(() => route.path)
</script>

<template>
  <ul class=" w-[10vw] menu bg-base-200 menu-compact">
    <li v-for="(v, i) in routes" :key="i">
      <a @click="() => router.push(v.url)" :class='{"bg-secondary text-secondary-content":routePath.includes(v.url)}'>
        {{ v.cmd }}
      </a>
    </li>
  </ul>
</template>

<style scoped>

</style>