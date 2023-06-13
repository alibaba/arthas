<script setup lang="ts">
import permachine from '@/machines/perRequestMachine';
import { fetchStore } from '@/stores/fetch';
import { ref } from 'vue';
import { interpret } from 'xstate';
import AutoComplete from './AutoComplete.vue';
const { label = "className", supportedover = false, noClassloader=false } = defineProps<{
  label?: string,
  submitF: (data: {
    classItem: Item,
    loaderItem: Item
  }) => void
  supportedover?:boolean
  noClassloader?:boolean
}>()
const optionClass = ref([] as { name: string, value: string }[])
const optionClassloders = ref([] as { name: string, value: string }[])
const fetchS = fetchStore()
const changeValue = (value: string) => {
  const searchClass = interpret(permachine)
  if (value.length > 2) {
    return fetchS.baseSubmit(searchClass, {
      action: "exec",
      command: `sc *${value}*`
    }).then(
      res => {
        optionClass.value.length = 0
        let result = (res as CommonRes).body.results[0]
        if (result.type === "sc" && !result.detailed && !result.withField) {
          console.log(result)
          result.classNames.forEach(name => {
            optionClass.value.push({
              name,
              value: name
            })
          })
        }
      }
    )
  }
  return Promise.resolve()
}
const blurF = (value: unknown) => {
  if (value !== "" && !noClassloader) {
    const searchClass = interpret(permachine)
    fetchS.baseSubmit(searchClass, {
      action: "exec",
      command: `sc -d *${value}*`
    }).then(
      res => {
        let result = (res as CommonRes).body.results[0]
        if (result.type === "sc" && result.detailed) {

          optionClassloders.value = result.classInfo.classloader.map(v => ({
            name: v,
            value: v.split("@")[1]
          }))
          optionClassloders.value.unshift({
            name: "default",
            value: ""
          })
        }
      }
    )
  }
}
const filterfn = (_: any, item: Item) => true
</script>

<template>
  <AutoComplete :label="label" :option-items="optionClass" :input-fn="changeValue" :filter-fn="filterfn" v-slot="slotP" :supportedover="supportedover"
    :blur-fn="blurF" as="form">
    <template v-if="noClassloader">
      <slot name="others"></slot>
      <button @click.prevent="submitF({
        classItem:slotP.selectItem,
        loaderItem:slotP.selectItem
      })" class="btn btn-primary btn-sm btn-outline mx-2 transition">submit</button>
    </template>
    <AutoComplete label="classloader" :option-items="optionClassloders" v-slot="slotQ" v-else>
      <slot name="others"></slot>
      <button @click.prevent="submitF({
        classItem:slotP.selectItem,
        loaderItem:slotQ.selectItem,
      })" class="btn btn-primary btn-sm btn-outline mx-2 transition">submit</button>
    </AutoComplete>
  </AutoComplete>
</template>
