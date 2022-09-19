<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import permachine from '@/machines/perRequestMachine';
import { fetchStore } from '@/stores/fetch';
import { publicStore } from '@/stores/public';
import { ref } from 'vue';
import { interpret } from 'xstate';
import AutoComplete from './AutoComplete.vue';
const { label = "inputClassName" } = defineProps<{
  label?: string,
  submitF: (data: {
    classItem: Item,
    loaderItem: Item
  }) => void
}>()
const { getCommonResEffect } = publicStore()
const optionClass = ref([] as { name: string, value: string }[])
const optionClassloders = ref([] as { name: string, value: string }[])
const fetchS = fetchStore()
const selectedClassItem = ref({ name: "", value: "" } as Item)
const changeValue = (value: string) => {
  const searchClass = interpret(permachine)
  if (value.length > 2) {
    fetchS.baseSubmit(searchClass, {
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

}
const blurF = (value: unknown) => {
  if (value !== "") {
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
  <AutoComplete :label="label" :option-items="optionClass" :input-fn="changeValue" :filter-fn="filterfn" v-slot="slotP"
    :blur-fn="blurF" as="form">
    <AutoComplete label="classloader" :option-items="optionClassloders" v-slot="slotQ">
      <slot name="others"></slot>
      <button @click.prevent="submitF({
        classItem:slotP.selectItem,
        loaderItem:slotQ.selectItem
      })" class="border bg-blue-400 p-2 rounded-md mx-2 hover:opacity-50 transition">submit</button>
    </AutoComplete>
  </AutoComplete>
</template>
