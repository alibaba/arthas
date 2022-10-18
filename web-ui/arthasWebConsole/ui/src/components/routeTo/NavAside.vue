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
  }, {
    name: "real time",
    url: '/asynchronize',
    icon: ViewGridIcon
  },
  {
    name: 'option',
    url: '/config',
    icon: PuzzleIcon
  },
  // {
  //   name: 'console',
  //   url: '/console',
  //   icon: TerminalIcon
  // },

]
const router = useRouter()
const routePath = computed(() => useRoute().path)
const toNext = (url: string) => {
  router.push(url)
}
const a: StatusResult = { type: "status", message: "", statusCode: 0 }
</script>

<template>
  <ul class="menu bg-base-200 w-[10vw] menu-compact hidden ">
    <li v-for="(tab, idx) in tabs" :key="idx" 
      @click="toNext(tab.url)" class="pl-2" >
      <a class="break-all" :class="{ 'bg-primary text-primary-content': routePath.includes(tab.url), }">
        <component :is="tab.icon" class="w-4 h-4" />
        {{
        tab.name
        }}
      </a>
    </li>
  </ul>
</template>

<style scoped>

</style>