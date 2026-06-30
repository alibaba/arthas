<script setup lang="ts">
import {
  Disclosure,
  DisclosureButton,
  DisclosurePanel,
} from "@headlessui/vue"
// bug？？？ data = title就会暴毙
const { title, map, buttonWidth = 'w-80', open = false, data = "", buttonAccent=false } = defineProps<{
  title: string,
  open?: boolean
  // reative Proxy
  map: Map<string, string[]>,
  buttonWidth?: string,
  buttonAccent?:boolean
  data?: any
}>()
const emit = defineEmits(["myclick"])
const disposeClick = (e:Event)=>{
  emit('myclick',e)
}

</script>

<template>
  <Disclosure as="section" class="w-100 flex flex-col mb-2">
    <DisclosureButton 
    @click.prevent="disposeClick"
    class="bg-info py-1 text-info-content rounded truncate"
      :class="{'bg-accent text-accent-content':buttonAccent,[buttonWidth]:true}"
      >
      {{ title }}
    </DisclosureButton>


    <transition enter-active-class="transition duration-75 ease-out" enter-from-class="h-0 opacity-0"
      enter-to-class="h-auto opacity-100" leave-active-class="transition duration-75 ease-out"
      leave-from-class="h-auto opacity-100" leave-to-class="h-0 opacity-0">
      <DisclosurePanel class="text-gray-500 w-full" as="ul" :static="open">
        <slot name="headerAside" :data="data"></slot>
        <div v-for="([k, v], i) in map" :key="k" class="flex mt-1">
          <Disclosure>
            <DisclosureButton class="bg-base-300 text-base-content w-40 break-all flex-shrink-0">
              {{ k }}
            </DisclosureButton>
            <DisclosurePanel as="ul" static class="flex-auto bg-base-200 text-base-content flex flex-col justify-center">
              <li v-for="(cv, ci) in v" :key="ci" :class="{ 'border-t-2': (ci > 0), 'border-base-100': (ci > 0) }"
                class=" pl-2 break-all">
                {{ cv }}
              </li>
            </DisclosurePanel>
          </Disclosure>
        </div>
        <slot name="others"></slot>
      </DisclosurePanel>
    </transition>
  </Disclosure>
</template>