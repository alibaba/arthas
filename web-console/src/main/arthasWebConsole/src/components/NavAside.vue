<script setup lang="ts">
import { ref } from 'vue';
import { PuzzleIcon } from "@heroicons/vue/outline"
const tabs = ref([
  {
    name: '会话',
    url: '/',
    icon: "PuzzleIcon"
  },
  {
    name: '功能选择',
    url: '/',
    icon: "PuzzleIcon"
  },
  {
    name: '项目配置',
    url: '/',
    icon: "PuzzleIcon"
  },
  {
    name: '日志',
    url: '/',
    icon: "PuzzleIcon"
  },
  {
    name: '调试台',
    url: '/',
    icon: "PuzzleIcon"
  }
])
</script>

<template>
  <div class=" h-full bg-gray-300">
    <ul class="flex flex-col justify-start w-40 h-full items-stretch">
      <li v-for="(tab, idx) in tabs" :key="idx"
        class="  flex flex-row justify-center items-center rounded-full transition-all h-16 m-4">
        <component :is="PuzzleIcon.name" class="w-6 h-6"/>
        <div>{{ tab.name }}</div>
      </li>
    </ul>
    <ul class=" w-0 overflow-hidden">
      <slot name="detail">
      </slot>
    </ul>
  </div>
</template>

<style scoped>
</style>