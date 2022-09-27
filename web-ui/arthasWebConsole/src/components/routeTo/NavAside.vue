<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { PuzzleIcon, TerminalIcon, ViewGridIcon } from "@heroicons/vue/outline"
import { DesktopComputerIcon } from "@heroicons/vue/solid"
import { useRoute, useRouter } from 'vue-router';
const tabs = [
  {
    name: 'dashboard',
    url: "/dashboard",
    icon: DesktopComputerIcon
  },
  {
    name: 'immediacy',
    url: '/synchronize',
    icon: ViewGridIcon
  },{
    name: "real time",
    url: '/asynchronize',
    icon: ViewGridIcon
  },
  {
    name: 'setting & config',
    url: '/config',
    icon: PuzzleIcon
  },
  {
    name: 'console',
    url: '/console',
    icon: TerminalIcon
  }, 
]
const router = useRouter()
const routePath = computed(()=>useRoute().path)
const toNext = (url: string) => {
  router.push(url)
}
const a: StatusResult = { type: "status", message: "", statusCode: 0 }
</script>

<template>
  <div class=" h-full bg-gray-300">
    <ul class="flex flex-col justify-start w-40 h-full items-stretch bg-blue-50">
      <li v-for="(tab, idx) in tabs" :key="idx" class="flex justify-center items-center hover:bg-gray-200 transition"
        @click="toNext(tab.url)" :class="{ 'bg-gray-200': routePath.includes(tab.url), }">
        <div class="bg-gray-200 h-10 w-10 grid place-items-center rounded-full">
          <component :is="tab.icon" class="w-3/4 h-3/4 text-gray-500" />
        </div>
        <button class=" outline-none grid place-items-centerh-16 w-20 m-4">{{
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