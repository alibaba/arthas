<script setup lang="ts">
import {
  Disclosure,
  DisclosureButton,
  DisclosurePanel
} from "@headlessui/vue"
// import SelectInput from "./SelectInput.vue";

defineProps<{
  title: string,
  list: { [x: string]: any }[],
  titleKeyName: string
}>()
</script>

<template>
  <Disclosure as="section" class="w-100 flex flex-col mb-2">
    <DisclosureButton class="py-2 bg-blue-400  rounded self-start w-80 hover:opacity-50 transition-all duration-100">
      {{ title }}
    </DisclosureButton>
    <transition enter-active-class="transition duration-75 ease-out" enter-from-class="h-0 opacity-0"
      enter-to-class="h-auto opacity-100" leave-active-class="transition duration-75 ease-out"
      leave-from-class="h-auto opacity-100" leave-to-class="h-0 opacity-0">
      <DisclosurePanel class="text-gray-500 w-10/12" as="ul">
        <li v-for="(v, i) in list" :key="i" class="flex mt-2">
          <Disclosure>
            <DisclosureButton class="bg-blue-200 w-1/4 p2 break-all">
              {{ v[titleKeyName] }}
            </DisclosureButton>
            <DisclosurePanel as="div" static class="flex-auto bg-blue-100 flex flex-col justify-center ml-2">
              <template v-for="(cv, ci) in Object.entries(v).filter(n => n[0] !== titleKeyName)" :key="ci">
                  <slot name="item" :kv="cv" :itemTitle="(v[titleKeyName] as string)" :idx="ci"></slot>
              </template>
            </DisclosurePanel>
          </Disclosure>
        </li>
      </DisclosurePanel>
    </transition>
  </Disclosure>
</template>

<style scoped>
</style>