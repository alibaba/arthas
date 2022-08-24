<script setup lang="ts">
import { useRouter } from 'vue-router';
defineProps<{
  routes:{cmd: string, url:string}[]
}>()

const router = useRouter()
</script>

<template>
  <ul class=" w-40 border-l bg-gray-200 ">
    <li v-for="(v, i) in routes" :key="i">
      <button @click="() => router.push(v.url)"
        class=" transition w-full h-12 bg-gray-300 focus:bg-gray-500 hover:bg-slate-400">
        {{ v.cmd }}
      </button>
    </li>
  </ul>
</template>

<style scoped>
</style>