<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
defineProps<{
  routes:{cmd: string, url:string}[]
}>()

const router = useRouter()
const route = useRoute()
const routePath = computed(()=>route.path)
</script>

<template>
  <ul class=" w-40 border-l bg-gray-200 ">
    <li v-for="(v, i) in routes" :key="i">
      <button @click="() => router.push(v.url)"
        class=" transition w-full h-12 bg-gray-300 hover:bg-slate-400"
        :class='{"bg-gray-500 text-white":routePath.includes(v.url)}'
        >
        {{ v.cmd }}
      </button>
    </li>
  </ul>
</template>

<style scoped>
</style>