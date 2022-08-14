<script setup lang="ts">
import machine from '@/machines/consoleMachine';
import { publicStore } from '@/stores/public';
import { useMachine } from '@xstate/vue';
import { onBeforeMount, ref } from 'vue';
import AutoComplete from './AutoComplete.vue';
const { label = "inputClassName" } = defineProps<{
  label?: string,
  submitF: (item: Item) => void
}>()
const { getCommonResEffect } = publicStore()
const searchClass = useMachine(machine)

const optionClass = ref([] as { name: string, value: string }[])

onBeforeMount(() => {
  searchClass.send("INIT")

  // searchClass.send({
  //   type: "SUBMIT",
  //   value: {
  //     action: "exec",
  //     command: `sc *`
  //   }
  // })
})

getCommonResEffect(searchClass, body => {
  optionClass.value.length = 0
  const result = body.results[0]
  if (result.type === "sc" && !result.detailed && !result.withField) {
    result.classNames.forEach(name => {
      optionClass.value.push({
        name,
        value: name
      })
    })
  }

  // optionClass.value = body.results.reduce((pre, result) => {
  //   if (result.type === "sc" && !result.detailed && !result.withField) {
  //     pre = pre.concat(result.classNames.map(name => ({
  //       name,
  //       value: name
  //     })))
  //     // console.log(
  //     //   result.classNames.map(name => ({
  //     //   name,
  //     //   value: name
  //     // })
  //     // ))
  //   }
  //   return pre
  // }, [] as Item[])
})
const changeValue = (value: string) => {
  console.log("changeee", value, optionClass.value)
  if (searchClass.state.value.matches("ready")) {
    searchClass.send({
      type: "SUBMIT",
      value: {
        action: "exec",
        command: `sc *${value}*`
      }
    })
  }
}
const filterfn = (_: any,options: Item[])=>options
</script>

<template>
  <AutoComplete :label="label" :option-items="optionClass" :submitfn="submitF" :input-fn="changeValue" :filter-fn="filterfn"></AutoComplete>
</template>

<style scoped>
</style>