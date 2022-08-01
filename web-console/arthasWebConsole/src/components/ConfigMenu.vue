<script setup lang="ts">
import {
  Disclosure,
  DisclosureButton,
  DisclosurePanel
} from "@headlessui/vue"
const props = defineProps<{
  title: string,
  list: string[],
  map: Map<string, string[]>
}>()
</script>

<template>
  <Disclosure as="section" class="w-100 flex flex-col mb-2">
    <DisclosureButton class="py-2 bg-blue-400  rounded self-start w-1/2 hover:opacity-50 transition-all duration-100">
      {{ title }}
    </DisclosureButton>
    <transition 
      enter-active-class="transition duration-75 ease-out" 
      enter-from-class="h-0 opacity-0"
      enter-to-class="h-auto opacity-100" 
      leave-active-class="transition duration-75 ease-out"
      leave-from-class="h-auto opacity-100" 
      leave-to-class="h-0 opacity-0">
      <DisclosurePanel class="text-gray-500 w-10/12" as="ul">
        <li v-for="(v, i) in list" :key="i" class="flex mt-2">
          <Disclosure>
            <DisclosureButton class="bg-blue-200 p-2 w-1/4">
              {{ v }} :
            </DisclosureButton>
            <DisclosurePanel as="ul" static class="flex-auto bg-blue-100 flex flex-col justify-center">
        <li v-for="(cv, ci) in map.get(v)" :key="ci" :class="{'border-t-4': (ci > 0),'border-white':(ci > 0)}" class=" pl-2">
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