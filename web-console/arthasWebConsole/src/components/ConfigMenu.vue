<script setup lang="ts">
import {
  Disclosure,
  DisclosureButton,
  DisclosurePanel
} from "@headlessui/vue"
// import ConfigMenu from "@/components/ConfigMenu.vue"
const props = defineProps<{
  title: string,
  list: string[],
  map: Map<string, string[]>
}>()
</script>

<template>
  <Disclosure as="section" class="w-100 flex flex-col ">
    <DisclosureButton class="py-2 bg-blue-400  rounded self-start w-1/2 hover:opacity-50 transition-all">
      {{ title }}
    </DisclosureButton>
    <transition 
    enter-active-class="transition duration-1000 ease-out" 
      enter-from-class="transform h-0 opacity-0"
      enter-to-class="transform h-auto opacity-100" 
      leave-active-class="transition duration-75 ease-out"
      leave-from-class="transform h-auto opacity-100" 
      leave-to-class="transform h-0 opacity-0">
      <DisclosurePanel class="text-gray-500 w-10/12" as="ul">
        <li v-for="(v, i) in list" :key="i" class="flex mt-2">
          <Disclosure>
            <DisclosureButton class="bg-blue-200 p-2 w-1/4">
              {{ v }} :
            </DisclosureButton>
            <DisclosurePanel as="ul" static class="flex-auto bg-blue-100 flex flex-col justify-center pl-2">
        <li v-for="(cv, ci) in map.get(v)" :key="ci">
          {{ cv }}
        </li>
      </DisclosurePanel>
  </Disclosure>
  </li>
  </DisclosurePanel>
  </transition>
  </Disclosure>
</template>

<style scoped>
</style>