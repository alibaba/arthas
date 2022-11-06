<script setup lang="ts">
import { computed, ref } from 'vue';
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
  inputFn,
  blurFn = _=>{},
  optionsInit=(_)=>{},
  filterFn,
  supportedover=false
} = defineProps<{
  label: string,
  optionItems: Item[],
  optionsInit?:(event:FocusEvent)=>void,
  filterFn?:(query:string,item:Item)=>boolean
  inputFn?: (value:string) => Promise<unknown>
  blurFn?:(value:any)=>void
  supportedover?:boolean
}>()

const query = ref('')
const selectedItem = ref({name:"",value:""} as Item)
const filterItems = computed(() => {  
  let result:Item[] = []
  if(query.value === ""){
    selectedItem.value = {name:"",value:""}
    result = optionItems
  } else {
    result = optionItems.filter(item=>{
      if(filterFn) return filterFn(query.value,item)
      else {
        return item.name.toLocaleLowerCase().includes(query.value.toLocaleLowerCase())
      }
    })
  }
  if(supportedover) result.unshift(selectedItem.value)
  return result
})
let changeMutex = true
const changeF = (event:Event &{target:HTMLInputElement}) => {
  query.value = event.target.value
  if(changeMutex) {
    changeMutex = false
    if(inputFn) inputFn(query.value).finally(()=>changeMutex = true)
    else changeMutex = false
  }
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
        class="relative w-full cursor-default 
        overflow-hidden rounded-lg bg-white text-left border 
        focus-within:outline
        outline-2
        min-w-[15rem]
        hover:shadow-md transition">
        <ComboboxInput class="w-full border-none py-2 pl-3 pr-10 leading-5 text-gray-900 focus-visible:outline-none" @change="changeF" @focus.prevent="optionsInit" @blur="blurF"
          :displayValue="(item) => (item as Item).name" />
        <ComboboxButton class="absolute inset-y-0 right-0 flex items-center pr-2">
          <SelectorIcon class="h-5 w-5 text-gray-400" aria-hidden="true" />
        </ComboboxButton>
      </div>
      <ComboboxOptions
        class="absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none">
        <div v-if="filterItems.length === 0 && query !== ''"
          class="relative cursor-default select-none py-2 px-4 text-gray-700">
          Nothing found.
        </div>

        <ComboboxOption 
          v-for="(item,i) in filterItems" as="template" :key="i" :value="item"
          v-slot="{ selected, active }">
          <li class="relative cursor-default select-none p-2" :class="{
            'bg-blue-400 text-white': active,
            'bg-blue-600 text-white': selected,
            'text-gray-900': !active && !selected,
          }">
            <span class="block"
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