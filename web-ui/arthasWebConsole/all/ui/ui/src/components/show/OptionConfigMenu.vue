<script setup lang="ts">
import {
  Disclosure,
  DisclosureButton,
  DisclosurePanel
} from "@headlessui/vue"

defineProps<{
  title: string,
  list: { [x: string]: any }[],
  titleKeyName: string
}>()
</script>

<template>
  <Disclosure as="section" class="w-100 flex flex-col mb-2">
    <DisclosureButton class="text-info-content py-1 bg-info rounded self-start w-80 ">
      {{ title }}
    </DisclosureButton>
    <transition enter-active-class="transition duration-75 ease-out" enter-from-class="h-0 opacity-0"
      enter-to-class="h-auto opacity-100" leave-active-class="transition duration-75 ease-out"
      leave-from-class="h-auto opacity-100" leave-to-class="h-0 opacity-0">
      <DisclosurePanel class=" w-10/12" as="ul">
        <li v-for="(v, i) in list" :key="i" class="flex mt-1">
          <Disclosure>
            <DisclosureButton class="bg-base-300 w-1/4 p2 break-all">
              {{ v[titleKeyName] }}
            </DisclosureButton>
            <DisclosurePanel as="div" static class="flex-auto bg-base-200 flex flex-col justify-center ml-1">
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