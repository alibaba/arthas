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
})
const changeValue = (value: string) => {
  if (value.length > 2 && searchClass.state.value.matches("ready")) {
    searchClass.send({
      type: "SUBMIT",
      value: {  
        action: "exec",
        command: `sc *${value}*`
      }
    })
  }
}
const filterfn = (_: any,item: Item)=>true
</script>

<template>
  <AutoComplete :label="label" :option-items="optionClass" :input-fn="changeValue" :filter-fn="filterfn" v-slot="slotP" as="form">
  <button @click.prevent="submitF(slotP.selectItem)" class="border bg-blue-400 p-2 rounded-md mx-2 hover:opacity-50 transition">submit</button>
  </AutoComplete>
</template>
