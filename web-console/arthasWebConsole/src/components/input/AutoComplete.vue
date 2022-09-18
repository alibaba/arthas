<script setup lang="ts">import { computed, Ref, ref } from 'vue';
import {
  Combobox, ComboboxButton, ComboboxInput, ComboboxOptions, ComboboxOption, ComboboxLabel,
} from "@headlessui/vue"
import { SelectorIcon } from "@heroicons/vue/outline"

/**
 * 
 * @zh 之后优化的时候可以把autoComplete的input做成伸缩式，可以由组件去
 */

const { 
  optionItems, 
  inputFn = (_)=>{},
  blurFn = _=>{},
  optionsInit=(_)=>{},
  filterFn
} = defineProps<{
  label: string,
  optionItems: Item[],
  optionsInit?:(event:FocusEvent)=>void,
  filterFn?:(query:string,item:Item)=>boolean
  inputFn?: (value:string) => void
  blurFn?:(value:any)=>void
}>()

const query = ref('')
const selectedItem = ref({name:"",value:""} as Item)
const filterItems = computed(() => {  

  if(query.value === ""){
    selectedItem.value = {name:"",value:""}
    return optionItems
  } else {
    return optionItems.filter(item=>{
      if(filterFn) return filterFn(query.value,item)
      else {
        return item.name.toLocaleLowerCase().includes(query.value.toLocaleLowerCase())
      }
    })
  }
})
const changeF = (event:Event &{target:HTMLInputElement}) => {
  query.value = event.target.value
  inputFn(query.value)
}
const blurF = (event:Event)=>{
  blurFn(selectedItem.value.value)
}
</script>

<template>
  <Combobox v-model="selectedItem" class="flex items-center" as="div">
    <ComboboxLabel class="p-2">{{ label }}</ComboboxLabel>
    <div class="relative flex-1">
      <div
        class="relative w-full cursor-default overflow-hidden rounded-lg bg-white text-left border focus:outline-none hover:shadow-md transition">
        <ComboboxInput class="w-full border-none py-2 pl-3 pr-10 leading-5 text-gray-900 " @change="changeF" @focus.prevent="optionsInit" @blur="blurF"
          :displayValue="(item) => (item as Item).name" />
        <ComboboxButton class="absolute inset-y-0 right-0 flex items-center pr-2">
          <SelectorIcon class="h-5 w-5 text-gray-400" aria-hidden="true" />
        </ComboboxButton>
      </div>
      <ComboboxOptions
        class="absolute mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none">
        <div v-if="filterItems.length === 0 && query !== ''"
          class="relative cursor-default select-none py-2 px-4 text-gray-700">
          Nothing found.
        </div>

        <ComboboxOption v-for="item in filterItems" as="template" :key="item.name" :value="item"
          v-slot="{ selected, active }">
          <li class="relative cursor-default select-none p-2" :class="{
            'bg-blue-400 text-white': active,
            'bg-blue-600 text-white': selected,
            'text-gray-900': !active && !selected,
          }">
            <span class="block truncate"
              :class="{ 'font-medium': selected, 'font-normal': !selected, 'text-white': active, 'text-teal-600': !active && !selected }">
              {{ item.name }}
            </span>
          </li>
        </ComboboxOption>
      </ComboboxOptions>
    </div>
    <slot :selectItem="selectedItem"></slot>
  </Combobox>
</template>

<style scoped>
</style>