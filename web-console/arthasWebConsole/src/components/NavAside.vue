<script setup lang="ts">
import { ref } from 'vue';
import { PuzzleIcon, TerminalIcon } from "@heroicons/vue/outline"
import { useRouter } from 'vue-router';
const tabs = ref([
  // {
  //   name: '会话',
  //   url: '/',
  //   icon: "PuzzleIcon"
  // },
  // {
  //   name: '功能选择',
  //   url: '/',
  //   icon: "PuzzleIcon"
  // },
  {
    name: '项目配置',
    url: '/config',
    icon: PuzzleIcon
  },
  // {
  //   name: '日志',
  //   url: '/',
  //   icon: "PuzzleIcon"
  // },
  // {
  //   name: '调试台',
  //   url: '/terminal',
  //   icon: PuzzleIcon
  // },
  {
    name: 'console',
    url: '/console',
    icon: TerminalIcon
  }
])
const router = useRouter()
const toNext = (url: string) => {
  router.push(url)
}
</script>

<template>
  <div class=" h-full bg-gray-300">
    <ul class="flex flex-col justify-start w-40 h-80 items-stretch bg-blue-50">
      <li v-for="(tab, idx) in tabs" :key="idx" class="flex justify-center items-center hover:bg-gray-200 transition">
        <div class="bg-gray-200 h-10 w-10 grid place-items-center rounded-full">
          <component :is="tab.icon" class="w-3/4 h-3/4 text-gray-500" />
        </div>
        <button @click="toNext(tab.url)"
          class=" outline-none grid place-items-centerh-16 w-20 m-4">{{
              tab.name
          }}</button>
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